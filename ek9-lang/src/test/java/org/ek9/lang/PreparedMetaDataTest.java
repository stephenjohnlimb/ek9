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
 * Unit tests for the PreparedMetaData class.
 * PreparedMetaData is set only when the contained JoinPoint is set.
 */
class PreparedMetaDataTest extends Common {

  // Constructor Tests

  @Test
  void testDefaultConstructor() {
    PreparedMetaData metaData = new PreparedMetaData();
    assertNotNull(metaData);

    // Default constructor should create unset PreparedMetaData
    assertUnset.accept(metaData);
    assertEquals("PreparedMetaData{}", metaData._string().state);
    
    // Accessor method should return unset JoinPoint
    assertUnset.accept(metaData.joinPoint());
  }

  @Test
  void testParameterizedConstructorValid() {
    JoinPoint validJoinPoint = JoinPoint._of("TestComponent", "testMethod");
    PreparedMetaData metaData = new PreparedMetaData(validJoinPoint);
    
    assertNotNull(metaData);
    assertSet.accept(metaData);
    
    // Should contain the JoinPoint values
    assertTrue(metaData._string().state.contains("TestComponent"));
    assertTrue(metaData._string().state.contains("testMethod"));
    
    // Accessor method should return equivalent JoinPoint
    JoinPoint retrievedJoinPoint = metaData.joinPoint();
    assertSet.accept(retrievedJoinPoint);
    assertEquals("TestComponent", retrievedJoinPoint.componentName().state);
    assertEquals("testMethod", retrievedJoinPoint.methodName().state);
  }

  @Test
  void testParameterizedConstructorWithNullJoinPoint() {
    PreparedMetaData metaData = new PreparedMetaData(null);
    
    assertNotNull(metaData);
    assertUnset.accept(metaData); // Should be unset due to null JoinPoint
    
    assertEquals("PreparedMetaData{}", metaData._string().state);
  }

  @Test
  void testParameterizedConstructorWithUnsetJoinPoint() {
    JoinPoint unsetJoinPoint = new JoinPoint(); // Unset
    PreparedMetaData metaData = new PreparedMetaData(unsetJoinPoint);
    
    assertNotNull(metaData);
    assertUnset.accept(metaData); // Should be unset due to unset JoinPoint
    
    assertEquals("PreparedMetaData{}", metaData._string().state);
  }

  // Factory Method Tests

  @Test
  void testFactoryMethodWithJoinPoint() {
    JoinPoint joinPoint = JoinPoint._of("Component", "method");
    PreparedMetaData metaData = PreparedMetaData._of(joinPoint);
    
    assertNotNull(metaData);
    assertSet.accept(metaData);
    assertEquals("Component", metaData.joinPoint().componentName().state);
    assertEquals("method", metaData.joinPoint().methodName().state);
  }

  @Test
  void testFactoryMethodWithJavaStrings() {
    PreparedMetaData metaData = PreparedMetaData._of("Component", "method");
    
    assertNotNull(metaData);
    assertSet.accept(metaData);
    assertEquals("Component", metaData.joinPoint().componentName().state);
    assertEquals("method", metaData.joinPoint().methodName().state);
  }

  // Accessor Method Tests

  @Test
  void testAccessorMethodSetState() {
    PreparedMetaData metaData = PreparedMetaData._of("TestClass", "testMethod");
    assertSet.accept(metaData);
    
    JoinPoint joinPoint = metaData.joinPoint();
    
    assertSet.accept(joinPoint);
    assertEquals("TestClass", joinPoint.componentName().state);
    assertEquals("testMethod", joinPoint.methodName().state);
  }

  @Test
  void testAccessorMethodUnsetState() {
    PreparedMetaData metaData = new PreparedMetaData();
    assertNotNull(metaData);
    assertUnset.accept(metaData);
    
    JoinPoint joinPoint = metaData.joinPoint();
    
    assertUnset.accept(joinPoint);
  }

  // Equality Operator Tests

  @Test
  void testEqualityWithSameValues() {
    PreparedMetaData md1 = PreparedMetaData._of("Component", "method");
    PreparedMetaData md2 = PreparedMetaData._of("Component", "method");
    
    Boolean result = md1._eq(md2);
    assertSet.accept(result);
    assertTrue(result.state);
  }

  @Test
  void testEqualityWithDifferentValues() {
    PreparedMetaData md1 = PreparedMetaData._of("Component1", "method");
    PreparedMetaData md2 = PreparedMetaData._of("Component2", "method");
    
    Boolean result = md1._eq(md2);
    assertSet.accept(result);
    assertFalse(result.state);
  }

  @Test
  void testEqualityWithUnsetPreparedMetaData() {
    PreparedMetaData setMd = PreparedMetaData._of("Component", "method");
    assertNotNull(setMd);
    PreparedMetaData unsetMd = new PreparedMetaData();
    
    Boolean result = setMd._eq(unsetMd);
    assertUnset.accept(result); // Should return unset
  }

