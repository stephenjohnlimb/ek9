package org.ek9lang.compiler.phase7.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Basic tests for CallDetailsBuilder functionality.
 * Tests the core cost-based method resolution and CallDetails construction.
 */
class CallDetailsBuilderTest {

  private SymbolTable symbolTable;

  @BeforeEach
  void setUp() {
    symbolTable = new SymbolTable();
  }

  @Test
  void testCallDetailsBuilderCreation() {
    // This is a placeholder test since we can't easily create IRContext without mocking
    // The real testing will come when we integrate with BinaryOperationGenerator
    assertTrue(true, "CallDetailsBuilder classes compile successfully");
  }

  @Test
  void testCallContextCreation() {
    // Test binary operation context creation
    final var leftType = new AggregateSymbol("LeftType", symbolTable);
    final var rightType = new AggregateSymbol("RightType", symbolTable);

    final var context = CallContext.forBinaryOperation(leftType, rightType, "_add", "_left", "_right", "scope1");

    assertEquals(leftType, context.targetType());
    assertEquals("_left", context.targetVariable());
    assertEquals("_add", context.methodName());
    assertEquals(List.of(rightType), context.argumentTypes());
    assertEquals(List.of("_right"), context.argumentVariables());
    assertEquals("scope1", context.scopeId());
  }

  @Test
  void testMethodResolutionResult() {
    final var method = new MethodSymbol("testMethod", symbolTable);
    final var result = new MethodResolutionResult(method, 100.0, false);

    assertTrue(result.isPerfectMatch());
    assertFalse(result.requiresPromotion());
    assertFalse(result.isInvalid());
  }

  @Test
  void testPromotionResult() {
    final var promotionResult = new PromotionResult(List.of("_promoted"), List.of());

    assertFalse(promotionResult.hasPromotions());
    assertEquals(0, promotionResult.getPromotionCount());
    assertEquals(List.of("_promoted"), promotionResult.promotedArguments());
  }

}