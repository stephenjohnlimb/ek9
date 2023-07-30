package org.ek9lang.compiler.main;

import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_LANG;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad generic uses of generics.
 */
class BadGenericUsesFullCompilationTest extends FullCompilationTest {

  public BadGenericUsesFullCompilationTest() {
    super("/examples/parseButFailCompile/badGenericUses");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertEK9GeneratedGenericTypes(program);
    assertGenericUses(program);
  }

  private void assertEK9GeneratedGenericTypes(final CompilableProgram program) {
    var ek9 = program.getParsedModules(EK9_LANG);
    assertFalse(ek9.isEmpty());
  }

  private void assertGenericUses(final CompilableProgram program) {

    var moduleName = "incorrect.generic.uses";
    var uses = program.getParsedModules(moduleName);
    assertFalse(uses.isEmpty());
    //Detailed tests us @ directives in EK9 source code
  }
}
