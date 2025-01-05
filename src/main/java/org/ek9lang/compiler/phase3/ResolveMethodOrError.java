package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.PossibleMatchingMethods;
import org.ek9lang.compiler.support.MostSpecificScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Given a search for a method from an aggregate - and include supers/traits etc,
 * this function will try and locate the method. But if not found or ambiguous it will issue errors.
 */
final class ResolveMethodOrError extends TypedSymbolAccess
    implements BiFunction<IToken, MethodSearchInScope, MethodSymbol> {
  private final PossibleMatchingMethods possibleMatchingMethods = new PossibleMatchingMethods();
  private final MostSpecificScope mostSpecificScope;
  private final AccessToSymbolOrError accessToSymbolOrError;

  /**
   * Create function with provided errorListener etc.
   */
  ResolveMethodOrError(final SymbolsAndScopes symbolsAndScopes,
                       final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.mostSpecificScope = new MostSpecificScope(symbolsAndScopes);
    this.accessToSymbolOrError = new AccessToSymbolOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public MethodSymbol apply(final IToken errorLocation, final MethodSearchInScope searchOnAggregate) {

    //Firstly if this a generic type that has been parameterised, then we must now re-resolve it
    //in this phase so that its methods and types all get expanded. We cannot wait for it to be
    //traversed in the ek9 code in definition, because it may be referenced before that.
    if (searchOnAggregate.scopeToSearch() instanceof PossibleGenericSymbol possibleGenericSymbol
        && possibleGenericSymbol.isParameterisedType()) {
      symbolsAndScopes.resolveOrDefine(possibleGenericSymbol, errorListener);
    }

    final var accessFromScope = mostSpecificScope.get();
    final var results = searchOnAggregate.scopeToSearch()
        .resolveMatchingMethods(searchOnAggregate.search(), new MethodSymbolSearchResult());

    if (results.isSingleBestMatchPresent() && results.getSingleBestMatchSymbol().isPresent()) {

      final var resolved = results.getSingleBestMatchSymbol().get();
      accessToSymbolOrError.accept(
          new SymbolAccessData(errorLocation, accessFromScope, searchOnAggregate.scopeToSearch(),
              searchOnAggregate.search().getName(), resolved));

      return resolved;

    }

    final var msgStart = "In relation to type '" + searchOnAggregate.scopeToSearch().getFriendlyScopeName() + "', and ";

    if (results.isAmbiguous()) {
      emitAmbiguousMatch(errorLocation, searchOnAggregate, msgStart, results);
    } else if (results.isEmpty()) {
      //For debug:
      searchOnAggregate.scopeToSearch()
          .resolveMatchingMethods(searchOnAggregate.search(), new MethodSymbolSearchResult());

      emitMethodNotResolved(errorLocation, searchOnAggregate, msgStart);
    }

    return null;
  }


  private void emitAmbiguousMatch(final IToken errorLocation,
                                  final MethodSearchInScope searchOnAggregate,
                                  final String msgStart,
                                  final MethodSymbolSearchResult results) {

    final var msg = msgStart + "'"
        + searchOnAggregate.search().toString()
        + "' resolved: "
        + results.getAmbiguousMethodParameters();

    errorListener.semanticError(errorLocation, msg, ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);

  }


  private void emitMethodNotResolved(final IToken errorLocation,
                                     final MethodSearchInScope searchOnAggregate,
                                     final String msgStart) {

    var msg = msgStart + "'" + searchOnAggregate.search().toString();
    final var nearMatches = possibleMatchingMethods.apply(searchOnAggregate);
    if (nearMatches.isEmpty()) {
      msg += "':";
    } else {
      msg += "', parameter mismatch. Possible method(s) ";
      msg += methodsToPresentation(nearMatches);
      msg += ":";
    }
    errorListener.semanticError(errorLocation, msg, ErrorListener.SemanticClassification.METHOD_NOT_RESOLVED);

  }

  private String methodsToPresentation(List<MethodSymbol> methods) {

    return methods.stream().map(ISymbol::getFriendlyName).collect(Collectors.joining(","));

  }
}
