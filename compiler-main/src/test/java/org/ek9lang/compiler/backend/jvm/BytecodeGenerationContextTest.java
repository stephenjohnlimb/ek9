package org.ek9lang.compiler.backend.jvm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Label;

/**
 * Unit tests for BytecodeGenerationContext.
 * Tests the context stack operations for control flow management.
 */
class BytecodeGenerationContextTest {

  @Test
  void testLoopContextBasic() {
    final var context = new BytecodeGenerationContext();
    assertFalse(context.isInsideLoop());
    assertTrue(context.isEmpty());

    final var continueLabel = new Label();
    final var exitLabel = new Label();

    context.enterLoop("loop1", continueLabel, exitLabel);

    assertTrue(context.isInsideLoop());
    assertFalse(context.isEmpty());
    assertEquals(continueLabel, context.getLoopContinueLabel().orElseThrow());
    assertEquals(exitLabel, context.getLoopExitLabel().orElseThrow());

    context.exitLoop();

    assertFalse(context.isInsideLoop());
    assertTrue(context.isEmpty());
  }

  @Test
  void testNestedLoops() {
    final var context = new BytecodeGenerationContext();

    final var outerContinue = new Label();
    final var outerExit = new Label();
    final var innerContinue = new Label();
    final var innerExit = new Label();

    context.enterLoop("outer", outerContinue, outerExit);
    context.enterLoop("inner", innerContinue, innerExit);

    // Should return inner loop's labels (nearest)
    assertEquals(innerContinue, context.getLoopContinueLabel().orElseThrow());
    assertEquals(innerExit, context.getLoopExitLabel().orElseThrow());

    context.exitLoop();  // Exit inner

    // Should now return outer loop's labels
    assertEquals(outerContinue, context.getLoopContinueLabel().orElseThrow());
    assertEquals(outerExit, context.getLoopExitLabel().orElseThrow());

    context.exitLoop();  // Exit outer

    assertFalse(context.isInsideLoop());
    assertTrue(context.isEmpty());
  }

  @Test
  void testExitLoopValidation() {
    final var context = new BytecodeGenerationContext();

    // Should throw when exiting empty stack
    assertThrows(IllegalStateException.class, context::exitLoop);
  }

  @Test
  void testGetAllScopeIds() {
    final var context = new BytecodeGenerationContext();

    context.enterLoop("outer", new Label(), new Label());
    context.enterLoop("inner", new Label(), new Label());

    final var scopeIds = context.getAllScopeIds();
    assertEquals(2, scopeIds.size());
    assertEquals("inner", scopeIds.get(0));  // Top of stack
    assertEquals("outer", scopeIds.get(1));
  }

  @Test
  void testGetCurrentScopeId() {
    final var context = new BytecodeGenerationContext();

    assertTrue(context.getCurrentScopeId().isEmpty());

    context.enterLoop("loop1", new Label(), new Label());
    assertEquals("loop1", context.getCurrentScopeId().orElseThrow());

    context.enterLoop("loop2", new Label(), new Label());
    assertEquals("loop2", context.getCurrentScopeId().orElseThrow());

    context.exitLoop();
    assertEquals("loop1", context.getCurrentScopeId().orElseThrow());

    context.exitLoop();
    assertTrue(context.getCurrentScopeId().isEmpty());
  }

  @Test
  void testQueryWhenNotInLoop() {
    final var context = new BytecodeGenerationContext();

    assertFalse(context.isInsideLoop());
    assertTrue(context.getLoopContinueLabel().isEmpty());
    assertTrue(context.getLoopExitLabel().isEmpty());
  }

  @Test
  void testIsInside() {
    final var context = new BytecodeGenerationContext();

    assertFalse(context.isInside(BytecodeFrameType.LOOP));

    context.enterLoop("loop1", new Label(), new Label());

    assertTrue(context.isInside(BytecodeFrameType.LOOP));
    assertFalse(context.isInside(BytecodeFrameType.SWITCH));  // Not in switch

    context.exitLoop();

    assertFalse(context.isInside(BytecodeFrameType.LOOP));
  }

}