  @Test
  void testEqualityWithNullPreparedMetaData() {
    PreparedMetaData md = PreparedMetaData._of("Component", "method");
    assertNotNull(md);
    
    Boolean result = md._eq(null);
    assertUnset.accept(result); // Should return unset
  }

  @Test
  void testEqualityWithAnyType() {
    PreparedMetaData md = PreparedMetaData._of("Component", "method");
    
    // Test with non-PreparedMetaData Any
    Boolean result = md._eq(String._of("test"));
    assertUnset.accept(result); // Should return unset for non-PreparedMetaData
    
    // Test with PreparedMetaData as Any
    PreparedMetaData md2 = PreparedMetaData._of("Component", "method");
    Boolean result2 = md._eq((Any) md2);
    assertSet.accept(result2);
    assertTrue(result2.state);
  }

  // Inequality Operator Tests

  @Test
  void testInequalityOperator() {
    PreparedMetaData md1 = PreparedMetaData._of("Component1", "method");
    PreparedMetaData md2 = PreparedMetaData._of("Component2", "method");
    
    Boolean result = md1._neq(md2);
    assertSet.accept(result);
    assertTrue(result.state);
  }

  @Test
  void testInequalityWithSameValues() {
    PreparedMetaData md1 = PreparedMetaData._of("Component", "method");
    PreparedMetaData md2 = PreparedMetaData._of("Component", "method");
    
    Boolean result = md1._neq(md2);
    assertSet.accept(result);
    assertFalse(result.state);
  }

  // Comparison Operator Tests

  @Test
  void testComparisonDelegation() {
    PreparedMetaData md1 = PreparedMetaData._of("Alpha", "method");
    PreparedMetaData md2 = PreparedMetaData._of("Beta", "method");
    
    Integer result = md1._cmp(md2);
    assertSet.accept(result);
    assertTrue(result.state < 0); // Alpha < Beta
    
    Integer result2 = md2._cmp(md1);
    assertSet.accept(result2);
    assertTrue(result2.state > 0); // Beta > Alpha
  }

  @Test
  void testComparisonEqual() {
    PreparedMetaData md1 = PreparedMetaData._of("Component", "method");
    PreparedMetaData md2 = PreparedMetaData._of("Component", "method");
    
    Integer result = md1._cmp(md2);
    assertSet.accept(result);
    assertEquals(0, result.state);
  }

  @Test
  void testComparisonWithUnsetPreparedMetaData() {
    PreparedMetaData setMd = PreparedMetaData._of("Component", "method");
    assertNotNull(setMd);
    PreparedMetaData unsetMd = new PreparedMetaData();
    
    Integer result = setMd._cmp(unsetMd);
    assertUnset.accept(result); // Should return unset
  }

  @Test
  void testComparisonWithAnyType() {
    PreparedMetaData md = PreparedMetaData._of("Component", "method");
    
    // Test with non-PreparedMetaData Any
    Integer result = md._cmp(String._of("test"));
    assertUnset.accept(result); // Should return unset
    
    // Test with PreparedMetaData as Any
    PreparedMetaData md2 = PreparedMetaData._of("Component", "method");
    Integer result2 = md._cmp((Any) md2);
    assertSet.accept(result2);
    assertEquals(0, result2.state);
  }

  // Assignment Operator Tests

  @Test
  void testCopyOperator() {
    PreparedMetaData source = PreparedMetaData._of("Source", "sourceMethod");
    PreparedMetaData target = new PreparedMetaData();
    assertUnset.accept(target);
    
    target._copy(source);
    assertSet.accept(target);
    assertEquals("Source", target.joinPoint().componentName().state);
    assertEquals("sourceMethod", target.joinPoint().methodName().state);
    
    // Should be equal but different objects
    assertTrue(source._eq(target).state);
    assertNotSame(source, target);
  }

  @Test
  void testCopyWithUnsetSource() {
    PreparedMetaData source = new PreparedMetaData();
    assertNotNull(source);
    PreparedMetaData target = PreparedMetaData._of("Target", "targetMethod");
    assertSet.accept(target);
    
    target._copy(source);
    assertUnset.accept(target); // Should become unset
  }

  @Test
  void testCopyWithNullSource() {
    PreparedMetaData target = PreparedMetaData._of("Target", "targetMethod");
    assertNotNull(target);
    assertSet.accept(target);
    
    target._copy(null);
    assertUnset.accept(target); // Should become unset
  }

  @Test
  void testReplaceOperator() {
    PreparedMetaData source = PreparedMetaData._of("Source", "sourceMethod");
    PreparedMetaData target = PreparedMetaData._of("Target", "targetMethod");
    
    target._replace(source);
    assertSet.accept(target);
    assertEquals("Source", target.joinPoint().componentName().state);
    assertEquals("sourceMethod", target.joinPoint().methodName().state);
  }

