package org.ek9lang.compiler.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Predicate;
import org.ek9lang.compiler.CompilableProgram;

public class SymbolCountCheck implements Predicate<CompilableProgram> {

  private final String forModuleName;
  private final int expectedSymbolCount;

  private final int expectModuleCount;

  private final ShowSymbolInModule symbolDisplay = new ShowSymbolInModule();

  public SymbolCountCheck(final String forModuleName, final int expectedSymbolCount) {
    this(1, forModuleName, expectedSymbolCount);
  }

  public SymbolCountCheck(final int expectModuleCount, final String forModuleName, final int expectedSymbolCount) {
    this.expectModuleCount = expectModuleCount;
    this.forModuleName = forModuleName;
    this.expectedSymbolCount = expectedSymbolCount;
  }

  public String getForModuleName() {
    return forModuleName;
  }

  @Override
  public boolean test(CompilableProgram compilableProgram) {
    var modules = compilableProgram.getParsedModules(forModuleName);
    assertNotNull(modules);
    assertEquals(expectModuleCount, modules.size(), "Incorrect number of modules for package.");

    var numSymbols = modules.stream().map(module -> module.getModuleScope().getSymbolsForThisScope())
        .mapToLong(List::size).sum();

    if (expectedSymbolCount != numSymbols) {
      symbolDisplay.accept(modules);
    }

    assertEquals(expectedSymbolCount, numSymbols);

    return true;
  }
}
