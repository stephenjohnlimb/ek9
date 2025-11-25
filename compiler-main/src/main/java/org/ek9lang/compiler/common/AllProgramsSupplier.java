package org.ek9lang.compiler.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ModuleScope;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.SharedThreadContext;

/**
 * Reviews all the modules and looks for all the 'Programs' that have been defined anywhere.
 */
public final class AllProgramsSupplier
    implements Function<SharedThreadContext<CompilableProgram>, List<AggregateSymbol>> {

  @SuppressWarnings("checkstyle:WhitespaceAfter")
  @Override
  public List<AggregateSymbol> apply(
      final SharedThreadContext<CompilableProgram> compilableProgramSharedThreadContext) {
    final var rtn = new ArrayList<AggregateSymbol>();

    compilableProgramSharedThreadContext.accept(compilableProgram -> compilableProgram
        .getParsedModuleNames()
        .stream()
        .map(compilableProgram::getParsedModules)
        .flatMap(Collection::stream)
        .map(ParsedModule::getModuleScope)
        .map(ModuleScope::getSymbolsForThisScope)
        .flatMap(Collection::stream)
        .filter(symbol -> symbol.getGenus() == SymbolGenus.PROGRAM)
        .map(AggregateSymbol.class::cast)
        .forEach(rtn::add));
    return rtn;
  }
}