  @Test
  void testMergeOperatorWithUnsetTarget() {
    PreparedMetaData source = PreparedMetaData._of("Source", "sourceMethod");
    PreparedMetaData target = new PreparedMetaData(); // Unset
    
    target._merge(source);
    assertSet.accept(target);
    assertEquals("Source", target.joinPoint().componentName().state);
    assertEquals("sourceMethod", target.joinPoint().methodName().state);
  }

  @Test
  void testMergeOperatorWithSetTarget() {
    PreparedMetaData source = PreparedMetaData._of("Source", "sourceMethod");
    PreparedMetaData target = PreparedMetaData._of("Target", "targetMethod"); // Already set
    
    target._merge(source);
    assertSet.accept(target);
    // Should keep original values since target was already set
    assertEquals("Target", target.joinPoint().componentName().state);
    assertEquals("targetMethod", target.joinPoint().methodName().state);
  }

  // String and Hashcode Tests

  @Test
  void testStringRepresentation() {
    PreparedMetaData setMd = PreparedMetaData._of("TestClass", "testMethod");
    String stringRep = setMd._string();
    assertSet.accept(stringRep);
    
    java.lang.String expected = "PreparedMetaData{joinPoint: JoinPoint{componentName: 'TestClass', methodName: 'testMethod'}}";
    assertEquals(expected, stringRep.state);
  }

  @Test
  void testStringRepresentationUnset() {
    PreparedMetaData unsetMd = new PreparedMetaData();
    String stringRep = unsetMd._string();
    assertSet.accept(stringRep);
    assertEquals("PreparedMetaData{}", stringRep.state);
  }

  @Test
  void testPromoteOperator() {
    PreparedMetaData md = PreparedMetaData._of("Component", "method");
    String promoted = md._promote();
    assertSet.accept(promoted);
    assertEquals(md._string().state, promoted.state);
  }

  @Test
  void testHashcodeOperator() {
    PreparedMetaData md1 = PreparedMetaData._of("Component", "method");
    PreparedMetaData md2 = PreparedMetaData._of("Component", "method");
    PreparedMetaData md3 = PreparedMetaData._of("Different", "method");
    
    Integer hash1 = md1._hashcode();
    Integer hash2 = md2._hashcode();
    Integer hash3 = md3._hashcode();
    
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
    PreparedMetaData unsetMd = new PreparedMetaData();
    assertNotNull(unsetMd);
    Integer hash = unsetMd._hashcode();
    assertUnset.accept(hash);
  }

  // Edge Cases and Null Safety

  @Test
  void testNullSafety() {
    // All methods should handle null gracefully
    assertDoesNotThrow(() -> new PreparedMetaData(null));
    assertDoesNotThrow(() -> PreparedMetaData._of( null));
    assertDoesNotThrow(() -> PreparedMetaData._of(null, null));
    
    PreparedMetaData md = PreparedMetaData._of("Component", "method");
    assertDoesNotThrow(() -> md._eq(null));
    assertDoesNotThrow(() -> md._neq(null));
    assertDoesNotThrow(() -> md._cmp(null));
    assertDoesNotThrow(() -> md._copy(null));
    assertDoesNotThrow(() -> md._replace(null));
    assertDoesNotThrow(() -> md._merge(null));
  }

  @Test
  void testJoinPointDelegation() {
    // Verify that PreparedMetaData properly delegates to JoinPoint
    JoinPoint jp1 = JoinPoint._of("Component", "method");
    JoinPoint jp2 = JoinPoint._of("Different", "method");
    
    PreparedMetaData md1 = PreparedMetaData._of(jp1.componentName().state, jp1.methodName().state);
    PreparedMetaData md2 = PreparedMetaData._of(jp2.componentName().state, jp2.methodName().state);
    
    // Equality should match JoinPoint equality
    Boolean jpEq = jp1._eq(jp2);
    Boolean mdEq = md1._eq(md2);
    assertEquals(jpEq.state, mdEq.state);
    
    // Comparison should match JoinPoint comparison
    Integer jpCmp = jp1._cmp(jp2);
    Integer mdCmp = md1._cmp(md2);
    assertEquals(jpCmp.state, mdCmp.state);
  }

  @Test
  void testRoundTripConsistency() {
    PreparedMetaData original = PreparedMetaData._of("TestComponent", "testMethod");
    
    // Create copy via copy operator
    PreparedMetaData copy = new PreparedMetaData();
    copy._copy(original);
    
    // Should be equal
    assertTrue(original._eq(copy).state);
    
    // String representations should be equal
    assertEquals(original._string().state, copy._string().state);
    
    // Hashcodes should be equal
    assertEquals(original._hashcode().state, copy._hashcode().state);
    
    // JoinPoint contents should be equal
    assertTrue(original.joinPoint()._eq(copy.joinPoint()).state);
  }
}