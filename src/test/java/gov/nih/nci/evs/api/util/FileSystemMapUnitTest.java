package gov.nih.nci.evs.api.util;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for FileSystemMapUnitTest. */
class FileSystemMapUnitTest {

  /** The map. */
  private FileSystemMap map;

  /** Cleanup. */
  @AfterEach
  void cleanup() {
    // Cleanup is automatic via shutdown hook
    map = null;
  }

  /**
   * Test basic operations default shard count.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Basic Operations: Should create map with default 256 shards")
  void testBasicOperations_DefaultShardCount() throws IOException {
    map = new FileSystemMap();
    assertEquals(256, map.getShardCount());
  }

  /**
   * Test basic operations put and get.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Basic Operations: Should put and get values")
  void testBasicOperations_PutAndGet() throws IOException {
    map = new FileSystemMap();

    Set<String> fruits = new HashSet<>(Arrays.asList("apple", "banana", "orange"));
    map.put("fruits", fruits);

    Set<String> retrieved = map.get("fruits");
    assertNotNull(retrieved);
    assertEquals(3, retrieved.size());
    assertTrue(retrieved.contains("apple"));
    assertTrue(retrieved.contains("banana"));
    assertTrue(retrieved.contains("orange"));
  }

  /**
   * Test basic operations get non existent.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Basic Operations: Should return null for non-existent key")
  void testBasicOperations_GetNonExistent() throws IOException {
    map = new FileSystemMap();
    assertNull(map.get("non-existent-key"));
  }

  /**
   * Test basic operations multiple puts.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Basic Operations: Should handle multiple puts")
  void testBasicOperations_MultiplePuts() throws IOException {
    map = new FileSystemMap();

    Set<String> fruits = new HashSet<>(Arrays.asList("apple", "banana"));
    Set<String> colors = new HashSet<>(Arrays.asList("red", "blue"));

    map.put("fruits", fruits);
    map.put("colors", colors);

    assertEquals(2, map.size());
    assertEquals(fruits, map.get("fruits"));
    assertEquals(colors, map.get("colors"));
  }

  /**
   * Test basic operations remove.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Basic Operations: Should remove keys")
  void testBasicOperations_Remove() throws IOException {
    map = new FileSystemMap();

    Set<String> values = new HashSet<>(Arrays.asList("value1", "value2"));
    map.put("key", values);

    Set<String> removed = map.remove("key");
    assertEquals(values, removed);
    assertNull(map.get("key"));
    assertEquals(0, map.size());
  }

  /**
   * Test basic operations contains key.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Basic Operations: Should check if key exists")
  void testBasicOperations_ContainsKey() throws IOException {
    map = new FileSystemMap();

    Set<String> values = new HashSet<>(Arrays.asList("value1"));
    map.put("test-key", values);

    assertTrue(map.containsKey("test-key"));
    assertFalse(map.containsKey("non-existent"));
  }

  /**
   * Test basic operations contains value.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Basic Operations: Should check if value exists")
  void testBasicOperations_ContainsValue() throws IOException {
    map = new FileSystemMap();

    Set<String> values = new HashSet<>(Arrays.asList("value1", "value2"));
    map.put("key", values);

    assertTrue(map.containsValue(values));
    assertFalse(map.containsValue(new HashSet<>(Arrays.asList("different"))));
  }

  /**
   * Test basic operations clear.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Basic Operations: Should clear all entries")
  void testBasicOperations_Clear() throws IOException {
    map = new FileSystemMap();

    map.put("key1", new HashSet<>(Arrays.asList("a")));
    map.put("key2", new HashSet<>(Arrays.asList("b")));

    assertEquals(2, map.size());

    map.clear();

    assertEquals(0, map.size());
    assertTrue(map.isEmpty());
  }

  /**
   * Test basic operations is empty.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Basic Operations: Should return correct isEmpty status")
  void testBasicOperations_IsEmpty() throws IOException {
    map = new FileSystemMap();

    assertTrue(map.isEmpty());

    map.put("key", new HashSet<>(Arrays.asList("value")));
    assertFalse(map.isEmpty());

    map.remove("key");
    assertTrue(map.isEmpty());
  }

  /**
   * Test bulk operations put all.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Bulk Operations: Should handle putAll")
  void testBulkOperations_PutAll() throws IOException {
    map = new FileSystemMap(256);

    Map<String, Set<String>> bulkData = new HashMap<>();
    bulkData.put("key1", new HashSet<>(Arrays.asList("v1", "v2")));
    bulkData.put("key2", new HashSet<>(Arrays.asList("v3", "v4")));
    bulkData.put("key3", new HashSet<>(Arrays.asList("v5", "v6")));

    map.putAll(bulkData);

    assertEquals(3, map.size());
    assertEquals(bulkData.get("key1"), map.get("key1"));
    assertEquals(bulkData.get("key2"), map.get("key2"));
    assertEquals(bulkData.get("key3"), map.get("key3"));
  }

  /**
   * Test bulk operations key set.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Bulk Operations: Should return correct keySet")
  void testBulkOperations_KeySet() throws IOException {
    map = new FileSystemMap(256);

    map.put("key1", new HashSet<>(Arrays.asList("a")));
    map.put("key2", new HashSet<>(Arrays.asList("b")));
    map.put("key3", new HashSet<>(Arrays.asList("c")));

    Set<String> keys = map.keySet();
    assertEquals(3, keys.size());
    assertTrue(keys.contains("key1"));
    assertTrue(keys.contains("key2"));
    assertTrue(keys.contains("key3"));
  }

  /**
   * Test bulk operations values.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Bulk Operations: Should return correct values")
  void testBulkOperations_Values() throws IOException {
    map = new FileSystemMap(256);

    Set<String> set1 = new HashSet<>(Arrays.asList("a", "b"));
    Set<String> set2 = new HashSet<>(Arrays.asList("c", "d"));

    map.put("key1", set1);
    map.put("key2", set2);

    Collection<Set<String>> values = map.values();
    assertEquals(2, values.size());
    assertTrue(values.contains(set1));
    assertTrue(values.contains(set2));
  }

  /**
   * Test bulk operations entry set.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Bulk Operations: Should return correct entrySet")
  void testBulkOperations_EntrySet() throws IOException {
    map = new FileSystemMap(256);

    Set<String> set1 = new HashSet<>(Arrays.asList("a"));
    Set<String> set2 = new HashSet<>(Arrays.asList("b"));

    map.put("key1", set1);
    map.put("key2", set2);

    Set<Map.Entry<String, Set<String>>> entries = map.entrySet();
    assertEquals(2, entries.size());

    Map<String, Set<String>> reconstructed = new HashMap<>();
    for (Map.Entry<String, Set<String>> entry : entries) {
      reconstructed.put(entry.getKey(), entry.getValue());
    }

    assertEquals(set1, reconstructed.get("key1"));
    assertEquals(set2, reconstructed.get("key2"));
  }

  /**
   * Test large dataset ten thousand keys.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Large Dataset: Should handle 10,000 keys efficiently")
  void testLargeDataset_TenThousandKeys() throws IOException {
    map = new FileSystemMap(512);

    long startTime = System.currentTimeMillis();

    // Add 10,000 keys
    for (int i = 0; i < 10000; i++) {
      String key = "key-" + i;
      Set<String> values =
          new HashSet<>(
              Arrays.asList("value-" + i + "-1", "value-" + i + "-2", "value-" + i + "-3"));
      map.put(key, values);
    }

    long duration = System.currentTimeMillis() - startTime;

    assertEquals(10000, map.size());

    Set<String> retrieved = map.get("key-5000");
    assertNotNull(retrieved);
    assertEquals(3, retrieved.size());
    assertTrue(retrieved.contains("value-5000-1"));

    assertTrue(duration < 30000, "Loading 10k keys took " + duration + "ms (should be < 30s)");
  }

  /**
   * Test large dataset bulk operations performance.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Large Dataset: Should handle bulk operations efficiently")
  void testLargeDataset_BulkOperationsPerformance() throws IOException {
    map = new FileSystemMap(256);

    Map<String, Set<String>> bulkData = new HashMap<>();
    for (int i = 0; i < 5000; i++) {
      bulkData.put("bulk-" + i, new HashSet<>(Arrays.asList("v1", "v2", "v3")));
    }

    long startTime = System.currentTimeMillis();
    map.putAll(bulkData);
    long duration = System.currentTimeMillis() - startTime;

    assertEquals(5000, map.size());
    assertTrue(duration < 20000, "putAll 5k entries took " + duration + "ms (should be < 20s)");
  }

  /**
   * Test sharding custom shard count.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Sharding: Should create map with custom shard count")
  void testSharding_CustomShardCount() throws IOException {
    map = new FileSystemMap(512);
    assertEquals(512, map.getShardCount());
  }

  /**
   * Test sharding key distribution.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Sharding: Should distribute keys across shards")
  void testSharding_KeyDistribution() throws IOException {
    map = new FileSystemMap(16);

    for (int i = 0; i < 1000; i++) {
      map.put("item-" + i, new HashSet<>(Arrays.asList("value-" + i)));
    }

    Map<Integer, Integer> distribution = map.getShardDistribution();

    int nonEmptyShards = 0;
    int totalKeys = 0;
    int minKeys = Integer.MAX_VALUE;
    int maxKeys = 0;

    for (Map.Entry<Integer, Integer> entry : distribution.entrySet()) {
      int keyCount = entry.getValue();
      if (keyCount > 0) {
        nonEmptyShards++;
        totalKeys += keyCount;
        minKeys = Math.min(minKeys, keyCount);
        maxKeys = Math.max(maxKeys, keyCount);
      }
    }

    assertTrue(nonEmptyShards >= 12, "Should use at least 12 of 16 shards");
    assertEquals(1000, totalKeys);

    double avgKeys = totalKeys / (double) nonEmptyShards;
    assertTrue(maxKeys < avgKeys * 2, "Max keys per shard should be < 2x average");
  }

  /**
   * Test auto persistence on put.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Auto-Persistence: Should auto-persist on put")
  void testAutoPersistence_OnPut() throws IOException {
    map = new FileSystemMap(16);

    map.put("test-key", new HashSet<>(Arrays.asList("value1", "value2")));

    Path storageDir = map.getStorageDirectory();
    assertTrue(Files.exists(storageDir));

    Map<Integer, Integer> distribution = map.getShardDistribution();
    int totalInShards = distribution.values().stream().mapToInt(Integer::intValue).sum();
    assertEquals(1, totalInShards, "Data should be persisted to shard");
  }

  /**
   * Test auto persistence on remove.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Auto-Persistence: Should auto-persist on remove")
  void testAutoPersistence_OnRemove() throws IOException {
    map = new FileSystemMap(16);

    map.put("key1", new HashSet<>(Arrays.asList("v1")));
    map.put("key2", new HashSet<>(Arrays.asList("v2")));

    map.remove("key1");

    assertFalse(map.containsKey("key1"));
    assertTrue(map.containsKey("key2"));
    assertEquals(1, map.size());
  }

  /**
   * Test auto persistence on put all.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Auto-Persistence: Should auto-persist on putAll")
  void testAutoPersistence_OnPutAll() throws IOException {
    map = new FileSystemMap(16);

    Map<String, Set<String>> bulk = new HashMap<>();
    for (int i = 0; i < 100; i++) {
      bulk.put("bulk-" + i, new HashSet<>(Arrays.asList("v" + i)));
    }

    map.putAll(bulk);

    assertEquals(100, map.size());

    Map<Integer, Integer> distribution = map.getShardDistribution();
    int totalInShards = distribution.values().stream().mapToInt(Integer::intValue).sum();
    assertEquals(100, totalInShards);
  }

  /**
   * Test edge cases null key.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Edge Cases: Should throw exception for null key")
  void testEdgeCases_NullKey() throws IOException {
    map = new FileSystemMap();

    assertThrows(
        NullPointerException.class,
        () -> {
          map.put(null, new HashSet<>(Arrays.asList("value")));
        });
  }

  /**
   * Test edge cases empty sets.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Edge Cases: Should handle empty sets")
  void testEdgeCases_EmptySets() throws IOException {
    map = new FileSystemMap();

    Set<String> emptySet = new HashSet<>();
    map.put("empty", emptySet);

    Set<String> retrieved = map.get("empty");
    assertNotNull(retrieved);
    assertTrue(retrieved.isEmpty());
  }

  /**
   * Test edge cases defensive copies.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Edge Cases: Should return defensive copies")
  void testEdgeCases_DefensiveCopies() throws IOException {
    map = new FileSystemMap();

    Set<String> original = new HashSet<>(Arrays.asList("a", "b", "c"));
    map.put("key", original);

    Set<String> retrieved1 = map.get("key");
    retrieved1.add("d");

    Set<String> retrieved2 = map.get("key");
    assertEquals(3, retrieved2.size());
    assertFalse(retrieved2.contains("d"));
  }

  /**
   * Test edge cases special character keys.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Edge Cases: Should handle special characters in keys")
  void testEdgeCases_SpecialCharacterKeys() throws IOException {
    map = new FileSystemMap();

    map.put("key-with-dashes", new HashSet<>(Arrays.asList("a")));
    map.put("key_with_underscores", new HashSet<>(Arrays.asList("b")));
    map.put("key.with.dots", new HashSet<>(Arrays.asList("c")));
    map.put("key with spaces", new HashSet<>(Arrays.asList("d")));

    assertEquals(4, map.size());
    assertNotNull(map.get("key-with-dashes"));
    assertNotNull(map.get("key with spaces"));
  }

  /**
   * Test edge cases overwrite existing.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Edge Cases: Should handle overwriting existing keys")
  void testEdgeCases_OverwriteExisting() throws IOException {
    map = new FileSystemMap();

    Set<String> original = new HashSet<>(Arrays.asList("a", "b"));
    Set<String> replacement = new HashSet<>(Arrays.asList("x", "y", "z"));

    map.put("key", original);
    Set<String> oldValue = map.put("key", replacement);

    assertEquals(original, oldValue);
    assertEquals(replacement, map.get("key"));
    assertEquals(1, map.size());
  }

  /**
   * Test integration polymorphic usage.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Integration: Should work as polymorphic Map")
  void testIntegration_PolymorphicUsage() throws IOException {
    Map<String, Set<String>> genericMap = new FileSystemMap(64);

    // Use as generic Map interface
    genericMap.put("processed", new HashSet<>(Arrays.asList("p1", "p2")));
    genericMap.put("data", new HashSet<>(Arrays.asList("d1")));
    genericMap.put("results", new HashSet<>(Arrays.asList("r1", "r2", "r3")));

    assertEquals(3, genericMap.size());
    assertTrue(genericMap.containsKey("processed"));
  }
}
