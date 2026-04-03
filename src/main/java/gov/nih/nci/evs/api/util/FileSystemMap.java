package gov.nih.nci.evs.api.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Map<String, Set<String>> implementation that uses the file system for storage instead of
 * memory. Uses sharding to distribute keys across multiple files. Files are automatically cleaned
 * up when the JVM exits via shutdown hook.
 */
/** */
public class FileSystemMap implements Map<String, Set<String>> {

  private static final Logger logger = LoggerFactory.getLogger(FileSystemMap.class);

  /** The storage dir. */
  private final Path storageDir;

  /** The shard count. */
  private final int shardCount;

  /** The cache size limit. */
  private static final int CACHE_SIZE = 10000;

  /** The hot key cache. */
  private final LinkedHashMap<String, Set<String>> hotKeyCache;

  /** The current shard index. */
  private int currentShardIndex = -1;

  /** The current shard cache. */
  private Map<String, Set<String>> currentShard = null;

  /**
   * Creates a new FileSystemMap with 256 shards.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public FileSystemMap() {
    this(256);
  }

  /**
   * Creates a new FileSystemMap.
   *
   * @param shardCount number of shard files (recommended: 64-1024 for 400k keys)
   */
  public FileSystemMap(int shardCount) {
    if (shardCount <= 0) {
      throw new IllegalArgumentException("Shard count must be positive");
    }

    this.shardCount = shardCount;
    this.hotKeyCache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true);

