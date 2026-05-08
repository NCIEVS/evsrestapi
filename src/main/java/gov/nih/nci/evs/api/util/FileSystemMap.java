package gov.nih.nci.evs.api.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Map<String, Set<String>> implementation backed by an H2 MVStore. Keys are persisted
 * individually, which avoids the whole-shard serialization cost of the original implementation
 * while preserving the mutable Map contract used by the hierarchy loader.
 */
/** */
public class FileSystemMap implements Map<String, Set<String>>, AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(FileSystemMap.class);

  /** The default hot key cache size limit. */
  private static final int DEFAULT_HOT_KEY_CACHE_SIZE = 100000;

  /** The default MVStore read cache size in MB. */
  private static final int DEFAULT_STORE_CACHE_SIZE_MB = 256;

  /** Report I/O stats after this many store operations. */
  private static final int IO_LOG_INTERVAL = 10000;

  /** Commit after this many persisted keys. */
  private static final int COMMIT_INTERVAL = 1000;

  /** The storage dir. */
  private final Path storageDir;

  /** The hot key cache size limit. */
  private final int hotKeyCacheSize;

  /** The MVStore cache size in MB. */
  private final int storeCacheSizeMb;

  /** The backing store. */
  private final MVStore store;

  /** The persisted key/value map. */
  private final MVMap<String, byte[]> backingMap;

  /** The hot key cache. */
  private final LinkedHashMap<String, Set<String>> hotKeyCache;

  /** The dirty keys. */
  private final Set<String> dirtyKeys = new HashSet<>();

  /** The total size. */
  private int totalSize = 0;

  /** The persisted load count. */
  private long loadCount = 0;

  /** The persisted save count. */
  private long saveCount = 0;

  /** The cache hit count. */
  private long cacheHitCount = 0;

  /** The last total I/O count reported in the log. */
  private long lastLoggedIoTotal = -1;

  /** The number of writes since the last commit. */
  private int writesSinceCommit = 0;

  /** Whether the map has been closed. */
  private boolean closed = false;

  /** A Set wrapper that marks a key dirty whenever it is mutated. */
  private static class DirtyTrackingSet extends AbstractSet<String> {

    /** The delegate set. */
    private final Set<String> delegate;

    /** The owning map. */
    private transient FileSystemMap owner;

    /** The owning key. */
    private transient String key;

    DirtyTrackingSet(Set<String> delegate, FileSystemMap owner, String key) {
      this.delegate = delegate;
      this.owner = owner;
      this.key = key;
    }

    void attach(FileSystemMap owner, String key) {
      this.owner = owner;
      this.key = key;
    }

    private void markDirty() {
      if (owner != null) {
        owner.markDirty(key);
      }
    }

    Set<String> copyDelegate() {
      return new HashSet<>(delegate);
    }

    @Override
    public Iterator<String> iterator() {
      final Iterator<String> delegateIterator = delegate.iterator();
      return new Iterator<>() {
        @Override
        public boolean hasNext() {
          return delegateIterator.hasNext();
        }

        @Override
        public String next() {
          return delegateIterator.next();
        }

        @Override
        public void remove() {
          delegateIterator.remove();
          markDirty();
        }
      };
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public boolean contains(Object o) {
      return delegate.contains(o);
    }

    @Override
    public boolean add(String e) {
      boolean changed = delegate.add(e);
      if (changed) {
        markDirty();
      }
      return changed;
    }

    @Override
    public boolean remove(Object o) {
      boolean changed = delegate.remove(o);
      if (changed) {
        markDirty();
      }
      return changed;
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
      boolean changed = delegate.addAll(c);
      if (changed) {
        markDirty();
      }
      return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      boolean changed = delegate.retainAll(c);
      if (changed) {
        markDirty();
      }
      return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      boolean changed = delegate.removeAll(c);
      if (changed) {
        markDirty();
      }
      return changed;
    }

    @Override
    public void clear() {
      if (!delegate.isEmpty()) {
        delegate.clear();
        markDirty();
      }
    }
  }

  /** Creates a new FileSystemMap with default cache sizes. */
  public FileSystemMap() {
    this(DEFAULT_HOT_KEY_CACHE_SIZE, DEFAULT_STORE_CACHE_SIZE_MB);
  }

  /**
   * Creates a new FileSystemMap.
   *
   * @param hotKeyCacheSize number of decoded keys to retain in memory
   */
  public FileSystemMap(int hotKeyCacheSize) {
    this(hotKeyCacheSize, DEFAULT_STORE_CACHE_SIZE_MB);
  }

  /**
   * Creates a new FileSystemMap with configurable cache sizes.
   *
   * @param hotKeyCacheSize number of decoded keys to retain in memory
   * @param storeCacheSizeMb MVStore read cache size in MB
   */
  public FileSystemMap(int hotKeyCacheSize, int storeCacheSizeMb) {
    if (hotKeyCacheSize <= 0) {
      throw new IllegalArgumentException("Hot key cache size must be positive");
    }
    if (storeCacheSizeMb <= 0) {
      throw new IllegalArgumentException("Store cache size must be positive");
    }

    this.hotKeyCacheSize = hotKeyCacheSize;
    this.storeCacheSizeMb = storeCacheSizeMb;
    this.hotKeyCache = new LinkedHashMap<>(hotKeyCacheSize, 0.75f, true);

    try {
      this.storageDir = Files.createTempDirectory("filesystemmap-");
      logger.info("  file map storage = {}", storageDir);

      this.store =
          new MVStore.Builder()
              .fileName(storageDir.resolve("map.mv.db").toString())
              .cacheSize(storeCacheSizeMb)
              .autoCommitDisabled()
              .open();
      this.backingMap = store.openMap("paths");
      this.totalSize = backingMap.size();

      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    flushAll();
                    cleanup();
                  }));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Ensure the map is open.
   *
   * @throws IllegalStateException if the map has been closed
   */
  private synchronized void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("FileSystemMap is closed");
    }
  }

  /**
   * Mark a key dirty.
   *
   * @param key the key
   */
  private synchronized void markDirty(String key) {
    dirtyKeys.add(key);
  }

  /**
   * Wrap a set so in-place mutations mark the key dirty.
   *
   * @param key the key
   * @param value the value
   * @return the wrapped set
   */
  private Set<String> wrapSet(String key, Set<String> value) {
    if (value == null) {
      return null;
    }
    if (value instanceof DirtyTrackingSet) {
      DirtyTrackingSet wrapped = (DirtyTrackingSet) value;
      wrapped.attach(this, key);
      return wrapped;
    }
    return new DirtyTrackingSet(value, this, key);
  }

  /**
   * Encode a set for storage.
   *
   * @param value the value
   * @return the encoded bytes
   */
  private byte[] encodeSet(Set<String> value) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(baos)) {
        out.writeInt(value.size());
        for (String entry : value) {
          byte[] bytes = entry.getBytes(StandardCharsets.UTF_8);
          out.writeInt(bytes.length);
          out.write(bytes);
        }
        out.flush();
      }
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Decode a set from storage.
   *
   * @param bytes the encoded bytes
   * @return the decoded set
   */
  private Set<String> decodeSet(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
      int size = in.readInt();
      Set<String> decoded = new HashSet<>(Math.max(5, size));
      for (int i = 0; i < size; i++) {
        int length = in.readInt();
        byte[] entry = in.readNBytes(length);
        decoded.add(new String(entry, StandardCharsets.UTF_8));
      }
      return decoded;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Evict the least recently used hot keys if cache exceeds capacity. */
  private synchronized void evictIfNecessary() {
    while (hotKeyCache.size() > hotKeyCacheSize) {
      final String key = hotKeyCache.entrySet().iterator().next().getKey();
      persistDirtyKeyIfNeeded(key);
      hotKeyCache.remove(key);
    }
  }

  /** Commit if enough writes have accumulated. */
  private synchronized void commitIfNeeded() {
    if (writesSinceCommit >= COMMIT_INTERVAL) {
      store.commit();
      writesSinceCommit = 0;
    }
  }

  /** Log I/O progress for long-running builds. */
  private synchronized void logIoProgressIfNeeded() {
    final long totalIo = loadCount + saveCount;
    if (totalIo > 0 && totalIo % IO_LOG_INTERVAL == 0 && totalIo != lastLoggedIoTotal) {
      lastLoggedIoTotal = totalIo;
      logger.info(
          "  file map io: loads={}, saves={}, cacheHits={}, dirtyKeys={}, hotKeys={}",
          loadCount,
          saveCount,
          cacheHitCount,
          dirtyKeys.size(),
          hotKeyCache.size());
    }
  }

  /**
   * Persist one key if it is dirty.
   *
   * @param key the key
   */
  private synchronized void persistDirtyKeyIfNeeded(String key) {
    if (!dirtyKeys.contains(key)) {
      return;
    }

    final Set<String> value = hotKeyCache.get(key);
    if (value == null) {
      dirtyKeys.remove(key);
      return;
    }

    backingMap.put(key, encodeSet(value));
    dirtyKeys.remove(key);
    saveCount++;
    writesSinceCommit++;
    commitIfNeeded();
    logIoProgressIfNeeded();
  }

  /**
   * Load a key from the MVStore and cache it.
   *
   * @param key the key
   * @return the loaded set
   */
  private Set<String> loadValue(String key) {
    ensureOpen();

    synchronized (this) {
      final Set<String> cached = hotKeyCache.get(key);
      if (cached != null) {
        cacheHitCount++;
        return cached;
      }
    }

    final byte[] encoded = backingMap.get(key);
    loadCount++;
    logIoProgressIfNeeded();
    if (encoded == null) {
      return null;
    }

    final Set<String> value = wrapSet(key, decodeSet(encoded));
    synchronized (this) {
      hotKeyCache.put(key, value);
      evictIfNecessary();
    }
    return value;
  }

  /** Flush all dirty cached keys to the MVStore. */
  private synchronized void flushAll() {
    if (closed) {
      return;
    }

    final List<String> keysToFlush = new ArrayList<>(dirtyKeys);
    for (String key : keysToFlush) {
      persistDirtyKeyIfNeeded(key);
    }

    if (writesSinceCommit > 0 || store.hasUnsavedChanges()) {
      store.commit();
      writesSinceCommit = 0;
    }
  }

  /** Clean up all files created by this map. */
  private synchronized void cleanup() {
    try {
      if (!closed) {
        try {
          flushAll();
        } catch (Exception e) {
          // Ignore during shutdown cleanup
        }
        store.close();
        closed = true;
      }
    } catch (Exception e) {
      // Ignore during cleanup
    }

    try {
      if (Files.exists(storageDir)) {
        try (var paths = Files.walk(storageDir)) {
          paths.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
        }
      }
    } catch (IOException e) {
      // Ignore during cleanup
    }
  }

  /**
   * Delete a path quietly.
   *
   * @param path the path
   */
  private void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      // Ignore during cleanup
    }
  }

  /* see superclass */
  @Override
  public synchronized int size() {
    return totalSize;
  }

  /* see superclass */
  @Override
  public synchronized boolean isEmpty() {
    return totalSize == 0;
  }

  /* see superclass */
  @Override
  public boolean containsKey(Object key) {
    if (!(key instanceof String)) {
      return false;
    }

    final String strKey = (String) key;
    synchronized (this) {
      if (hotKeyCache.containsKey(strKey)) {
        return true;
      }
    }
    ensureOpen();
    return backingMap.containsKey(strKey);
  }

  /* see superclass */
  @Override
  public boolean containsValue(Object value) {
    if (!(value instanceof Set)) {
      return false;
    }

    @SuppressWarnings("unchecked")
    final Set<String> target = (Set<String>) value;

    synchronized (this) {
      for (Set<String> cached : hotKeyCache.values()) {
        if (cached.equals(target)) {
          return true;
        }
      }
    }

    flushAll();
    for (byte[] encoded : backingMap.values()) {
      if (decodeSet(encoded).equals(target)) {
        return true;
      }
    }
    return false;
  }

  /* see superclass */
  @Override
  public Set<String> get(Object key) {
    if (!(key instanceof String)) {
      return null;
    }
    return loadValue((String) key);
  }

  /* see superclass */
  @Override
  public Set<String> put(String key, Set<String> value) {
    if (key == null) {
      throw new NullPointerException("Key cannot be null");
    }
    ensureOpen();

    final Set<String> wrappedValue = wrapSet(key, value == null ? null : new HashSet<>(value));
    final Set<String> oldValue = get(key);

    synchronized (this) {
      if (oldValue == null) {
        totalSize++;
      }
      hotKeyCache.put(key, wrappedValue);
      markDirty(key);
      evictIfNecessary();
    }

    return oldValue;
  }

  /**
   * Add a single value to the set for a key using one key-level lookup.
   *
   * @param key the key
   * @param value the value
   * @return true if the set changed
   */
  public boolean addToSet(String key, String value) {
    if (key == null) {
      throw new NullPointerException("Key cannot be null");
    }
    ensureOpen();

    Set<String> set = get(key);
    if (set == null) {
      set = wrapSet(key, new HashSet<>(5));
      synchronized (this) {
        hotKeyCache.put(key, set);
        totalSize++;
        markDirty(key);
      }
    }

    boolean changed = set.add(value);
    synchronized (this) {
      evictIfNecessary();
    }
    return changed;
  }

  /* see superclass */
  @Override
  public Set<String> remove(Object key) {
    if (!(key instanceof String)) {
      return null;
    }
    ensureOpen();

    final String strKey = (String) key;
    final Set<String> oldValue = get(strKey);
    if (oldValue == null) {
      return null;
    }

    synchronized (this) {
      hotKeyCache.remove(strKey);
      dirtyKeys.remove(strKey);
      backingMap.remove(strKey);
      totalSize--;
      saveCount++;
      writesSinceCommit++;
      commitIfNeeded();
      logIoProgressIfNeeded();
    }
    return oldValue;
  }

  /* see superclass */
  @Override
  public void putAll(Map<? extends String, ? extends Set<String>> m) {
    for (Map.Entry<? extends String, ? extends Set<String>> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  /* see superclass */
  @Override
  public synchronized void clear() {
    ensureOpen();
    hotKeyCache.clear();
    dirtyKeys.clear();
    backingMap.clear();
    store.commit();
    writesSinceCommit = 0;
    totalSize = 0;
  }

  /* see superclass */
  @Override
  public Set<String> keySet() {
    flushAll();
    return new HashSet<>(backingMap.keySet());
  }

  /* see superclass */
  @Override
  public Collection<Set<String>> values() {
    flushAll();
    List<Set<String>> values = new ArrayList<>();
    for (byte[] encoded : backingMap.values()) {
      values.add(decodeSet(encoded));
    }
    return values;
  }

  /* see superclass */
  @Override
  public Set<Entry<String, Set<String>>> entrySet() {
    throw new UnsupportedOperationException(
        "See implementation if this is needed but it has spotbugs issues");
  }

  /* see superclass */
  @Override
  public synchronized void close() {
    cleanup();
  }

  /**
   * Get the storage directory path (useful for debugging).
   *
   * @return the storage directory
   */
  public Path getStorageDirectory() {
    return storageDir;
  }

  /**
   * Get the hot key cache size.
   *
   * @return the hot key cache size
   */
  public int getHotKeyCacheSize() {
    return hotKeyCacheSize;
  }

  /**
   * Get the store cache size in MB.
   *
   * @return the store cache size
   */
  public int getStoreCacheSizeMb() {
    return storeCacheSizeMb;
  }
}
