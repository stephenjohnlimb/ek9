package org.ek9lang.compiler.symbol.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Predicate;
import org.ek9lang.compiler.internals.CompilableProgram;

public class SymbolCountCheck implements Predicate<CompilableProgram> {

  private final String forModuleName;
  private final int expectedSymbolCount;

  private final int expectModuleCount;

  public SymbolCountCheck(final String forModuleName, final int expectedSymbolCount) {
    this(1, forModuleName, expectedSymbolCount);
  }

  public SymbolCountCheck(final int expectModuleCount, final String forModuleName, final int expectedSymbolCount) {
    this.expectModuleCount = expectModuleCount;
    this.forModuleName = forModuleName;
    this.expectedSymbolCount = expectedSymbolCount;
  }

  @Override
  public boolean test(CompilableProgram compilableProgram) {
    var modules = compilableProgram.getParsedModules(forModuleName);
    assertNotNull(modules);
    //only ever expect 1 during these tests
    assertEquals(expectModuleCount, modules.size(), "Incorrect number of modules for package.");
    var symbols = modules.stream().map(module -> module.getModuleScope().getSymbolsForThisScope())
        .flatMap(List::stream).toList();

    if (expectedSymbolCount != symbols.size()) {
      modules.forEach(module -> System.out.println(
          "For scope name [" + module.getModuleName() + "] " + module.getSource().getFileName()));
      for (var symbol : symbols) {
        System.out.println("Internal Name: [" + symbol.getName() + "] Presentable Name: [" + symbol + "] ["
            + symbol.getFullyQualifiedName() + "] as " + symbol.getCategory());
      }
    }
    assertEquals(expectedSymbolCount, symbols.size());

    return true;
  }
}
