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

/** Tests for FileSystemMap. */
class FileSystemMapUnitTest {

  /** The map. */
  private FileSystemMap map;

  /** Cleanup. */
  @AfterEach
  void cleanup() {
    if (map != null) {
      map.close();
      map = null;
    }
  }

  /**
   * Test basic operations default cache sizes.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Basic Operations: Should create map with default cache sizes")
  void testBasicOperations_DefaultCacheSizes() throws IOException {
    map = new FileSystemMap();
    assertEquals(100000, map.getHotKeyCacheSize());
    assertEquals(256, map.getStoreCacheSizeMb());
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

    final Set<String> fruits = new HashSet<>(Arrays.asList("apple", "banana", "orange"));
    map.put("fruits", fruits);

    final Set<String> retrieved = map.get("fruits");
    assertNotNull(retrieved);
    assertEquals(3, retrieved.size());
    assertTrue(retrieved.contains("apple"));
    assertTrue(retrieved.contains("banana"));
    assertTrue(retrieved.contains("orange"));

    map.put("fruits2", new HashSet<>());
    map.get("fruits2").add("apple");
    map.get("fruits2").add("banana");
    map.get("fruits2").add("orange");
    final Set<String> retrieved2 = map.get("fruits2");
    assertNotNull(retrieved2);
    assertEquals(3, retrieved2.size());
  }

  /**
   * Test basic operations in place mutation persists.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Basic Operations: Should persist in-place set mutations")
  void testBasicOperations_InPlaceMutationPersists() throws IOException {
    map = new FileSystemMap(16);

    map.put("mutable", new HashSet<>());
    map.get("mutable").add("alpha");
    map.get("mutable").add("beta");

    assertEquals(1, map.size());
    final Set<String> retrieved = map.get("mutable");
    assertEquals(2, retrieved.size());
    assertTrue(retrieved.contains("alpha"));
    assertTrue(retrieved.contains("beta"));
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

    final Set<String> fruits = new HashSet<>(Arrays.asList("apple", "banana"));
    final Set<String> colors = new HashSet<>(Arrays.asList("red", "blue"));

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

    final Set<String> values = new HashSet<>(Arrays.asList("value1", "value2"));
    map.put("key", values);

    final Set<String> removed = map.remove("key");
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
    map.put("test-key", new HashSet<>(Arrays.asList("value1")));
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

    final Set<String> values = new HashSet<>(Arrays.asList("value1", "value2"));
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

    final Map<String, Set<String>> bulkData = new HashMap<>();
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

    final Set<String> keys = map.keySet();
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

    final Set<String> set1 = new HashSet<>(Arrays.asList("a", "b"));
    final Set<String> set2 = new HashSet<>(Arrays.asList("c", "d"));
    map.put("key1", set1);
    map.put("key2", set2);

    final Collection<Set<String>> values = map.values();
    assertEquals(2, values.size());
    assertTrue(values.contains(set1));
    assertTrue(values.contains(set2));
  }

  /**
   * Test large dataset ten thousand keys.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Large Dataset: Should handle 10,000 keys efficiently")
  void testLargeDataset_TenThousandKeys() throws IOException {
    map = new FileSystemMap(10000);

    final long startTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
      map.put(
          "key-" + i,
          new HashSet<>(
              Arrays.asList("value-" + i + "-1", "value-" + i + "-2", "value-" + i + "-3")));
    }
    final long duration = System.currentTimeMillis() - startTime;

    assertEquals(10000, map.size());
    final Set<String> retrieved = map.get("key-5000");
    assertNotNull(retrieved);
    assertEquals(3, retrieved.size());
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
    map = new FileSystemMap(5000);

    final Map<String, Set<String>> bulkData = new HashMap<>();
    for (int i = 0; i < 5000; i++) {
      bulkData.put("bulk-" + i, new HashSet<>(Arrays.asList("v1", "v2", "v3")));
    }

    final long startTime = System.currentTimeMillis();
    map.putAll(bulkData);
    final long duration = System.currentTimeMillis() - startTime;

    assertEquals(5000, map.size());
    assertTrue(duration < 20000, "putAll 5k entries took " + duration + "ms (should be < 20s)");
  }

  /**
   * Test configuration custom hot key cache size.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Configuration: Should create map with custom hot key cache size")
  void testConfiguration_CustomHotKeyCacheSize() throws IOException {
    map = new FileSystemMap(512);
    assertEquals(512, map.getHotKeyCacheSize());
  }

  /**
   * Test configuration configurable cache sizes.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Configuration: Should create map with configurable cache sizes")
  void testConfiguration_ConfigurableCacheSizes() throws IOException {
    map = new FileSystemMap(2000, 64);
    assertEquals(2000, map.getHotKeyCacheSize());
    assertEquals(64, map.getStoreCacheSizeMb());
  }

  /**
   * Test auto persistence on put.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Auto-Persistence: Should persist data to store")
  void testAutoPersistence_OnPut() throws IOException {
    map = new FileSystemMap(16);

    map.put("test-key", new HashSet<>(Arrays.asList("value1", "value2")));

    final Path storageDir = map.getStorageDirectory();
    assertTrue(Files.exists(storageDir));
    assertEquals(1, map.size());
    assertTrue(map.keySet().contains("test-key"));
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

    final Map<String, Set<String>> bulk = new HashMap<>();
    for (int i = 0; i < 100; i++) {
      bulk.put("bulk-" + i, new HashSet<>(Arrays.asList("v" + i)));
    }

    map.putAll(bulk);
    assertEquals(100, map.size());
    assertEquals(100, map.keySet().size());
  }

  /**
   * Test auto persistence add to set.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @DisplayName("Auto-Persistence: Should support addToSet without duplicates")
  void testAutoPersistence_AddToSet() throws IOException {
    map = new FileSystemMap(16);

    assertTrue(map.addToSet("path-key", "root|child"));
    assertFalse(map.addToSet("path-key", "root|child"));
    assertTrue(map.addToSet("path-key", "root|child|leaf"));

    final Set<String> retrieved = map.get("path-key");
    assertEquals(2, retrieved.size());
    assertTrue(retrieved.contains("root|child"));
    assertTrue(retrieved.contains("root|child|leaf"));
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
        NullPointerException.class, () -> map.put(null, new HashSet<>(Arrays.asList("value"))));
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
    map.put("empty", new HashSet<>());
    final Set<String> retrieved = map.get("empty");
    assertNotNull(retrieved);
    assertTrue(retrieved.isEmpty());
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

    final Set<String> original = new HashSet<>(Arrays.asList("a", "b"));
    final Set<String> replacement = new HashSet<>(Arrays.asList("x", "y", "z"));

    map.put("key", original);
    final Set<String> oldValue = map.put("key", replacement);

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
    @SuppressWarnings("resource")
    final Map<String, Set<String>> genericMap = new FileSystemMap(64);

    genericMap.put("processed", new HashSet<>(Arrays.asList("p1", "p2")));
    genericMap.put("data", new HashSet<>(Arrays.asList("d1")));
    genericMap.put("results", new HashSet<>(Arrays.asList("r1", "r2", "r3")));

    assertEquals(3, genericMap.size());
    assertTrue(genericMap.containsKey("processed"));
  }
}
