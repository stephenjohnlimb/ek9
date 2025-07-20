package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the JoinPoint class.
 * JoinPoint is set only when both componentName and methodName are set.
 */
class JoinPointTest extends Common {

  // Constructor Tests

  @Test
  void testDefaultConstructor() {
    JoinPoint joinPoint = new JoinPoint();
    assertNotNull(joinPoint);

    // Default constructor should create unset JoinPoint
    assertUnset.accept(joinPoint);
    assertEquals("JoinPoint{}", joinPoint._string().state);
    
    // Accessor methods should return unset strings
    assertUnset.accept(joinPoint.componentName());
    assertUnset.accept(joinPoint.methodName());
  }

  @Test
  void testParameterizedConstructorValid() {
    String component = String._of("TestComponent");
    String method = String._of("testMethod");
    JoinPoint joinPoint = new JoinPoint(component, method);
    
    assertNotNull(joinPoint);
    assertSet.accept(joinPoint);
    
    // Should contain both values
    assertTrue(joinPoint._string().state.contains("TestComponent"));
    assertTrue(joinPoint._string().state.contains("testMethod"));
    
    // Accessor methods should return the values
    assertEquals("TestComponent", joinPoint.componentName().state);
    assertEquals("testMethod", joinPoint.methodName().state);
  }

  @Test
  void testParameterizedConstructorWithNullComponent() {
    String method = String._of("testMethod");
    JoinPoint joinPoint = new JoinPoint(null, method);
    
    assertNotNull(joinPoint);
    assertUnset.accept(joinPoint); // Should be unset due to null component
    
    assertEquals("JoinPoint{}", joinPoint._string().state);
  }

  @Test
  void testParameterizedConstructorWithNullMethod() {
    String component = String._of("TestComponent");
    JoinPoint joinPoint = new JoinPoint(component, null);
    
    assertNotNull(joinPoint);
    assertUnset.accept(joinPoint); // Should be unset due to null method
    
    assertEquals("JoinPoint{}", joinPoint._string().state);
  }

  @Test
  void testParameterizedConstructorWithUnsetComponent() {
    String component = new String(); // Unset
    String method = String._of("testMethod");
    JoinPoint joinPoint = new JoinPoint(component, method);
    
    assertNotNull(joinPoint);
    assertUnset.accept(joinPoint); // Should be unset due to unset component
  }

  @Test
  void testParameterizedConstructorWithUnsetMethod() {
    String component = String._of("TestComponent");
    String method = new String(); // Unset
    JoinPoint joinPoint = new JoinPoint(component, method);
    
    assertNotNull(joinPoint);
    assertUnset.accept(joinPoint); // Should be unset due to unset method
  }

  @Test
  void testParameterizedConstructorWithBothUnset() {
    String component = new String(); // Unset
    String method = new String(); // Unset
    JoinPoint joinPoint = new JoinPoint(component, method);
    
    assertNotNull(joinPoint);
    assertUnset.accept(joinPoint); // Should be unset
  }

  // Factory Method Tests

  @Test
  void testFactoryMethodWithStrings() {
    JoinPoint joinPoint = JoinPoint._of(String._of("Component"), String._of("method"));
    
    assertNotNull(joinPoint);
    assertSet.accept(joinPoint);
    assertEquals("Component", joinPoint.componentName().state);
    assertEquals("method", joinPoint.methodName().state);
  }

  @Test
  void testFactoryMethodWithJavaStrings() {
    JoinPoint joinPoint = JoinPoint._of("Component", "method");
    
    assertNotNull(joinPoint);
    assertSet.accept(joinPoint);
    assertEquals("Component", joinPoint.componentName().state);
    assertEquals("method", joinPoint.methodName().state);
  }


  // Accessor Method Tests

  @Test
  void testAccessorMethodsSetState() {
    JoinPoint joinPoint = JoinPoint._of("TestClass", "testMethod");
    assertSet.accept(joinPoint);
    
    String componentName = joinPoint.componentName();
    String methodName = joinPoint.methodName();
    
    assertSet.accept(componentName);
    assertSet.accept(methodName);
    assertEquals("TestClass", componentName.state);
    assertEquals("testMethod", methodName.state);
  }

  @Test
  void testAccessorMethodsUnsetState() {
    JoinPoint joinPoint = new JoinPoint();
    assertNotNull(joinPoint);
    assertUnset.accept(joinPoint);
    
    String componentName = joinPoint.componentName();
    String methodName = joinPoint.methodName();
    
    assertUnset.accept(componentName);
    assertUnset.accept(methodName);
  }

  // Equality Operator Tests

  @Test
  void testEqualityWithSameValues() {
    JoinPoint jp1 = JoinPoint._of("Component", "method");
    JoinPoint jp2 = JoinPoint._of("Component", "method");
    
    Boolean result = jp1._eq(jp2);
    assertSet.accept(result);
    assertTrue(result.state);
  }

  @Test
  void testEqualityWithDifferentComponent() {
    JoinPoint jp1 = JoinPoint._of("Component1", "method");
    JoinPoint jp2 = JoinPoint._of("Component2", "method");
    
    Boolean result = jp1._eq(jp2);
    assertSet.accept(result);
    assertFalse(result.state);
  }

