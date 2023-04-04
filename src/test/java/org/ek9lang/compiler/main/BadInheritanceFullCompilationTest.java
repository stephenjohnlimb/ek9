package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.support.SimpleResolverForTesting;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad inheritance usage.
 */
class BadInheritanceFullCompilationTest extends FullCompilationTest {

  public BadInheritanceFullCompilationTest() {
    super("/examples/parseButFailCompile/badInheritance");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    var modules = program.getParsedModules("bad.inherited.records");
    assertFalse(modules.isEmpty());
    SimpleResolverForTesting resolver = new SimpleResolverForTesting(modules.get(0).getModuleScope(), true);

    assertRecord3Hierarchy(resolver);
    assertNoTypeHierarchy("Record4", resolver);
    assertNoTypeHierarchy("Record5", resolver);
  }

  private void assertRecord3Hierarchy(final SimpleResolverForTesting resolver) {
    var record3 = resolver.apply("Record3");
    assertTrue(record3.isPresent());
    record3.ifPresent(symbol -> {
      if (symbol instanceof AggregateSymbol aggregate) {
        var record3Super = aggregate.getSuperAggregateScopedSymbol();
        assertTrue(record3Super.isPresent());
        assertEquals("Record2", record3Super.get().getName());
        var record2Super = record3Super.get().getSuperAggregateScopedSymbol();
        assertTrue(record2Super.isPresent());
        assertEquals("Record1", record2Super.get().getName());
        //Now check nothing else.
        var record1Super = record2Super.get().getSuperAggregateScopedSymbol();
        assertFalse(record1Super.isPresent());
      } else {
        fail("Expecting an aggregate");
      }
    });
  }

  private void assertNoTypeHierarchy(final String typeName, final SimpleResolverForTesting resolver) {
    var record4 = resolver.apply(typeName);
    assertTrue(record4.isPresent());
    assertFalse(((AggregateSymbol)record4.get()).getSuperAggregateScopedSymbol().isPresent());
  }
}
