package org.ek9lang.compiler.main.rules;

import java.util.List;
import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;

/**
 * Does a simple check (excluding any inheritance) for duplicated operations (methods, operators) on
 * any sort of Aggregate, i.e. classes, components, traits and just operators on records.
 * This is the first of such checks, in later phases inheritance of methods with invalid return types
 * and also unimplemented abstract methods will be checked (by other checkers).
 */
public class CheckForDuplicateOperations extends RuleSupport implements BiConsumer<Token, IAggregateSymbol> {
  /**
   * Create a new operations checker an aggregates.
   */
  public CheckForDuplicateOperations(
      SymbolAndScopeManagement symbolAndScopeManagement,
      ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final Token errorLocationToken, final IAggregateSymbol aggregate) {
    checkForDuplicatedMethods(errorLocationToken, aggregate, aggregate.getAllNonAbstractMethods());
    checkForDuplicatedMethods(errorLocationToken, aggregate, aggregate.getAllAbstractMethods());
  }

  private void checkForDuplicatedMethods(final Token errorLocationToken, final IAggregateSymbol aggregate,
                                         final List<MethodSymbol> methods) {
    methods.forEach(method -> {
      MethodSymbolSearchResult results = new MethodSymbolSearchResult();
      var matching = aggregate.resolveMatchingMethods(new MethodSymbolSearch(method), results);
      if (!matching.isSingleBestMatchPresent()) {
        emitErrors(errorLocationToken, aggregate, method);
      }
    });
  }

  private void emitErrors(final Token errorLocationToken,
                          final IAggregateSymbol aggregate,
                          final MethodSymbol method) {
    var operation = method.isOperator() ? "operator" : "method";

    var msg = "Originating from line: "
        + errorLocationToken.getLine() + " and relating to '"
        + aggregate.getFriendlyName()
        + "', " + operation + ": '"
        + method.getFriendlyName()
        + "':";

    errorListener.semanticError(method.getSourceToken(), msg, ErrorListener.SemanticClassification.METHOD_DUPLICATED);
  }
}
