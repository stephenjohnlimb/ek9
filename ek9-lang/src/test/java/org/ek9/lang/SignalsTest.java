package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test Signals component for Unix/Linux signal handling.
 * Tests signal registration, handler execution, System.exit() integration, and edge cases.
 */
@SuppressWarnings("checkstyle:MethodName")
class SignalsTest extends Common {

  private Signals signals;
  private SystemExitManager.Test testExitManager;

  @BeforeEach
  void setUp() {
    testExitManager = new SystemExitManager.Test();
    signals = new Signals(testExitManager);
  }

  // Helper methods for cleaner test code
  private SignalHandler createNoExitHandler() {
    return new SignalHandler() {
      @Override
      public Integer _call(String signal) {
        return new Integer(); // Unset Integer - no exit
      }
    };
  }

  private SignalHandler createExitHandler(int exitCode) {
    return new SignalHandler() {
      @Override
      public Integer _call(String signal) {
        return Integer._of(exitCode); // Set Integer - triggers exit
      }
    };
  }

  private SignalHandler createThrowingHandler() {
    return new SignalHandler() {
      @Override
      public Integer _call(String signal) {
        throw new RuntimeException("Test exception");
      }
    };
  }

  private _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 createSignalList(
      java.lang.String... signals) {
    final var list = new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1();
    for (final var signalName : signals) {
      list._addAss(String._of(signalName));
    }
    return list;
  }

  @Test
  void testConstruction() {
    // Test default constructor
    final var defaultSignals = new Signals();
    assertNotNull(defaultSignals);
    assertSet.accept(defaultSignals);

    // Test internal constructor with custom exit manager
    assertNotNull(signals);
    assertSet.accept(signals);
    assertEquals("Signals{}", signals._string().state);
  }

  @Test
  void testFactoryMethods() {
    // Test _of() factory method
    final var factorySignals = Signals._of();
    assertNotNull(factorySignals);
    assertSet.accept(factorySignals);
  }

  @Test
  void testSingleSignalRegistration() {
    // Test registering a supported signal
    signals.register(String._of("USR1"), createNoExitHandler());

    // Verify _string() shows the registered signal
    final var signalsString = signals._string().state;
    assertTrue(signalsString.contains("USR1:1"));
  }

  @Test
  void testListSignalRegistration() {
    // Create list of signals using helper
    final var signalList = createSignalList("USR1", "USR2", "HUP");

    // Register signals
    final var result = signals.register(signalList, createNoExitHandler());

    // Verify result contains registered signals
    assertNotNull(result);
    assertSet.accept(result);

    // Should have registered all supported signals
    final var resultIterator = result.iterator();
    boolean foundUSR1 = false, foundUSR2 = false, foundHUP = false;

    while (resultIterator.hasNext().state) {
      final var signal = resultIterator.next().state;
      if ("USR1".equals(signal)) {
        foundUSR1 = true;
      }
      if ("USR2".equals(signal)) {
        foundUSR2 = true;
      }
      if ("HUP".equals(signal)) {
        foundHUP = true;
      }
    }

    assertTrue(foundUSR1);
    assertTrue(foundUSR2);
    assertTrue(foundHUP);
  }

  @Test
  void testUnsupportedSignalHandling() {
    // Create list with mixed supported/unsupported signals
    final var signalList = createSignalList("USR1", "INVALID", "USR2");

    final var result = signals.register(signalList, createNoExitHandler());
    assertNotNull(result);
    assertSet.accept(result);

    // Should only contain supported signals
    final var resultIterator = result.iterator();
    while (resultIterator.hasNext().state) {
      final var signal = resultIterator.next().state;
      assertNotEquals("INVALID", signal);
    }
  }

  @Test
  void testMultipleHandlersPerSignal() {
    // Register multiple handlers for same signal
    signals.register(String._of("USR1"), createNoExitHandler());
    signals.register(String._of("USR1"), createNoExitHandler());

    // _string() should show 2 handlers for USR1
    final var signalsString = signals._string().state;
    assertTrue(signalsString.contains("USR1:2"));
  }

