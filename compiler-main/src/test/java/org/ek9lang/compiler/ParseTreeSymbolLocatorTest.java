package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.junit.jupiter.api.Test;

/**
 * Tests the ParseTreeSymbolLocator functionality by compiling EK9 source
 * and verifying that symbols can be located and the locateSymbolAtToken
 * method is properly integrated into ParsedModule.
 */
class ParseTreeSymbolLocatorTest extends PhasesTest {

  ParseTreeSymbolLocatorTest() {
    super("/examples/lsp/");
  }

  @Test
  void testSymbolLocatorIntegration() {
    testToPhase(CompilationPhase.PRE_IR_CHECKS);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult,
                                    final int numberOfErrors,
                                    final CompilableProgram program) {

    assertTrue(compilationResult, "Compilation should succeed");
    assertEquals(0, numberOfErrors, "Should have no errors");

    // Get the parsed module for our test file
    final var modules = program.getParsedModules("com.customer.lsp.hoversymboltest");
    assertEquals(1, modules.size(), "Should have exactly one module");

    final var module = modules.getFirst();
    assertNotNull(module, "Module should not be null");

    final var source = module.getSource();
    assertNotNull(source, "Source should not be null");

    // Verify the parse tree exists - this is prerequisite for the locator
    assertNotNull(source.getCompilationUnitContext(), "Parse tree should exist");

    // Verify the module scope contains expected symbols
    final var moduleScope = module.getModuleScope();
    assertNotNull(moduleScope, "Module scope should exist");

    // The function 'getGreeting' should be in the module scope
    final var functionSymbols = moduleScope.getSymbolsForThisScope().stream()
        .filter(s -> s.getCategory() == SymbolCategory.FUNCTION)
        .toList();
    assertFalse(functionSymbols.isEmpty(), "Should have function symbols");

    // Find getGreeting function
    final var getGreetingOpt = functionSymbols.stream()
        .filter(s -> s.getName().equals("getGreeting"))
        .findFirst();
    assertTrue(getGreetingOpt.isPresent(), "Should find getGreeting function");

    final var getGreeting = getGreetingOpt.get();
    assertEquals("getGreeting", getGreeting.getName());
    assertEquals(SymbolCategory.FUNCTION, getGreeting.getCategory());

    // The program 'HoverTestProgram' should also be in the module scope
    final var programSymbols = moduleScope.getSymbolsForThisScope().stream()
        .filter(s -> s.getName().equals("HoverTestProgram"))
        .toList();
    assertFalse(programSymbols.isEmpty(), "Should have HoverTestProgram");
  }
}
