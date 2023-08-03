package org.ek9lang.compiler.symbols;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.common.ScopeStack;
import org.junit.jupiter.api.Test;

/**
 * Just simple tests of the scope stack that will be used in the
 * compiler phases when building the internal model and symbols.
 */
final class ScopeStackTest {
  @Test
  void testNewScopeStack() {
    ScopeStack underTest = new ScopeStack(new SymbolTable());
    assertFalse(underTest.isEmpty());

    var theTop = underTest.getVeryBaseScope();
    assertNotNull(theTop);
    var top = underTest.pop();

    assertSame(top, theTop);
    assertNotNull(top);
    assertTrue(underTest.isEmpty());
  }

  @Test
  void testPushOnScopeStack() {
    ScopeStack underTest = new ScopeStack(new SymbolTable());
    assertFalse(underTest.isEmpty());

    underTest.push(new LocalScope("A test", new SymbolTable()));

    var scope = underTest.pop();
    assertNotNull(scope);
    assertFalse(underTest.isEmpty());

    var top = underTest.pop();
    assertNotNull(top);

    assertTrue(underTest.isEmpty());
  }

  /**
   * Checks the traversal back up the scope stack to find blocks of a particular block type.
   */
  @Test
  void testStackTraversal() {
    var rootSymbolTable = new SymbolTable();
    ScopeStack underTest = new ScopeStack(rootSymbolTable);

    //This should find the SymbolTable as a BLOCK.
    var foundBlock = underTest.traverseBackUpStack(IScope.ScopeType.BLOCK);
    assertTrue(foundBlock.isPresent());

    var foundNonBlock = underTest.traverseBackUpStack(IScope.ScopeType.NON_BLOCK);
    assertTrue(foundNonBlock.isEmpty());

    var foundDynamicBlock = underTest.traverseBackUpStack(IScope.ScopeType.DYNAMIC_BLOCK);
    assertTrue(foundDynamicBlock.isEmpty());

    var dynamicBlock = new LocalScope("ADynamicBlock", rootSymbolTable);
    dynamicBlock.setScopeType(IScope.ScopeType.DYNAMIC_BLOCK);

    underTest.push(dynamicBlock);

    //Now put another block on and see if we can navigate past it.
    underTest.push(new SymbolTable());

    var foundNonBlock2 = underTest.traverseBackUpStack(IScope.ScopeType.NON_BLOCK);
    assertTrue(foundNonBlock2.isEmpty());

    var foundDynamicBlock2 = underTest.traverseBackUpStack(IScope.ScopeType.DYNAMIC_BLOCK);
    assertTrue(foundDynamicBlock2.isPresent());

    var nonBlock = new LocalScope("ANonBlock", rootSymbolTable);
    nonBlock.setScopeType(IScope.ScopeType.NON_BLOCK);
    underTest.push(nonBlock);

    //Now put another block on and see if we can navigate past it.
    underTest.push(new SymbolTable());
    //Now put another block on and see if we can navigate past it.
    underTest.push(new SymbolTable());

    var foundNonBlock3 = underTest.traverseBackUpStack(IScope.ScopeType.NON_BLOCK);
    assertTrue(foundNonBlock3.isPresent());
  }
}
