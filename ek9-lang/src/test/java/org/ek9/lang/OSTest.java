package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the OS class.
 * Note: OS is stateless and isSet() always returns true.
 */
class OSTest extends Common {

  // Constructor Tests

  @Test
  void testDefaultConstructor() {
    OS os = new OS();
    assertNotNull(os);
    
    // OS is always set since it's stateless
    assertTrue(os._isSet().state);
  }

  // PID Tests

  @Test
  void testPidReturnsValidValue() {
    OS os = new OS();
    assertNotNull(os);
    Integer pid = os.pid();
    
    assertSet.accept(pid);
    assertTrue(pid.state > 0); // PIDs should be positive
    
    // PID should be within reasonable range for most systems
    assertTrue(pid.state < java.lang.Integer.MAX_VALUE);
  }

  @Test
  void testPidConsistency() {
    OS os = new OS();
    assertNotNull(os);
    
    Integer pid1 = os.pid();
    Integer pid2 = os.pid();
    
    assertSet.accept(pid1);
    assertSet.accept(pid2);
    assertEquals(pid1.state, pid2.state);
  }

  @Test
  void testPidFromMultipleInstances() {
    OS os1 = new OS();
    OS os2 = new OS();
    
    assertNotNull(os1);
    assertNotNull(os2);
    
    Integer pid1 = os1.pid();
    Integer pid2 = os2.pid();
    
    assertSet.accept(pid1);
    assertSet.accept(pid2);
    assertEquals(pid1.state, pid2.state); // Same process, same PID
  }

  @Test
  void testPidFormat() {
    OS os = new OS();
    assertNotNull(os);
    Integer pid = os.pid();
    
    assertSet.accept(pid);
    
    // PID should be a reasonable positive integer
    assertTrue(pid.state > 0);
    assertTrue(pid.state < 1000000); // Most systems have PIDs < 1M
  }

  @Test
  void testPidRange() {
    OS os = new OS();
    assertNotNull(os);
    Integer pid = os.pid();
    
    assertSet.accept(pid);
    
    // PID should be within valid range
    assertTrue(pid.state >= 1); // PIDs start from 1 (init process is 1)
    assertTrue(pid.state <= java.lang.Integer.MAX_VALUE);
  }

  // isSet Tests

  @Test
  void testIsSetAlwaysTrue() {
    OS os = new OS();
    assertNotNull(os);
    assertTrue(os._isSet().state);
    
    // Create multiple instances
    OS os2 = new OS();
    assertNotNull(os2);
    assertTrue(os2._isSet().state);
  }

  @Test
  void testIsSetAfterPidCall() {
    OS os = new OS();
    assertNotNull(os);
    
    // Should be set before and after pid() call
    assertTrue(os._isSet().state);
    
    Integer pid = os.pid();
    assertSet.accept(pid);
    
    assertTrue(os._isSet().state);
  }

  // Cross-platform Compatibility Tests

  @Test
  void testMultipleCallsPerformance() {
    OS os = new OS();
    assertNotNull(os);
    
    // Should be fast to call multiple times
    for (int i = 0; i < 100; i++) {
      Integer pid = os.pid();
      assertSet.accept(pid);
      assertTrue(pid.state > 0);
    }
  }

  // Error Handling Tests

  @Test
  void testNoExceptionsThrown() {
    OS os = new OS();
    assertNotNull(os);
    
    // Test that no exceptions are thrown for any operations
    assertDoesNotThrow(() -> {
      os.pid();
      os._isSet();
    });
  }

  @Test
  void testStatelessBehavior() {
    OS os = new OS();
    assertNotNull(os);
    
    // Multiple calls should be independent and consistent
    Integer pid1 = os.pid();
    Integer pid2 = os.pid();
    Integer pid3 = os.pid();
    
    assertSet.accept(pid1);
    assertSet.accept(pid2);
    assertSet.accept(pid3);
    
    assertEquals(pid1.state, pid2.state);
    assertEquals(pid2.state, pid3.state);
  }

  // Integration Tests

  @Test
  void testOSIntegration() {
    OS os = new OS();
    assertNotNull(os);
    
    // Test that OS integrates properly with EK9 type system
    assertTrue(os._isSet().state);
    
    Integer pid = os.pid();
    assertSet.accept(pid);
    
    // PID should be the current JVM process ID
    long actualPid = ProcessHandle.current().pid();
    assertEquals((int) actualPid, pid.state);
  }

  @Test
  void testProcessHandleIntegration() {
    OS os = new OS();
    assertNotNull(os);
    
    // Verify that our implementation matches Java's ProcessHandle
    Integer osPid = os.pid();
    long javaProcessPid = ProcessHandle.current().pid();
    
    assertSet.accept(osPid);
    assertEquals((int) javaProcessPid, osPid.state);
  }
}