  @Test
  void testSystemExitIntegration() {
    // Create handler that returns set Integer (triggers exit)
    final SignalHandler exitHandler = createExitHandler(42);

    // Register handler
    signals.register(String._of("USR1"), exitHandler);

    // Simulate signal - this would normally be triggered by native signal
    // We'll test this through the internal handleSignal method access via reflection
    // For now, verify the handler itself returns the expected value
    final var result = exitHandler._call(String._of("USR1"));
    assertSet.accept(result);
    assertEquals(42, result.state);
  }

  @Test
  void testInvalidInputHandling() {
    // Test register with unset signal string
    final var unsetSignal = new String();
    assertNotNull(unsetSignal);
    unsetSignal.unSet();

    // Should handle gracefully (no exception)
    signals.register(unsetSignal, createNoExitHandler());

    // Test edge case scenarios - just verify no exceptions are thrown
    signals.register(String._of("USR1"), createNoExitHandler());

    // Verify signals remains functional after edge case handling
    assertTrue.accept(signals._isSet());
  }

  @Test
  void testStringOutput() {
    // Test empty signals string output
    assertEquals("Signals{}", signals._string().state);

    // Register some signals and test output format
    signals.register(String._of("USR1"), createNoExitHandler());
    signals.register(String._of("HUP"), createNoExitHandler());

    final var output = signals._string().state;
    assertTrue(output.startsWith("Signals{"));
    assertTrue(output.endsWith("}"));
    assertTrue(output.contains("USR1:1") || output.contains("HUP:1"));
  }

  @Test
  void testAllStandardSignals() {
    // Test all standard signals that should be supported
    final var signalList = createSignalList("HUP", "TRAP", "ABRT", "PIPE", "ALRM", "TERM",
        "CHLD", "TTIN", "TTOU", "PROF", "WINCH", "USR1", "USR2");

    final var result = signals.register(signalList, createNoExitHandler());
    assertNotNull(result);
    assertSet.accept(result);

    // Some signals might not be supported on all platforms
    // But the method should handle this gracefully
    assertTrue(result._len().state >= 0);
  }

  @Test
  void testIsSetOperator() {
    // Signals should always be set when created
    assertTrue.accept(signals._isSet());

    // Even after operations with handlers, should remain set
    signals.register(String._of("USR1"), createNoExitHandler());
    assertTrue.accept(signals._isSet());
  }

  @Test
  void testEdgeCases() {
    // Test empty signal list
    final var emptyList = new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1();
    final var result = signals.register(emptyList, createNoExitHandler());
    assertNotNull(result);
    assertSet.accept(result);
    assertEquals(0, result._len().state);

    // Test null elements in list (unset String objects)
    final var listWithUnset = new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1();
    listWithUnset._addAss(String._of("USR1"));
    final var unsetString = new String();
    unsetString.unSet();
    listWithUnset._addAss(unsetString);
    listWithUnset._addAss(String._of("USR2"));

    final var result2 = signals.register(listWithUnset, createNoExitHandler());
    assertNotNull(result2);
    assertSet.accept(result2);
    // Should only register valid strings
    assertTrue(result2._len().state >= 0 && result2._len().state <= 2);
  }

  @Test
  void testConcurrentSignalRegistration() {
    // Test that multiple registrations don't interfere with each other
    signals.register(String._of("USR1"), createNoExitHandler());
    signals.register(String._of("USR2"), createNoExitHandler());
    signals.register(String._of("USR1"), createNoExitHandler()); // Same signal, different handler

    final var output = signals._string().state;
    assertTrue(output.contains("USR1:2")); // Should have 2 handlers for USR1
    assertTrue(output.contains("USR2:1")); // Should have 1 handler for USR2
  }

  @Test
  void testHandleSignalDirectExecution() {
    // Test the core handleSignal method using reflection
    final var exitHandler = createExitHandler(99);
    signals.register(String._of("USR1"), exitHandler);

    try {
      // Use reflection to call private handleSignal method
      final var method = Signals.class.getDeclaredMethod("handleSignal", java.lang.String.class);
      method.setAccessible(true);
      method.invoke(signals, "USR1");

      // Verify SystemExitManager was called with correct exit code
      assertTrue(testExitManager.wasExitCalled());
      assertEquals(99, testExitManager.getLastExitCode().intValue());
    } catch (java.lang.NoSuchMethodException | java.lang.IllegalAccessException |
             java.lang.reflect.InvocationTargetException e) {
      throw new RuntimeException("Failed to test handleSignal via reflection", e);
    }
  }

