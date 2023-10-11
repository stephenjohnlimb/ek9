package org.ek9lang.compiler.support;

import java.util.List;
import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.CompilerException;

/**
 * Does a simple check (excluding any inheritance) for duplicated operations (methods, operators) on
 * any sort of Aggregate, i.e. classes, components, traits and just operators on records.
 * This is the first of such checks, in later phases inheritance of methods with invalid return types
 * and also unimplemented abstract methods will be checked (by other checkers).
 */
public final class CheckForDuplicateOperations implements BiConsumer<IToken, IAggregateSymbol> {

  private final ErrorListener errorListener;

  /**
   * Create a new operations checker an aggregates.
   */
  public CheckForDuplicateOperations(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final IToken errorLocationToken, final IAggregateSymbol aggregate) {
    checkForDuplicatedOverloadableMethods(errorLocationToken, aggregate, aggregate.getAllNonAbstractMethods());
    checkForDuplicatedOverloadableMethods(errorLocationToken, aggregate, aggregate.getAllAbstractMethods());
  }

  private void checkForDuplicatedOverloadableMethods(final IToken errorLocationToken, final IAggregateSymbol aggregate,
                                                     final List<MethodSymbol> methods) {
    methods.forEach(method -> {
      //Parameterised types have an empty argument constructor
      //and another constructor with just the parameterizing types.
      if ((aggregate.isParameterisedType() && !method.isConstructor()) || !aggregate.isParameterisedType()) {
        MethodSymbolSearchResult results = new MethodSymbolSearchResult();
        var search = new MethodSymbolSearch(method);
        var matching = aggregate.resolveMatchingMethods(search, results);
        if (matching.isEmpty()) {
          //paranoid check - this indicates some earlier stage is in error.
          throw new CompilerException("Expecting to be able to find a method ["
              + search
              + "] that is known to exist on ["
              + aggregate.getFriendlyName()
              + "] in "
              + aggregate.getSourceToken().getSourceName());
        } else if (!matching.isSingleBestMatchPresent()) {
          emitErrors(errorLocationToken, aggregate, method);
        }
      }
    });
  }

  private void emitErrors(final IToken errorLocationToken,
                          final IAggregateSymbol aggregate,
                          final MethodSymbol method) {
    var operation = method.isOperator() ? "operator" : "method";

    var msg = "Originating from line: "
        + errorLocationToken.getLine() + " and relating to '"
        + aggregate.getFriendlyName()
        + "', "
        + operation + ": '"
        + method.getFriendlyName()
        + "':";

    errorListener.semanticError(method.getSourceToken(), msg, ErrorListener.SemanticClassification.METHOD_DUPLICATED);

    errorListener.semanticError(errorLocationToken, "Source of duplication:",
        ErrorListener.SemanticClassification.METHOD_DUPLICATED);
  }
}