    try {
      // Create a temporary directory for storage
      this.storageDir = Files.createTempDirectory("filesystemmap-");
      logger.info("  file map storage = " + storageDir);

      // Register shutdown hook to flush and clean up files
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

  /** Evict the least recently used keys if cache exceeds capacity. */
  private synchronized void evictIfNecessary() {
    while (hotKeyCache.size() > CACHE_SIZE) {
      Map.Entry<String, Set<String>> eldest = hotKeyCache.entrySet().iterator().next();
      String key = eldest.getKey();
      Set<String> value = eldest.getValue();

      // Remove from cache FIRST to avoid ConcurrentModificationException during loadShard overlay
      hotKeyCache.remove(key);

      // Push back to shard
      int shardIndex = getShardIndex(key);
      Map<String, Set<String>> shard = loadShard(shardIndex);
      shard.put(key, value);
    }
  }

  /**
   * Determine which shard a key belongs to.
   *
   * @param key the key
   * @return the shard index
   */
  private int getShardIndex(String key) {
    return Math.abs(key.hashCode() % shardCount);
  }

  /**
   * Get the file path for a shard.
   *
   * @param shardIndex the shard index
   * @return the shard path
   */
  private Path getShardPath(int shardIndex) {
    return storageDir.resolve("shard-" + shardIndex + ".dat");
  }

  /**
   * Load a shard from disk into cache.
   *
   * @param shardIndex the shard index
   * @return the map
   */
  private synchronized Map<String, Set<String>> loadShard(int shardIndex) {
    if (shardIndex == currentShardIndex && currentShard != null) {
      return currentShard;
    }

    if (currentShardIndex != -1) {
      saveShard(currentShardIndex);
    }

    Path shardPath = getShardPath(shardIndex);
    Map<String, Set<String>> shard;

    if (Files.exists(shardPath)) {
      try (ObjectInputStream ois =
          new ObjectInputStream(new BufferedInputStream(Files.newInputStream(shardPath)))) {
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> loaded = (Map<String, Set<String>>) ois.readObject();
        shard = loaded;
      } catch (IOException | ClassNotFoundException e) {
        // Fail if we can't load a shard
        throw new RuntimeException(e);
      }
    } else {
      shard = new HashMap<>();
    }

    // Overlay dirty data from hotKeyCache safely
    for (Map.Entry<String, Set<String>> entry : hotKeyCache.entrySet()) {
      if (getShardIndex(entry.getKey()) == shardIndex) {
        shard.put(entry.getKey(), entry.getValue());
      }
    }

    currentShardIndex = shardIndex;
    currentShard = shard;
    return shard;
  }

  /**
   * Save a shard from cache to disk.
   *
   * @param shardIndex the shard index
   */
  private synchronized void saveShard(int shardIndex) {
    if (shardIndex != currentShardIndex || currentShard == null) {
      return;
    }

    Map<String, Set<String>> shard = currentShard;
    Path shardPath = getShardPath(shardIndex);

    if (shard.isEmpty()) {
      // Delete empty shards
      try {
        Files.deleteIfExists(shardPath);
      } catch (IOException e) {
        // Ignore
      }
      return;
    }

    try (ObjectOutputStream oos =
        new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(shardPath)))) {
      oos.writeObject(new HashMap<>(shard));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Flush all cached shards to disk. */
  private synchronized void flushAll() {
    // Collect all dirty shards from the hot key cache
    Set<Integer> dirtyShards = new HashSet<>();
    for (String key : hotKeyCache.keySet()) {
      dirtyShards.add(getShardIndex(key));
    }

    // Swapping via loadShard will save the currently loaded one and overlay
    for (Integer shardIndex : dirtyShards) {
      if (shardIndex != currentShardIndex) {
        loadShard(shardIndex);
      }
    }

    if (currentShardIndex != -1) {
      saveShard(currentShardIndex);
    }
  }

  /* see superclass */
  @Override
  public int size() {
    int total = 0;
    for (int i = 0; i < shardCount; i++) {
      Map<String, Set<String>> shard = loadShard(i);
      total += shard.size();
    }
    return total;
  }

  /* see superclass */
  @Override
  public boolean isEmpty() {
    for (int i = 0; i < shardCount; i++) {
      Map<String, Set<String>> shard = loadShard(i);
      if (!shard.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /* see superclass */
  @Override
  public boolean containsKey(Object key) {
    if (!(key instanceof String)) {
      return false;
    }
    String strKey = (String) key;

    synchronized (this) {
      if (hotKeyCache.containsKey(strKey)) {
        return true;
      }
    }

    int shardIndex = getShardIndex(strKey);
    Map<String, Set<String>> shard = loadShard(shardIndex);
    return shard.containsKey(key);
  }

  /* see superclass */
  @Override
  public boolean containsValue(Object value) {
    if (!(value instanceof Set)) {
      return false;
    }

    for (int i = 0; i < shardCount; i++) {
      Map<String, Set<String>> shard = loadShard(i);
      if (shard.containsValue(value)) {
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
    String strKey = (String) key;

    synchronized (this) {
      if (hotKeyCache.containsKey(strKey)) {
        return hotKeyCache.get(strKey);
      }
    }

    int shardIndex = getShardIndex(strKey);
    Map<String, Set<String>> shard = loadShard(shardIndex);
    Set<String> value = shard.get(strKey);

    if (value != null) {
      synchronized (this) {
        hotKeyCache.put(strKey, value);
        evictIfNecessary();
      }
    }

    // Return directly so modifications will reflect in the shard memory
    return value;
  }

  /* see superclass */
  @Override
  public Set<String> put(String key, Set<String> value) {
    if (key == null) {
      throw new NullPointerException("Key cannot be null");
    }

    synchronized (this) {
      hotKeyCache.put(key, value);
      evictIfNecessary();
    }

    int shardIndex = getShardIndex(key);
    Map<String, Set<String>> shard = loadShard(shardIndex);
    // No defensive copy, store directly
    Set<String> oldValue = shard.put(key, value);
    // Removed immediate auto-flush since we save on swap/flush All
    return oldValue;
  }

  /* see superclass */
  @Override
  public Set<String> remove(Object key) {
    if (!(key instanceof String)) {
      return null;
    }
    String strKey = (String) key;

    synchronized (this) {
      hotKeyCache.remove(strKey);
    }

    int shardIndex = getShardIndex(strKey);
    Map<String, Set<String>> shard = loadShard(shardIndex);
    Set<String> oldValue = shard.remove(strKey);
    return oldValue;
  }

  /* see superclass */
  @Override
  public void putAll(Map<? extends String, ? extends Set<String>> m) {
    // Group puts by shard for efficiency
    Map<Integer, List<Map.Entry<? extends String, ? extends Set<String>>>> byShard =
        new HashMap<>();

    for (Map.Entry<? extends String, ? extends Set<String>> entry : m.entrySet()) {
      int shardIndex = getShardIndex(entry.getKey());
      byShard.computeIfAbsent(shardIndex, k -> new ArrayList<>()).add(entry);
    }

    // Process each shard
    for (Map.Entry<Integer, List<Map.Entry<? extends String, ? extends Set<String>>>> shardEntry :
        byShard.entrySet()) {
      int shardIndex = shardEntry.getKey();
      Map<String, Set<String>> shard = loadShard(shardIndex);

      for (Map.Entry<? extends String, ? extends Set<String>> entry : shardEntry.getValue()) {
        shard.put(entry.getKey(), entry.getValue());
      }
    }

    // Update hotkey cache safely without interrupting shard grouping logic
    synchronized (this) {
      for (Map.Entry<? extends String, ? extends Set<String>> entry : m.entrySet()) {
        hotKeyCache.put(entry.getKey(), entry.getValue());
      }
      evictIfNecessary();
    }
  }

  /* see superclass */
  @Override
  public synchronized void clear() {
    hotKeyCache.clear();
    currentShardIndex = -1;
    currentShard = null;

    for (int i = 0; i < shardCount; i++) {
      try {
        Files.deleteIfExists(getShardPath(i));
      } catch (IOException e) {
        // Continue with other shards
      }
    }
  }

  /* see superclass */
  @Override
  public Set<String> keySet() {
    Set<String> keys = new HashSet<>();
    for (int i = 0; i < shardCount; i++) {
      Map<String, Set<String>> shard = loadShard(i);
      keys.addAll(shard.keySet());
    }
    return keys;
  }

  /* see superclass */
  @Override
  public Collection<Set<String>> values() {
    List<Set<String>> values = new ArrayList<>();
    for (int i = 0; i < shardCount; i++) {
      Map<String, Set<String>> shard = loadShard(i);
      values.addAll(shard.values());
    }
    return values;
  }

  /* see superclass */
  @Override
  public Set<Entry<String, Set<String>>> entrySet() {
    throw new UnsupportedOperationException(
        "See implementation if this is needed but it has spotbugs issues");
    //    Set<Entry<String, Set<String>>> entries = new HashSet<>();
    //    for (int i = 0; i < shardCount; i++) {
    //      Map<String, Set<String>> shard = loadShard(i);
    //      entries.addAll(shard.entrySet());
    //    }
    //    return entries;
  }

  /** Clean up all files created by this map. */
  private void cleanup() {
    try {
      // Delete all shard files
      for (int i = 0; i < shardCount; i++) {
        try {
          Files.deleteIfExists(getShardPath(i));
        } catch (IOException e) {
          // Ignore errors during cleanup
        }
      }

      // Delete the storage directory
      Files.deleteIfExists(storageDir);
    } catch (IOException e) {
      // Ignore errors during cleanup
    }
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
   * Get the number of shards.
   *
   * @return the shard count
   */
  public int getShardCount() {
    return shardCount;
  }

  /**
   * Get statistics about shard distribution (useful for debugging).
   *
   * @return the shard distribution
   */
  public Map<Integer, Integer> getShardDistribution() {
    Map<Integer, Integer> distribution = new HashMap<>();
    for (int i = 0; i < shardCount; i++) {
      Map<String, Set<String>> shard = loadShard(i);
      distribution.put(i, shard.size());
    }
    return distribution;
  }
}
