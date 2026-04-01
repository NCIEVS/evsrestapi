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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

  /** The shard cache. */
  private final Map<Integer, Map<String, Set<String>>> shardCache;

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
    this.shardCache = new ConcurrentHashMap<>();

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
    if (shardCache.containsKey(shardIndex)) {
      return shardCache.get(shardIndex);
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
        //        // If we can't read the shard, start with empty map
        //        shard = new HashMap<>();

        // Fail if we can't load a shard
        throw new RuntimeException(e);
      }
    } else {
      shard = new HashMap<>();
    }

    shardCache.put(shardIndex, shard);
    return shard;
  }

  /**
   * Save a shard from cache to disk.
   *
   * @param shardIndex the shard index
   */
  private synchronized void saveShard(int shardIndex) {
    Map<String, Set<String>> shard = shardCache.get(shardIndex);
    if (shard == null) {
      return;
    }

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
  private void flushAll() {
    for (Integer shardIndex : shardCache.keySet()) {
      saveShard(shardIndex);
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

    int shardIndex = getShardIndex((String) key);
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

    int shardIndex = getShardIndex((String) key);
    Map<String, Set<String>> shard = loadShard(shardIndex);
    Set<String> value = shard.get(key);

    // Return a defensive copy to prevent external modifications
    return value != null ? new HashSet<>(value) : null;
  }

  /* see superclass */
  @Override
  public Set<String> put(String key, Set<String> value) {
    if (key == null) {
      throw new NullPointerException("Key cannot be null");
    }

    int shardIndex = getShardIndex(key);
    Map<String, Set<String>> shard = loadShard(shardIndex);
    Set<String> oldValue = shard.put(key, new HashSet<>(value));

    // Automatically flush this shard to disk
    saveShard(shardIndex);

    return oldValue;
  }

  /* see superclass */
  @Override
  public Set<String> remove(Object key) {
    if (!(key instanceof String)) {
      return null;
    }

    int shardIndex = getShardIndex((String) key);
    Map<String, Set<String>> shard = loadShard(shardIndex);
    Set<String> oldValue = shard.remove(key);

    // Automatically flush this shard to disk
    saveShard(shardIndex);

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
        shard.put(entry.getKey(), new HashSet<>(entry.getValue()));
      }

      // Automatically flush this shard to disk
      saveShard(shardIndex);
    }
  }

  /* see superclass */
  @Override
  public void clear() {
    shardCache.clear();

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