  @Test
  void testEqualityWithDifferentMethod() {
    JoinPoint jp1 = JoinPoint._of("Component", "method1");
    JoinPoint jp2 = JoinPoint._of("Component", "method2");
    
    Boolean result = jp1._eq(jp2);
    assertSet.accept(result);
    assertFalse(result.state);
  }

  @Test
  void testEqualityWithUnsetJoinPoint() {
    JoinPoint setJp = JoinPoint._of("Component", "method");
    assertNotNull(setJp);
    JoinPoint unsetJp = new JoinPoint();
    
    Boolean result = setJp._eq(unsetJp);
    assertUnset.accept(result); // Should return unset
  }

  @Test
  void testEqualityWithNullJoinPoint() {
    JoinPoint jp = JoinPoint._of("Component", "method");
    assertNotNull(jp);
    
    Boolean result = jp._eq(null);
    assertUnset.accept(result); // Should return unset
  }

  @Test
  void testEqualityWithAnyType() {
    JoinPoint jp = JoinPoint._of("Component", "method");
    
    // Test with non-JoinPoint Any
    Boolean result = jp._eq(String._of("test"));
    assertUnset.accept(result); // Should return unset for non-JoinPoint
    
    // Test with JoinPoint as Any
    JoinPoint jp2 = JoinPoint._of("Component", "method");
    Boolean result2 = jp._eq((Any) jp2);
    assertSet.accept(result2);
    assertTrue(result2.state);
  }

  // Inequality Operator Tests

  @Test
  void testInequalityOperator() {
    JoinPoint jp1 = JoinPoint._of("Component1", "method");
    JoinPoint jp2 = JoinPoint._of("Component2", "method");
    
    Boolean result = jp1._neq(jp2);
    assertSet.accept(result);
    assertTrue(result.state);
  }

  @Test
  void testInequalityWithSameValues() {
    JoinPoint jp1 = JoinPoint._of("Component", "method");
    JoinPoint jp2 = JoinPoint._of("Component", "method");
    
    Boolean result = jp1._neq(jp2);
    assertSet.accept(result);
    assertFalse(result.state);
  }

  // Comparison Operator Tests

  @Test
  void testComparisonByComponent() {
    JoinPoint jp1 = JoinPoint._of("Alpha", "method");
    JoinPoint jp2 = JoinPoint._of("Beta", "method");
    
    Integer result = jp1._cmp(jp2);
    assertSet.accept(result);
    assertTrue(result.state < 0); // Alpha < Beta
    
    Integer result2 = jp2._cmp(jp1);
    assertSet.accept(result2);
    assertTrue(result2.state > 0); // Beta > Alpha
  }

  @Test
  void testComparisonByMethod() {
    JoinPoint jp1 = JoinPoint._of("Component", "methodA");
    JoinPoint jp2 = JoinPoint._of("Component", "methodB");
    
    Integer result = jp1._cmp(jp2);
    assertSet.accept(result);
    assertTrue(result.state < 0); // methodA < methodB
  }

  @Test
  void testComparisonEqual() {
    JoinPoint jp1 = JoinPoint._of("Component", "method");
    JoinPoint jp2 = JoinPoint._of("Component", "method");
    
    Integer result = jp1._cmp(jp2);
    assertSet.accept(result);
    assertEquals(0, result.state);
  }

  @Test
  void testComparisonWithUnsetJoinPoint() {
    JoinPoint setJp = JoinPoint._of("Component", "method");
    assertNotNull(setJp);
    JoinPoint unsetJp = new JoinPoint();
    
    Integer result = setJp._cmp(unsetJp);
    assertUnset.accept(result); // Should return unset
  }

  @Test
  void testComparisonWithAnyType() {
    JoinPoint jp = JoinPoint._of("Component", "method");
    
    // Test with non-JoinPoint Any
    Integer result = jp._cmp(String._of("test"));
    assertUnset.accept(result); // Should return unset
    
    // Test with JoinPoint as Any
    JoinPoint jp2 = JoinPoint._of("Component", "method");
    Integer result2 = jp._cmp((Any) jp2);
    assertSet.accept(result2);
    assertEquals(0, result2.state);
  }

  // Assignment Operator Tests

  @Test
  void testCopyOperator() {
    JoinPoint source = JoinPoint._of("Source", "sourceMethod");
    JoinPoint target = new JoinPoint();
    assertUnset.accept(target);
    
    target._copy(source);
    assertSet.accept(target);
    assertEquals("Source", target.componentName().state);
    assertEquals("sourceMethod", target.methodName().state);
    
    // Should be equal but different objects
    assertTrue(source._eq(target).state);
    assertNotSame(source, target);
  }

  @Test
  void testCopyWithUnsetSource() {
    JoinPoint source = new JoinPoint();
    assertNotNull(source);
    JoinPoint target = JoinPoint._of("Target", "targetMethod");
    assertSet.accept(target);
    
    target._copy(source);
    assertUnset.accept(target); // Should become unset
  }

