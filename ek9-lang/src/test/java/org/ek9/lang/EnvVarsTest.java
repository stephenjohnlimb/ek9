package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test suite for EnvVars EK9 built-in type.
 */
class EnvVarsTest extends Common {

  @Test
  void testConstructor() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    assertSet.accept(envVars);
  }

  @Test
  void testIsSetAlwaysTrue() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    // EnvVars is stateless, so isSet should always be true
    assertSet.accept(envVars._isSet());
  }

  @Test
  void testGetValidEnvironmentVariable() {
    EnvVars envVars = new EnvVars();
    
    // Test with PATH environment variable (should exist on most systems)
    String path = envVars.get(String._of("PATH"));
    
    // PATH should exist and be set
    assertSet.accept(path);
    Assertions.assertFalse(path.state.isEmpty());
  }

  @Test
  void testGetNonExistentEnvironmentVariable() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    
    // Test with non-existent environment variable
    String nonExistent = envVars.get(String._of("NON_EXISTENT_ENV_VAR_12345"));
    
    // Should return unset String
    assertUnset.accept(nonExistent);
  }

  @Test
  void testGetWithNullInput() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    
    // Test with null input
    String result = envVars.get(null);
    
    // Should return unset String
    assertUnset.accept(result);
  }

  @Test
  void testGetWithUnsetInput() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    
    // Test with unset String input
    String result = envVars.get(new String());
    
    // Should return unset String
    assertUnset.accept(result);
  }

  @Test
  void testGetWithEmptyStringInput() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    
    // Test with empty string input
    String result = envVars.get(String._of(""));
    
    // Empty string is a valid key in EK9, should return unset String (env var doesn't exist)
    assertUnset.accept(result);
  }

  @Test
  void testContainsValidEnvironmentVariable() {
    EnvVars envVars = new EnvVars();
    
    // Test with PATH environment variable (should exist on most systems)
    Boolean contains = envVars._contains(String._of("PATH"));
    
    // PATH should exist
    assertSet.accept(contains);
    assertTrue(contains.state);
  }

  @Test
  void testContainsNonExistentEnvironmentVariable() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    
    // Test with non-existent environment variable
    Boolean contains = envVars._contains(String._of("NON_EXISTENT_ENV_VAR_12345"));
    
    // Should return set Boolean with false value
    assertSet.accept(contains);
    assertFalse.accept(contains);
  }

  @Test
  void testContainsWithNullInput() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    
    // Test with null input
    Boolean result = envVars._contains(null);
    
    // Should return unset Boolean
    assertUnset.accept(result);
  }

  @Test
  void testContainsWithUnsetInput() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);

    // Test with unset String input
    Boolean result = envVars._contains(new String());
    
    // Should return unset Boolean
    assertUnset.accept(result);
  }

  @Test
  void testContainsWithEmptyStringInput() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);

    // Test with empty string input
    Boolean result = envVars._contains(String._of(""));
    
    // Empty string is a valid key in EK9, should return set Boolean with false value
    assertSet.accept(result);
    assertFalse.accept(result);
  }

  @Test
  void testKeys() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    
    // Get environment variable keys
    StringInput keys = envVars.keys();
    
    // Should return set StringInput
    assertSet.accept(keys._isSet());
  }

  @Test
  void testKeysIteration() {
    EnvVars envVars = new EnvVars();
    
    // Get environment variable keys
    StringInput keys = envVars.keys();
    
    // Should be able to iterate through keys
    assertSet.accept(keys._isSet());
    
    List<String> keyList = new ArrayList<>();
    while (keys.hasNext().state) {
      String key = keys.next();
      assertSet.accept(key);
      keyList.add(key);
    }
    
    // Should have found some environment variables
    Assertions.assertFalse(keyList.isEmpty());
    
    // Should contain common environment variables like PATH
    boolean foundPath = keyList.stream()
        .anyMatch(key -> "PATH".equals(key.state));
    assertTrue(foundPath);
  }

  @Test
  void testKeysHasNext() {
    EnvVars envVars = new EnvVars();
    
    // Get environment variable keys
    StringInput keys = envVars.keys();
    
    // Should have some keys initially
    assertSet.accept(keys.hasNext());
    assertTrue(keys.hasNext().state);
    
    // Iterate through all keys
    while (keys.hasNext().state) {
      String key = keys.next();
      assertSet.accept(key);
    }
    
    // Should have no more keys
    assertSet.accept(keys.hasNext());
    assertFalse.accept(keys.hasNext());
  }

  @Test
  void testKeysNext() {
    EnvVars envVars = new EnvVars();
    
    // Get environment variable keys
    StringInput keys = envVars.keys();
    
    if (keys.hasNext().state) {
      String firstKey = keys.next();
      assertSet.accept(firstKey);
      Assertions.assertFalse(firstKey.state.isEmpty());
    }
  }

  @Test
  void testKeysNextWhenEmpty() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    
    // Get environment variable keys
    StringInput keys = envVars.keys();
    
    // Iterate through all keys
    while (keys.hasNext().state) {
      keys.next();
    }
    
    // Now try to get next when empty
    String result = keys.next();
    assertUnset.accept(result);
  }

  @Test
  void testKeysClose() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    
    // Get environment variable keys
    StringInput keys = envVars.keys();
    
    // Should be set initially
    assertSet.accept(keys._isSet());
    
    // Close the StringInput
    keys._close();
    
    // Should be unset after closing
    assertFalse.accept(keys._isSet());
    
    // hasNext should return false after closing
    assertSet.accept(keys.hasNext());
    assertFalse.accept(keys.hasNext());
    
    // next should return unset after closing
    String result = keys.next();
    assertUnset.accept(result);
  }

  @Test
  void testKeysIsSet() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    
    // Get environment variable keys
    StringInput keys = envVars.keys();
    
    // Should be set initially
    assertSet.accept(keys._isSet());
    
    // Close the StringInput
    keys._close();
    
    // Should be unset after closing
    assertFalse.accept(keys._isSet());
  }

  @Test
  void testGetAndContainsConsistency() {
    EnvVars envVars = new EnvVars();
    assertNotNull(envVars);
    
    // Test with PATH environment variable
    String pathKey = String._of("PATH");
    
    Boolean containsPath = envVars._contains(pathKey);
    String pathValue = envVars.get(pathKey);
    
    // If contains returns true, get should return a set value
    if (containsPath._isSet().state && containsPath.state) {
      assertSet.accept(pathValue);
    }
    
    // Test with non-existent environment variable
    String nonExistentKey = String._of("NON_EXISTENT_ENV_VAR_12345");
    
    Boolean containsNonExistent = envVars._contains(nonExistentKey);
    String nonExistentValue = envVars.get(nonExistentKey);
    
    // If contains returns false, get should return unset
    if (containsNonExistent._isSet().state && !containsNonExistent.state) {
      assertUnset.accept(nonExistentValue);
    }
  }

  @Test
  void testMultipleInstances() {
    EnvVars envVars1 = new EnvVars();
    EnvVars envVars2 = new EnvVars();
    
    // Both instances should be set
    assertSet.accept(envVars1._isSet());
    assertSet.accept(envVars2._isSet());
    
    // Both should return the same results for the same environment variable
    String path1 = envVars1.get(String._of("PATH"));
    String path2 = envVars2.get(String._of("PATH"));
    
    // Both should have the same set/unset state
    assertEquals(path1._isSet().state, path2._isSet().state);
    
    // If both are set, they should have the same value
    if (path1._isSet().state && path2._isSet().state) {
      assertEquals(path1.state, path2.state);
    }
  }

  @Test
  void testKeysAndGetConsistency() {
    EnvVars envVars = new EnvVars();
    
    // Get all environment variable keys
    StringInput keys = envVars.keys();
    
    // Get the first key
    if (keys.hasNext().state) {
      String firstKey = keys.next();
      assertSet.accept(firstKey);
      
      // The environment variable should exist according to contains
      Boolean contains = envVars._contains(firstKey);
      assertSet.accept(contains);
      assertTrue(contains.state);
      
      // The environment variable should have a value according to get
      String value = envVars.get(firstKey);
      assertSet.accept(value);
    }
  }
}