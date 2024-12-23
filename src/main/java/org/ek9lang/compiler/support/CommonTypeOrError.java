package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TYPE_MUST_BE_FUNCTION;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TYPE_MUST_NOT_BE_FUNCTION;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.UNABLE_TO_DETERMINE_COMMON_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Attempts to find a common type from the CommonTypeDeterminationDetails or issues errors.
 * Alteration to this processing is for 'AnyClass' and 'AnyRecord' - those are a last resort.
 * So supers first, then traits (if appropriate) and finally AnyClass/AnyRecord.
 */
public class CommonTypeOrError extends RuleSupport
    implements Function<CommonTypeDeterminationDetails, Optional<ISymbol>> {

  public CommonTypeOrError(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public Optional<ISymbol> apply(final CommonTypeDeterminationDetails details) {

    return getCommonType(details);

  }

  private Optional<ISymbol> getCommonType(final CommonTypeDeterminationDetails details) {

    //But note the ek9 developer could have accidentally mixed vars which were functions and aggregates
    //We have to accept this but detect and issue error.
    if (canCommonTypeBeDetermined(details)) {
      return determineCommonType(details);
    }

    return Optional.empty();
  }

  private boolean canCommonTypeBeDetermined(final CommonTypeDeterminationDetails details) {

    if (details.argumentTypes().isEmpty()) {
      return false;
    }

    return (details.argumentTypes().get(0) instanceof FunctionSymbol
        && checkFunctionSymbols(details))
        || (details.argumentTypes().get(0) instanceof AggregateSymbol
        && checkAggregateSymbols(details));
  }

  private Optional<ISymbol> determineCommonType(final CommonTypeDeterminationDetails details) {

    final List<ISymbol> typesToTry = new ArrayList<>();

    getTypesToTry(details.argumentTypes().get(0), typesToTry);
    //We do not want to include these - only later as a last resort
    var anyClassType = symbolsAndScopes.getEk9Types().ek9AnyClass();
    var anyRecordType = symbolsAndScopes.getEk9Types().ek9AnyRecord();
    typesToTry.remove(anyClassType);
    typesToTry.remove(anyRecordType);

    for (var type : typesToTry) {
      if (allAssignableTo(type, details.argumentTypes())) {
        return Optional.of(type);
      }
    }

    if (allAssignableTo(anyClassType, details.argumentTypes())) {
      return Optional.of(anyClassType);
    }

    if (allAssignableTo(anyRecordType, details.argumentTypes())) {
      return Optional.of(anyRecordType);
    }

    emitNoCommonType(details.lineToken(), details.argumentTypes().get(0));

    return Optional.empty();
  }

  private void getTypesToTry(final ISymbol symbolType, final List<ISymbol> addToTypes) {


    if (addToTypes.contains(symbolType)) {
      return;
    }

    addToTypes.add(symbolType);

    if (symbolType instanceof FunctionSymbol functionSymbol && functionSymbol.getSuperFunction().isPresent()) {
      getTypesToTry(functionSymbol.getSuperFunction().get(), addToTypes);
    } else if (symbolType instanceof AggregateSymbol aggregateSymbol) {

      if (aggregateSymbol.getSuperAggregate().isPresent()) {
        getTypesToTry(aggregateSymbol.getSuperAggregate().get(), addToTypes);
      }

      if (aggregateSymbol instanceof AggregateWithTraitsSymbol aggregateWithTraitsSymbol) {
        aggregateWithTraitsSymbol.getAllTraits().forEach(symbol -> getTypesToTry(symbol, addToTypes));
      }
    }

  }

  private boolean allAssignableTo(final ISymbol typeSymbol, final List<ISymbol> typeList) {

    return typeList.stream().filter(fun -> fun.isAssignableTo(typeSymbol)).count() == typeList.size();
  }

  private boolean checkAggregateSymbols(final CommonTypeDeterminationDetails details) {

    int count = 0;

    for (int i = 0; i < details.argumentTypes().size(); i++) {
      if (details.argumentTypes().get(i) instanceof AggregateSymbol) {
        count++;
      } else {
        emitExpectingAggregateError(details.lineToken(), details.argumentSymbols().get(i));
      }
    }

    return count == details.argumentSymbols().size();
  }

  private boolean checkFunctionSymbols(final CommonTypeDeterminationDetails details) {

    int count = 0;
    for (int i = 0; i < details.argumentTypes().size(); i++) {
      if (details.argumentTypes().get(i) instanceof FunctionSymbol) {
        count++;
      } else {
        emitExpectingFunctionError(details.lineToken(), details.argumentSymbols().get(i));
      }
    }

    return count == details.argumentSymbols().size();
  }

  private void emitExpectingFunctionError(final IToken lineToken, final ISymbol argument) {

    final var msg = "Expecting a function not '" + argument.getFriendlyName() + "':";
    errorListener.semanticError(lineToken, msg, TYPE_MUST_BE_FUNCTION);

  }

  private void emitExpectingAggregateError(final IToken lineToken, final ISymbol argument) {

    final var msg = "Expecting a non-function not function '" + argument.getName() + "':";
    errorListener.semanticError(lineToken, msg, TYPE_MUST_NOT_BE_FUNCTION);

  }

  private void emitNoCommonType(final IToken lineToken, final ISymbol argument) {

    final var msg = "With '" + argument.getFriendlyName() + "':";
    errorListener.semanticError(lineToken, msg, UNABLE_TO_DETERMINE_COMMON_TYPE);

  }
}