  @Test
  void testCopyWithNullSource() {
    JoinPoint target = JoinPoint._of("Target", "targetMethod");
    assertNotNull(target);
    assertSet.accept(target);
    
    target._copy(null);
    assertUnset.accept(target); // Should become unset
  }

  @Test
  void testReplaceOperator() {
    JoinPoint source = JoinPoint._of("Source", "sourceMethod");
    JoinPoint target = JoinPoint._of("Target", "targetMethod");
    
    target._replace(source);
    assertSet.accept(target);
    assertEquals("Source", target.componentName().state);
    assertEquals("sourceMethod", target.methodName().state);
  }

  @Test
  void testMergeOperatorWithUnsetTarget() {
    JoinPoint source = JoinPoint._of("Source", "sourceMethod");
    JoinPoint target = new JoinPoint(); // Unset
    
    target._merge(source);
    assertSet.accept(target);
    assertEquals("Source", target.componentName().state);
    assertEquals("sourceMethod", target.methodName().state);
  }

  @Test
  void testMergeOperatorWithSetTarget() {
    JoinPoint source = JoinPoint._of("Source", "sourceMethod");
    JoinPoint target = JoinPoint._of("Target", "targetMethod"); // Already set
    
    target._merge(source);
    assertSet.accept(target);
    // Should keep original values since target was already set
    assertEquals("Target", target.componentName().state);
    assertEquals("targetMethod", target.methodName().state);
  }

  // String and Hashcode Tests

  @Test
  void testStringRepresentation() {
    JoinPoint setJp = JoinPoint._of("TestClass", "testMethod");
    String stringRep = setJp._string();
    assertSet.accept(stringRep);
    
    java.lang.String expected = "JoinPoint{componentName: 'TestClass', methodName: 'testMethod'}";
    assertEquals(expected, stringRep.state);
  }

  @Test
  void testStringRepresentationUnset() {
    JoinPoint unsetJp = new JoinPoint();
    String stringRep = unsetJp._string();
    assertSet.accept(stringRep);
    assertEquals("JoinPoint{}", stringRep.state);
  }

  @Test
  void testPromoteOperator() {
    JoinPoint jp = JoinPoint._of("Component", "method");
    String promoted = jp._promote();
    assertSet.accept(promoted);
    assertEquals(jp._string().state, promoted.state);
  }

  @Test
  void testHashcodeOperator() {
    JoinPoint jp1 = JoinPoint._of("Component", "method");
    JoinPoint jp2 = JoinPoint._of("Component", "method");
    JoinPoint jp3 = JoinPoint._of("Different", "method");
    
    Integer hash1 = jp1._hashcode();
    Integer hash2 = jp2._hashcode();
    Integer hash3 = jp3._hashcode();
    
    assertSet.accept(hash1);
    assertSet.accept(hash2);
    assertSet.accept(hash3);
    
    // Same values should have same hashcode
    assertEquals(hash1.state, hash2.state);
    
    // Different values should have different hashcode (usually)
    assertNotEquals(hash1.state, hash3.state);
  }

  @Test
  void testHashcodeUnset() {
    JoinPoint unsetJp = new JoinPoint();
    assertNotNull(unsetJp);
    Integer hash = unsetJp._hashcode();
    assertUnset.accept(hash);
  }

  // Edge Cases and Null Safety

  @Test
  void testNullSafety() {
    // All methods should handle null gracefully
    assertDoesNotThrow(() -> new JoinPoint(null, null));
    assertDoesNotThrow(() -> JoinPoint._of(null, (String) null));
    assertDoesNotThrow(() -> JoinPoint._of( null, (java.lang.String) null));
    
    JoinPoint jp = JoinPoint._of("Component", "method");
    assertDoesNotThrow(() -> jp._eq( null));
    assertDoesNotThrow(() -> jp._neq(null));
    assertDoesNotThrow(() -> jp._cmp(null));
    assertDoesNotThrow(() -> jp._copy(null));
    assertDoesNotThrow(() -> jp._replace(null));
    assertDoesNotThrow(() -> jp._merge(null));
  }

  @Test
  void testEmptyStringsVsUnsetStrings() {
    // Empty strings should still be considered set
    JoinPoint jp1 = JoinPoint._of("", "");
    assertSet.accept(jp1);
    assertEquals("", jp1.componentName().state);
    assertEquals("", jp1.methodName().state);
    
    // Unset strings should make JoinPoint unset
    JoinPoint jp2 = new JoinPoint(new String(), new String());
    assertUnset.accept(jp2);
  }

  @Test
  void testRoundTripConsistency() {
    JoinPoint original = JoinPoint._of("TestComponent", "testMethod");
    
    // Create copy via copy operator
    JoinPoint copy = new JoinPoint();
    copy._copy(original);
    
    // Should be equal
    assertTrue(original._eq(copy).state);
    
    // String representations should be equal
    assertEquals(original._string().state, copy._string().state);
    
    // Hashcodes should be equal
    assertEquals(original._hashcode().state, copy._hashcode().state);
  }
}