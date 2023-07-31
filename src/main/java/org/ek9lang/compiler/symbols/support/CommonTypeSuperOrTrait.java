package org.ek9lang.compiler.symbols.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Given a list of symbols (normally variables), this code will get the type from each of those symbols.
 * See SymbolTypeExtractor for this.
 * It will then determine if the first type is an aggregate or a function.
 * If it is a function - then it will check that all the other types in the list are also functions and
 * if they are the 'same' function, typically this will be false (for functions), it will then check the
 * super (if present) and check if the other functions also have the same super. This will continue going up
 * the supers until they are the same or there are no more supers.
 * For aggregates, the same process if followed for supers, except if there is no common super the code looks
 * for a common 'trait'.
 * If any of the variableSymbols have not been typed then this function will return an empty Optional.
 * If there are no common supers then this function will return an empty Optional.
 * This function will issue semantic errors.
 */
public class CommonTypeSuperOrTrait implements BiFunction<Token, List<ISymbol>, Optional<ISymbol>> {

  final ErrorListener errorListener;
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();

  public CommonTypeSuperOrTrait(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public Optional<ISymbol> apply(final Token lineToken, final List<ISymbol> argumentSymbols) {
    if (argumentSymbols.isEmpty()) {
      return Optional.empty();
    }

    var argumentTypes = symbolTypeExtractor.apply(argumentSymbols);
    if (argumentTypes.size() != argumentSymbols.size()) {
      //No error issued as other code should have detected this.
      return Optional.empty();
    }

    return getCommonType(lineToken, argumentSymbols, argumentTypes);
  }

  private Optional<ISymbol> getCommonType(final Token lineToken,
                                          final List<ISymbol> argumentSymbols,
                                          final List<ISymbol> argumentTypes) {
    //But note the ek9 developer could have accidentally mixed vars which were functions and aggregates
    //We have to accept this but detect and issue error.
    if (canCommonTypeBeDetermined(lineToken, argumentSymbols, argumentTypes)) {
      return determineCommonType(lineToken, argumentSymbols, argumentTypes);
    }
    return Optional.empty();
  }

  private boolean canCommonTypeBeDetermined(final Token lineToken,
                                            final List<ISymbol> argumentSymbols,
                                            final List<ISymbol> argumentTypes) {
    return (argumentTypes.get(0) instanceof FunctionSymbol
        && checkFunctionSymbols(lineToken, argumentSymbols, argumentTypes))
        || (argumentTypes.get(0) instanceof AggregateSymbol
        && checkAggregateSymbols(lineToken, argumentSymbols, argumentTypes));
  }

  private Optional<ISymbol> determineCommonType(final Token lineToken,
                                                final List<ISymbol> argumentSymbols,
                                                final List<ISymbol> argumentTypes) {
    List<ISymbol> typesToTry = new ArrayList<>();
    getTypesToTry(argumentTypes.get(0), typesToTry);

    for (var type : typesToTry) {
      if (allAssignableTo(type, argumentTypes)) {
        return Optional.of(type);
      }
    }
    emitNoCommonType(lineToken, argumentSymbols.get(0));
    return Optional.empty();
  }

  private void getTypesToTry(final ISymbol symbolType, List<ISymbol> addToTypes) {
    if (addToTypes.contains(symbolType)) {
      return;
    }
    addToTypes.add(symbolType);
    if (symbolType instanceof FunctionSymbol functionSymbol && functionSymbol.getSuperFunctionSymbol().isPresent()) {
      getTypesToTry(functionSymbol.getSuperFunctionSymbol().get(), addToTypes);
    } else if (symbolType instanceof AggregateSymbol aggregateSymbol) {
      if (aggregateSymbol.getSuperAggregateSymbol().isPresent()) {
        getTypesToTry(aggregateSymbol.getSuperAggregateSymbol().get(), addToTypes);
      }
      if (aggregateSymbol instanceof AggregateWithTraitsSymbol aggregateWithTraitsSymbol) {
        aggregateWithTraitsSymbol.getAllTraits().forEach(symbol -> getTypesToTry(symbol, addToTypes));
      }
    }
  }

  private boolean allAssignableTo(final ISymbol typeSymbol, final List<ISymbol> typeList) {
    return typeList.stream().filter(fun -> fun.isAssignableTo(typeSymbol)).count() == typeList.size();
  }

  private boolean checkAggregateSymbols(final Token lineToken,
                                        final List<ISymbol> argumentSymbols,
                                        final List<ISymbol> argumentTypes) {
    int count = 0;
    for (int i = 0; i < argumentTypes.size(); i++) {
      if (argumentTypes.get(i) instanceof AggregateSymbol) {
        count++;
      } else {
        emitExpectingAggregateError(lineToken, argumentSymbols.get(i));
      }
    }
    return count == argumentSymbols.size();
  }

  private boolean checkFunctionSymbols(final Token lineToken,
                                       final List<ISymbol> argumentSymbols,
                                       final List<ISymbol> argumentTypes) {
    int count = 0;
    for (int i = 0; i < argumentTypes.size(); i++) {
      if (argumentTypes.get(i) instanceof FunctionSymbol) {
        count++;
      } else {
        emitExpectingFunctionError(lineToken, argumentSymbols.get(i));
      }
    }
    return count == argumentSymbols.size();
  }

  private void emitExpectingFunctionError(final Token lineToken, final ISymbol argument) {
    var msg = "Expecting a function not '" + argument.getFriendlyName() + "':";
    errorListener.semanticError(lineToken, msg,
        ErrorListener.SemanticClassification.TYPE_MUST_BE_FUNCTION);
  }

  private void emitExpectingAggregateError(final Token lineToken, final ISymbol argument) {
    var msg = "Expecting a non-function not function '" + argument.getName() + "':";
    errorListener.semanticError(lineToken, msg,
        ErrorListener.SemanticClassification.TYPE_MUST_NOT_BE_FUNCTION);
  }

  private void emitNoCommonType(final Token lineToken, final ISymbol argument) {
    var msg = "With '" + argument.getFriendlyName() + "':";
    errorListener.semanticError(lineToken, msg,
        ErrorListener.SemanticClassification.UNABLE_TO_DETERMINE_COMMON_TYPE);
  }
}
