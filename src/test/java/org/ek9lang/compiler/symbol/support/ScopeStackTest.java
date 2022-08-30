package org.ek9lang.compiler.symbol.support;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.symbol.LocalScope;
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

}