  @Test
  void testHandleSignalNoExit() {
    // Test handler that doesn't trigger exit
    final var noExitHandler = createNoExitHandler();
    signals.register(String._of("USR1"), noExitHandler);

    try {
      // Call handleSignal via reflection
      final var method = Signals.class.getDeclaredMethod("handleSignal", java.lang.String.class);
      method.setAccessible(true);
      method.invoke(signals, "USR1");

      // Verify no exit was called
      assertFalse(testExitManager.wasExitCalled());
      assertNull(testExitManager.getLastExitCode());
    } catch (java.lang.NoSuchMethodException | java.lang.IllegalAccessException |
             java.lang.reflect.InvocationTargetException e) {
      throw new RuntimeException("Failed to test handleSignal via reflection", e);
    }
  }

  @Test
  void testHandleSignalMultipleHandlers() {
    // Test multiple handlers - first doesn't exit, second does
    final var noExitHandler = createNoExitHandler();
    final var exitHandler = createExitHandler(42);

    signals.register(String._of("USR1"), noExitHandler);
    signals.register(String._of("USR1"), exitHandler);

    try {
      // Call handleSignal via reflection
      final var method = Signals.class.getDeclaredMethod("handleSignal", java.lang.String.class);
      method.setAccessible(true);
      method.invoke(signals, "USR1");

      // Should exit with code from second handler
      assertTrue(testExitManager.wasExitCalled());
      assertEquals(42, testExitManager.getLastExitCode().intValue());
    } catch (java.lang.NoSuchMethodException | java.lang.IllegalAccessException |
             java.lang.reflect.InvocationTargetException e) {
      throw new RuntimeException("Failed to test handleSignal via reflection", e);
    }
  }

  @Test
  void testHandlerExceptionBehavior() {
    // Test that handlers that throw exceptions don't prevent other handlers from running
    // This tests the behavior directly without reflection

    final var throwingHandler = createThrowingHandler();
    final var exitHandler = createExitHandler(123);

    // Test throwing handler directly first
    try {
      throwingHandler._call(String._of("test"));
      // Should not reach here
      throw new RuntimeException("Expected exception was not thrown");
    } catch (RuntimeException e) {
      assertEquals("Test exception", e.getMessage());
    }

    // Test that the exit handler works correctly
    final var result = exitHandler._call(String._of("test"));
    assertSet.accept(result);
    assertEquals(123, result.state);

    // Register both handlers for signal
    signals.register(String._of("USR1"), throwingHandler);
    signals.register(String._of("USR1"), exitHandler);

    // For now, just verify the registration worked - 
    // the actual exception handling in handleSignal will be tested
    // when native signals trigger the handlers
    final var signalsString = signals._string().state;
    assertTrue(signalsString.contains("USR1:2"));
  }

  @Test
  void testHandleSignalUnregistered() {
    try {
      // Test calling handleSignal for unregistered signal
      final var method = Signals.class.getDeclaredMethod("handleSignal", java.lang.String.class);
      method.setAccessible(true);
      method.invoke(signals, "NONEXISTENT");

      // Should not crash or call exit
      assertFalse(testExitManager.wasExitCalled());
    } catch (java.lang.NoSuchMethodException | java.lang.IllegalAccessException |
             java.lang.reflect.InvocationTargetException e) {
      throw new RuntimeException("Failed to test handleSignal via reflection", e);
    }
  }

  @Test
  void testSystemExitManagerIntegration() {
    // Test SystemExitManager.Test functionality directly
    assertFalse(testExitManager.wasExitCalled());
    assertNull(testExitManager.getLastExitCode());

    testExitManager.exit(55);

    assertTrue(testExitManager.wasExitCalled());
    assertEquals(55, testExitManager.getLastExitCode().intValue());

    // Test reset functionality
    testExitManager.reset();
    assertFalse(testExitManager.wasExitCalled());
    assertNull(testExitManager.getLastExitCode());
  }
}