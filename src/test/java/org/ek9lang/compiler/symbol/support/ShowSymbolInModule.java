package org.ek9lang.compiler.symbol.support;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Just a utility to output the symbols held inside a module (can have multiple scopes in that module).
 */
public class ShowSymbolInModule implements Consumer<List<ParsedModule>> {
  @Override
  public void accept(final List<ParsedModule> modules) {
    modules.forEach(module -> System.out.println(
        "For scope name [" + module.getModuleName() + "] " + module.getSource().getFileName()));

    modules.stream().map(module -> module.getModuleScope().getSymbolsForThisScope())
        .flatMap(List::stream).map(toPresentation()).forEach(System.out::println);
  }

  private Function<ISymbol, String> toPresentation() {
    return symbol -> "Internal Name: [" + symbol.getName() + "] Presentable Name: [" + symbol + "] ["
        + symbol.getFullyQualifiedName() + "] as " + symbol.getCategory();
  }
}
