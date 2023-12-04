package org.ek9lang.compiler.common;

import java.util.List;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Used for variable forms of simple code flow analysis.
 * Mainly focuses on initialisation, safe access or checking is specific methods
 * have been called before others.
 */
public interface CodeFlowAnalyzer {
  List<ISymbol> getSymbolsNotMeetingAcceptableCriteria(IScope inScope);

  boolean doesSymbolMeetAcceptableCriteria(final ISymbol identifierSymbol, final IScope inScope);

  void recordSymbol(final ISymbol identifierSymbol, final IScope inScope);

  void markSymbolAsMeetingAcceptableCriteria(final ISymbol identifierSymbol, final IScope inScope);
}
