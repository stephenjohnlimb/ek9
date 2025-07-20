package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Aspect class.
 * Aspect is always set and serves as a base class for AOP implementations.
 */
class AspectTest extends Common {

  // Constructor Tests

  @Test
  void testDefaultConstructor() {
    Aspect aspect = new Aspect();
    assertNotNull(aspect);

    // Aspect should always be set (stateless utility)
    assertSet.accept(aspect);
    assertEquals("Aspect{}", aspect._string().state);
    
    // Should always return true for isSet
    Boolean isSetResult = aspect._isSet();
    assertSet.accept(isSetResult);
    assertTrue(isSetResult.state);
  }


  // AOP Method Tests

  @Test
  void testBeforeAdviceWithValidJoinPoint() {
    Aspect aspect = new Aspect();
    JoinPoint validJoinPoint = JoinPoint._of("TestComponent", "testMethod");
    
    PreparedMetaData result = aspect.beforeAdvice(validJoinPoint);
    assertNotNull(result);
    assertSet.accept(result);
    
    // Should create PreparedMetaData containing the JoinPoint
    JoinPoint resultJoinPoint = result.joinPoint();
    assertSet.accept(resultJoinPoint);
    assertEquals("TestComponent", resultJoinPoint.componentName().state);
    assertEquals("testMethod", resultJoinPoint.methodName().state);
  }

  @Test
  void testBeforeAdviceWithUnsetJoinPoint() {
    Aspect aspect = new Aspect();
    JoinPoint unsetJoinPoint = new JoinPoint(); // Unset
    assertNotNull(aspect);
    assertNotNull(unsetJoinPoint);
    
    PreparedMetaData result = aspect.beforeAdvice(unsetJoinPoint);
    assertNotNull(result);
    assertUnset.accept(result); // Should return unset PreparedMetaData
  }


  @Test
  void testAfterAdviceWithValidPreparedMetaData() {
    Aspect aspect = new Aspect();
    PreparedMetaData validMetaData = PreparedMetaData._of("Component", "method");
    assertNotNull(aspect);
    assertNotNull(validMetaData);
    
    // Base implementation is no-op, should not throw
    assertDoesNotThrow(() -> aspect.afterAdvice(validMetaData));
  }

  @Test
  void testAfterAdviceWithUnsetPreparedMetaData() {
    Aspect aspect = new Aspect();
    PreparedMetaData unsetMetaData = new PreparedMetaData(); // Unset
    assertNotNull(aspect);
    assertNotNull(unsetMetaData);
    
    // Base implementation should handle unset data gracefully
    assertDoesNotThrow(() -> aspect.afterAdvice(unsetMetaData));
  }


  // Round-trip AOP Test

  @Test
  void testCompleteAOPCycle() {
    // Verify complete AOP cycle and extensibility pattern
    Aspect aspect = new Aspect();
    JoinPoint originalJoinPoint = JoinPoint._of("Service", "process");
    assertNotNull(aspect);
    assertNotNull(originalJoinPoint);
    
    // Before advice: JoinPoint -> PreparedMetaData
    PreparedMetaData metaData = aspect.beforeAdvice(originalJoinPoint);
    assertSet.accept(metaData);
    
    // Verify the PreparedMetaData contains the original JoinPoint data
    JoinPoint retrievedJoinPoint = metaData.joinPoint();
    assertTrue(originalJoinPoint._eq(retrievedJoinPoint).state);
    
    // After advice: should complete without error (base implementation)
    assertDoesNotThrow(() -> aspect.afterAdvice(metaData));
    
    // Verify Aspect remains stateless and always set throughout
    assertSet.accept(aspect);
    assertTrue(aspect._isSet().state);
  }

  // Edge Cases and Null Safety

  @Test
  void testNullSafety() {
    Aspect aspect = new Aspect();
    assertNotNull(aspect);
    
    // All methods should handle null gracefully
    PreparedMetaData result = aspect.beforeAdvice(null);
    assertNotNull(result);
    assertUnset.accept(result); // Should return unset PreparedMetaData
    
    assertDoesNotThrow(() -> aspect.afterAdvice(null));
    
    // Constructor should not fail
    assertDoesNotThrow(Aspect::new);
  }

  @Test
  void testStringRepresentation() {
    Aspect aspect = new Aspect();
    String stringRep = aspect._string();
    assertSet.accept(stringRep);
    assertEquals("Aspect{}", stringRep.state);
  }

  @Test
  void testMultipleInstances() {
    // Multiple Aspect instances should behave identically (stateless)
    Aspect aspect1 = new Aspect();
    Aspect aspect2 = new Aspect();
    assertNotNull(aspect1);
    assertNotNull(aspect2);
    
    // Both should be set
    assertSet.accept(aspect1);
    assertSet.accept(aspect2);
    
    // Both should have same string representation
    assertEquals(aspect1._string().state, aspect2._string().state);
    
    // Both should handle same input identically
    JoinPoint testJoinPoint = JoinPoint._of("Test", "method");
    PreparedMetaData result1 = aspect1.beforeAdvice(testJoinPoint);
    PreparedMetaData result2 = aspect2.beforeAdvice(testJoinPoint);
    
    assertTrue(result1._eq(result2).state);
  }

  @Test
  void testStatelessBehavior() {
    Aspect aspect = new Aspect();
    assertNotNull(aspect);
    
    // Multiple calls should not affect state (stateless utility)
    JoinPoint jp1 = JoinPoint._of("Class1", "method1");
    JoinPoint jp2 = JoinPoint._of("Class2", "method2");
    
    PreparedMetaData result1 = aspect.beforeAdvice(jp1);
    PreparedMetaData result2 = aspect.beforeAdvice(jp2);
    
    // Results should be different (contain different JoinPoints)
    assertSet.accept(result1);
    assertSet.accept(result2);
    
    // But aspect itself should remain in same state
    assertSet.accept(aspect);
    assertTrue(aspect._isSet().state);
  }

  @Test
  void testBeforeAdviceInputValidation() {
    Aspect aspect = new Aspect();
    assertNotNull(aspect);
    
    // Test various invalid JoinPoint scenarios
    JoinPoint nullComponentJoinPoint = new JoinPoint(null, String._of("method"));
    JoinPoint nullMethodJoinPoint = new JoinPoint(String._of("component"), null);
    JoinPoint bothNullJoinPoint = new JoinPoint(null, null);
    
    // All should return unset PreparedMetaData
    assertUnset.accept(aspect.beforeAdvice(nullComponentJoinPoint));
    assertUnset.accept(aspect.beforeAdvice(nullMethodJoinPoint));
    assertUnset.accept(aspect.beforeAdvice(bothNullJoinPoint));
  }

}