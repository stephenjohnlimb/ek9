package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the HMAC class.
 * Note: HMAC is stateless and isSet() always returns true.
 */
class HMACTest extends Common {

  // Constructor Tests

  @Test
  void testDefaultConstructor() {
    HMAC hmac = new HMAC();
    assertNotNull(hmac);
    
    // HMAC is always set since it's stateless
    assertTrue(hmac._isSet().state);
  }

  // SHA256 String Tests

  @Test
  void testSHA256WithValidString() {
    HMAC hmac = new HMAC();
    String input = String._of("hello");
    String result = hmac.SHA256(input);
    
    assertSet.accept(result);
    assertFalse(result.state.isEmpty());
    
    // Should be 64 character hex string (32 bytes * 2 hex chars)
    assertEquals(64, result.state.length());
    
    // Should contain only hex characters
    assertTrue(result.state.matches("^[0-9a-f]+$"));
  }

  @Test
  void testSHA256WithEmptyString() {
    HMAC hmac = new HMAC();
    String input = String._of("");
    String result = hmac.SHA256(input);
    
    assertSet.accept(result);
    assertEquals(64, result.state.length());
    
    // Empty string should have known SHA256 hash
    assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", 
                 result.state);
  }

  @Test
  void testSHA256WithNullString() {
    HMAC hmac = new HMAC();
    assertNotNull(hmac);
    String result = hmac.SHA256((String) null);
    
    assertUnset.accept(result);
  }

  @Test
  void testSHA256WithUnsetString() {
    HMAC hmac = new HMAC();
    assertNotNull(hmac);
    String unsetString = new String(); // Unset string
    String result = hmac.SHA256(unsetString);
    
    assertUnset.accept(result);
  }

  @Test
  void testSHA256StringConsistency() {
    HMAC hmac = new HMAC();
    String input = String._of("test");
    
    String result1 = hmac.SHA256(input);
    String result2 = hmac.SHA256(input);
    
    assertSet.accept(result1);
    assertSet.accept(result2);
    assertEquals(result1.state, result2.state);
  }

  @Test
  void testSHA256StringDifferentInputs() {
    HMAC hmac = new HMAC();
    String input1 = String._of("hello");
    String input2 = String._of("world");
    
    String result1 = hmac.SHA256(input1);
    String result2 = hmac.SHA256(input2);
    
    assertSet.accept(result1);
    assertSet.accept(result2);
    assertNotEquals(result1.state, result2.state);
  }

  // SHA256 GUID Tests

  @Test
  void testSHA256WithValidGUID() {
    HMAC hmac = new HMAC();
    GUID guid = new GUID();
    String result = hmac.SHA256(guid);
    
    assertSet.accept(result);
    assertFalse(result.state.isEmpty());
    assertEquals(64, result.state.length());
    assertTrue(result.state.matches("^[0-9a-f]+$"));
  }

  @Test
  void testSHA256WithNullGUID() {
    HMAC hmac = new HMAC();
    assertNotNull(hmac);
    String result = hmac.SHA256((GUID) null);
    
    assertUnset.accept(result);
  }

  @Test
  void testSHA256GUIDConsistency() {
    HMAC hmac = new HMAC();
    GUID guid = new GUID();
    
    String result1 = hmac.SHA256(guid);
    String result2 = hmac.SHA256(guid);
    
    assertSet.accept(result1);
    assertSet.accept(result2);
    assertEquals(result1.state, result2.state);
  }

  @Test
  void testSHA256GUIDDifferentInputs() {
    HMAC hmac = new HMAC();
    GUID guid1 = new GUID();
    GUID guid2 = new GUID();
    
    String result1 = hmac.SHA256(guid1);
    String result2 = hmac.SHA256(guid2);
    
    assertSet.accept(result1);
    assertSet.accept(result2);
    // Different GUIDs should produce different hashes
    assertNotEquals(result1.state, result2.state);
  }

  @Test
  void testSHA256GUIDVsStringEquivalence() {
    HMAC hmac = new HMAC();
    GUID guid = new GUID();
    String guidStr = guid._string();
    
    String resultFromGuid = hmac.SHA256(guid);
    String resultFromString = hmac.SHA256(guidStr);
    
    assertSet.accept(resultFromGuid);
    assertSet.accept(resultFromString);
    assertEquals(resultFromGuid.state, resultFromString.state);
  }

  // isSet Tests

  @Test
  void testIsSetAlwaysTrue() {
    HMAC hmac = new HMAC();
    assertTrue(hmac._isSet().state);
    
    // Create multiple instances
    HMAC hmac2 = new HMAC();
    assertTrue(hmac2._isSet().state);
  }

  // Known Hash Values Tests

  @Test
  void testKnownHashValues() {
    HMAC hmac = new HMAC();
    
    // Test known SHA256 hash for "hello"
    String input = String._of("hello");
    String result = hmac.SHA256(input);
    
    assertSet.accept(result);
    assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", 
                 result.state);
  }

  @Test
  void testMultipleInstances() {
    HMAC hmac1 = new HMAC();
    HMAC hmac2 = new HMAC();
    
    String input = String._of("test");
    String result1 = hmac1.SHA256(input);
    String result2 = hmac2.SHA256(input);
    
    assertSet.accept(result1);
    assertSet.accept(result2);
    assertEquals(result1.state, result2.state);
  }

  @Test
  void testNoExceptionsThrown() {
    HMAC hmac = new HMAC();
    
    // Test that no exceptions are thrown for any operations
    assertDoesNotThrow(() -> {
      hmac.SHA256(String._of("test"));
      hmac.SHA256(new GUID());
      hmac._isSet();
    });
  }
}