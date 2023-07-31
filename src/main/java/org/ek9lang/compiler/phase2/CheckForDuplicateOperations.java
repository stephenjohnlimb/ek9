package org.ek9lang.compiler.phase2;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.Symbol;
import org.ek9lang.compiler.symbols.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.search.MethodSymbolSearchResult;

/**
 * Does a simple check (excluding any inheritance) for duplicated operations (methods, operators) on
 * any sort of Aggregate, i.e. classes, components, traits and just operators on records.
 * This is the first of such checks, in later phases inheritance of methods with invalid return types
 * and also unimplemented abstract methods will be checked (by other checkers).
 */
final class CheckForDuplicateOperations extends RuleSupport implements BiConsumer<Token, IAggregateSymbol> {
  /**
   * Create a new operations checker an aggregates.
   */
  CheckForDuplicateOperations(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final Token errorLocationToken, final IAggregateSymbol aggregate) {
    if (aggregate.isGenericInNature()) {
      checkForDuplicatedNoneOverloadableMethods(errorLocationToken, aggregate, aggregate.getAllNonAbstractMethods());
      checkForDuplicatedNoneOverloadableMethods(errorLocationToken, aggregate, aggregate.getAllAbstractMethods());
    } else {
      checkForDuplicatedOverloadableMethods(errorLocationToken, aggregate, aggregate.getAllNonAbstractMethods());
      checkForDuplicatedOverloadableMethods(errorLocationToken, aggregate, aggregate.getAllAbstractMethods());
    }
  }

  private void checkForDuplicatedOverloadableMethods(final Token errorLocationToken, final IAggregateSymbol aggregate,
                                                     final List<MethodSymbol> methods) {
    methods.forEach(method -> {
      MethodSymbolSearchResult results = new MethodSymbolSearchResult();
      var matching = aggregate.resolveMatchingMethods(new MethodSymbolSearch(method), results);
      if (!matching.isSingleBestMatchPresent()) {
        emitErrors(errorLocationToken, aggregate, method, ErrorListener.SemanticClassification.METHOD_DUPLICATED);
      }
    });
  }

  /**
   * For generic/template types, we check that any methods that are not constructors or methods
   * that doe have arguments(parameters) are not overloaded.
   * This is because if there were multiple methods with the same name, but with different generic parameters,
   * it is possible when parameterised that the methods would be duplicated.
   * For example doIt(U, V) and doIt(V, U) are ok in a generic contexts (definition) but when
   * U -> Integer and V -> Integer - now we have two methods with the same signatures.
   * So the solution is to stop method overloading where any types of parameters used.
   */
  private void checkForDuplicatedNoneOverloadableMethods(final Token errorLocationToken,
                                                         final IAggregateSymbol aggregate,
                                                         final List<MethodSymbol> methods) {
    var methodNames = methods.stream()
        .filter(method -> !method.isConstructor())
        .filter(method -> !method.getCallParameters().isEmpty())
        .map(Symbol::getName).toList();

    methods.forEach(method -> {
      int count = Collections.frequency(methodNames, method.getName());
      if (count > 1) {
        emitErrors(errorLocationToken, aggregate, method,
            ErrorListener.SemanticClassification.OVERLOADING_NOT_SUPPORTED);
      }

    });
  }

  private void emitErrors(final Token errorLocationToken,
                          final IAggregateSymbol aggregate,
                          final MethodSymbol method,
                          final ErrorListener.SemanticClassification classification) {
    var operation = method.isOperator() ? "operator" : "method";

    var msg = "Originating from line: "
        + errorLocationToken.getLine() + " and relating to '"
        + aggregate.getFriendlyName()
        + "', " + operation + ": '"
        + method.getFriendlyName()
        + "':";

    errorListener.semanticError(method.getSourceToken(), msg, classification);
  }
}
