package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CANNOT_CALL_ABSTRACT_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_MUST_HAVE_SINGLE_PARAMETER;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_MUST_HAVE_TWO_PARAMETERS;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_MUST_RETURN_VALUE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_OR_DELEGATE_NOT_REQUIRED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_OR_DELEGATE_REQUIRED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.INCOMPATIBLE_TYPES;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.INCOMPATIBLE_TYPE_ARGUMENTS;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.IS_NOT_AN_AGGREGATE_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MUST_RETURN_BOOLEAN;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MUST_RETURN_INTEGER;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.REQUIRE_NO_ARGUMENTS;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.REQUIRE_ONE_ARGUMENT;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TYPE_MUST_BE_FUNCTION;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.UNABLE_TO_FIND_COMPARATOR_FOR_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.UNABLE_TO_FIND_HASHCODE_FOR_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.UNABLE_TO_FIND_PIPE_FOR_TYPE;

import java.util.Optional;
import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.StreamCallSymbol;
import org.ek9lang.core.CompilerException;

/**
 * Processes/updates and checks that a stream assembly is actually viable in terms of types.
 * This is a bit tricky because some pipeline parts can accept anything in and produce the same thing out,
 * but in of themselves they have no idea what that type will be.
 * It's only when we get to this stage we can push the types down from the source through each of the parts.
 * Then finally check to see if the terminal part could receive that type.
 * There are some other constraints as well, like sorting and grouping.
 * This focuses on the checking/population of consumes/produces of types in each of
 * the stages of the pipeline from the sources, pipe-line-parts* and termination.
 * This is quite tricky because some things types are fixed and others more flexible.
 * {@link org.ek9lang.compiler.support.SymbolFactory#newStreamPart(EK9Parser.StreamPartContext, IScope)}
 * This populates the StreamCallSymbol.
 */
public class ProcessStreamAssembly extends TypedSymbolAccess implements Consumer<StreamAssembly> {

  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();

  protected ProcessStreamAssembly(SymbolAndScopeManagement symbolAndScopeManagement,
                                  ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final StreamAssembly streamAssembly) {

    //OK so need to keep track of the 'type' flowing through the pipeline
    //ensuring it can be used, also it may get transformed!.

    ISymbol streamType = streamAssembly.source().getProducesSymbolType();

    for (var streamPartCtx : streamAssembly.streamParts()) {
      streamType = processStreamPart(streamPartCtx, streamType);
    }

    processStreamTermination(streamAssembly, streamType);

  }

  private ISymbol processStreamPart(final EK9Parser.StreamPartContext streamPartCtx, final ISymbol currentStreamType) {

    //Now depending on the pipeline part (map, filter, etc.) the resulting type might change.
    var newStreamType = evaluateStreamType(streamPartCtx, currentStreamType);

    //Now update the call with what it should expect and what it will produce.
    //If there are type mismatches - then errors will be issued and Void type will be employed.
    var streamCallPart = (StreamCallSymbol) symbolAndScopeManagement.getRecordedSymbol(streamPartCtx);
    streamCallPart.setConsumesSymbolType(currentStreamType);
    streamCallPart.setProducesSymbolType(newStreamType);
    streamCallPart.setType(newStreamType);

    return newStreamType;

  }

  /**
   * A bit of a giant switch here, I may refactor this once it is all working.
   * Upshot is look at the current stream part and the current stream type and check if that would work.
   * If it does work return the possibly altered stream type for the next part of the stream.
   *
   * @param streamPartCtx     The part of the stream being processed (i.e. the map, sort etc.).
   * @param currentStreamType The current type that is flowing through this part of the stream.
   * @return The possibly new current type that is going to be flowing out of this operation.
   */
  private ISymbol evaluateStreamType(final EK9Parser.StreamPartContext streamPartCtx,
                                     final ISymbol currentStreamType) {

    if (streamPartCtx.MAP() != null) {
      if (streamPartCtx.pipelinePart().size() == 1) {
        return checkViableMapFunctionOrError(streamPartCtx.pipelinePart(0), currentStreamType);
      }
      errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);
    } else if (streamPartCtx.FILTER() != null || streamPartCtx.SELECT() != null) {
      if (streamPartCtx.pipelinePart().size() == 1) {
        return checkViableSelectFunctionOrError(streamPartCtx.pipelinePart(0), currentStreamType);
      }
      errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);
    } else if (streamPartCtx.CALL() != null || streamPartCtx.ASYNC() != null) {
      return checkViableCallFunctionOrError(streamPartCtx, currentStreamType);
    } else if (streamPartCtx.SORT() != null) {
      return checkViableSortOrError(streamPartCtx, currentStreamType);
    } else if (streamPartCtx.UNIQ() != null) {
      return checkViableUniqOrError(streamPartCtx, currentStreamType);
    } else {
      throw new CompilerException("Stream part [" + streamPartCtx.op.getText() + "] not implemented");
    }
    return symbolAndScopeManagement.getEk9Types().ek9Void();
  }

  private ISymbol checkViableSortOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                         final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      checkTypeAsSuitableComparatorOrError(streamPartCtx, currentStreamType);
    } else if (streamPartCtx.pipelinePart().size() == 1) {
      checkViableSortFunctionOrError(streamPartCtx.pipelinePart().get(0), currentStreamType);
    }
    //The grammar will prevent the 'else' here.

    //No matter what 'sort' not alter the stream type being used, so return the same type.
    return currentStreamType;
  }

  private ISymbol checkViableUniqOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                         final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      checkTypeAsSuitableHashCodeOrError(streamPartCtx, currentStreamType);
    } else if (streamPartCtx.pipelinePart().size() == 1) {
      checkViableHashCodeFunctionOrError(streamPartCtx.pipelinePart().get(0), currentStreamType);
    }
    //The grammar will prevent the 'else' here.

    //No matter what 'uniq' not alter the stream type being used, so return the same type.
    return currentStreamType;
  }


  private void checkTypeAsSuitableComparatorOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                                    final ISymbol currentStreamType) {

    //For this mechanism to work the type must be an aggregate and must have the appropriate comparator operator.
    accessAggregateAsTypeOrError(streamPartCtx, currentStreamType)
        .ifPresent(aggregate -> checkComparatorPresentOrError(streamPartCtx.op, aggregate, currentStreamType));

  }

  private void checkTypeAsSuitableHashCodeOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                                  final ISymbol currentStreamType) {

    //For this mechanism to work the type must be an aggregate and must have the appropriate hashcode operator.
    accessAggregateAsTypeOrError(streamPartCtx, currentStreamType)
        .ifPresent(aggregate -> checkHashCodePresentOrError(streamPartCtx.op, aggregate, currentStreamType));

  }

  private void checkViableHashCodeFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                                  final ISymbol currentStreamType) {
    //For this mechanism to work the identifier reference used as the function must have a type of function
    //it must also accept specific arguments and return an Integer (i.e. must be a comparator function).
    accessFunctionSymbolOrError(partCtx)
        .ifPresent(function -> checkIsHashCodeFunctionOrError(partCtx.start, function, currentStreamType));

  }

  private void checkViableSortFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                              final ISymbol currentStreamType) {
    //For this mechanism to work the identifier reference used as the function must have a type of function
    //it must also accept specific arguments and return an Integer (i.e. must be a comparator function).
    accessFunctionSymbolOrError(partCtx)
        .ifPresent(function -> checkIsComparatorFunctionOrError(partCtx.start, function, currentStreamType));

  }

  private ISymbol checkViableCallFunctionOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                                 final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      if (currentStreamType instanceof FunctionSymbol functionSymbol) {
        return acceptsNoArgumentsDoesNotReturnVoid(streamPartCtx.op, functionSymbol);
      } else {
        var msg = "expecting a function not type '" + currentStreamType.getFriendlyName() + "':";
        errorListener.semanticError(streamPartCtx.op, msg, TYPE_MUST_BE_FUNCTION);
      }
    } else {
      errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_NOT_REQUIRED);
    }

    return symbolAndScopeManagement.getEk9Types().ek9Void();
  }

  private ISymbol checkViableMapFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                                final ISymbol currentStreamType) {

    return checkViableFunctionOrError(partCtx, currentStreamType);

  }

  private ISymbol checkViableSelectFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                                   final ISymbol currentStreamType) {

    var functionReturnType = checkViableFunctionOrError(partCtx, currentStreamType);
    if (!symbolAndScopeManagement.getEk9Types().ek9Boolean().isExactSameType(functionReturnType)) {
      errorListener.semanticError(partCtx.start, "", MUST_RETURN_BOOLEAN);
    }

    //Does not alter the current stream type flowing through the pipeline.
    return currentStreamType;

  }

  private ISymbol checkViableFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                             final ISymbol currentStreamType) {

    var possibleFunction = accessFunctionSymbolOrError(partCtx);
    if (possibleFunction.isEmpty()) {
      return symbolAndScopeManagement.getEk9Types().ek9Void();
    }
    return acceptsTypeOrError(partCtx.start, possibleFunction.get(), currentStreamType);

  }

  private Optional<IAggregateSymbol> accessAggregateAsTypeOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                                                  final ISymbol currentStreamType) {
    if (currentStreamType instanceof IAggregateSymbol aggregateSymbol) {
      return Optional.of(aggregateSymbol);
    }
    var msg = "need 'aggregate' type, but '" + currentStreamType.getFriendlyName() + "':";
    errorListener.semanticError(streamPartCtx.op, msg, IS_NOT_AN_AGGREGATE_TYPE);
    return Optional.empty();

  }

  private Optional<FunctionSymbol> accessFunctionSymbolOrError(final EK9Parser.PipelinePartContext partCtx) {
    var expectedMappingFunction = getRecordedAndTypedSymbol(partCtx);

    if (expectedMappingFunction != null && expectedMappingFunction.getType().isPresent()) {
      var expectedFunctionType = expectedMappingFunction.getType().get();

      if (!expectedMappingFunction.isMarkedAbstract()) {
        if (expectedFunctionType instanceof FunctionSymbol functionSymbol) {
          return Optional.of(functionSymbol);
        } else {
          var msg = "type '" + expectedFunctionType.getFriendlyName() + "':";
          errorListener.semanticError(partCtx.start, msg, FUNCTION_OR_DELEGATE_REQUIRED);
        }
      } else {
        errorListener.semanticError(partCtx.start, "", CANNOT_CALL_ABSTRACT_TYPE);
      }
    }
    return Optional.empty();
  }

  private ISymbol acceptsNoArgumentsDoesNotReturnVoid(final Token errorLocation,
                                                      final FunctionSymbol functionSymbol) {

    var errorMsg = "'" + functionSymbol.getFriendlyName() + "':";
    if (!functionSymbol.getCallParameters().isEmpty()) {
      errorListener.semanticError(errorLocation, errorMsg, REQUIRE_NO_ARGUMENTS);
    } else if (functionSymbol.getReturningSymbol().getType().isPresent()) {
      var returnType = functionSymbol.getReturningSymbol().getType().get();
      if (returnType.isExactSameType(symbolAndScopeManagement.getEk9Types().ek9Void())) {
        errorListener.semanticError(errorLocation, errorMsg, FUNCTION_MUST_RETURN_VALUE);
      }
      return returnType;
    }

    return symbolAndScopeManagement.getEk9Types().ek9Void();
  }

  private void checkHashCodePresentOrError(final Token errorLocation,
                                           final IAggregateSymbol aggregateSymbol,
                                           final ISymbol currentStreamType) {

    //Need to search on the aggregateSymbol for a hashcode '#?' operator.
    var search = new MethodSymbolSearch("#?");
    locateMethodOrError(search, errorLocation, aggregateSymbol, currentStreamType, UNABLE_TO_FIND_HASHCODE_FOR_TYPE);

  }

  private void checkComparatorPresentOrError(final Token errorLocation,
                                             final IAggregateSymbol aggregateSymbol,
                                             final ISymbol currentStreamType) {

    //Need to search on the aggregateSymbol for a comparator '<=>' operator with compatible type arguments.
    //Accepts a single argument compatible with the currentStreamType
    var search = new MethodSymbolSearch("<=>")
        .addTypeParameter(currentStreamType);
    locateMethodOrError(search, errorLocation, aggregateSymbol, currentStreamType, UNABLE_TO_FIND_COMPARATOR_FOR_TYPE);

  }

  private void locateMethodOrError(final MethodSymbolSearch search,
                                   final Token errorLocation,
                                   final IAggregateSymbol aggregateSymbol,
                                   final ISymbol currentStreamType,
                                   final ErrorListener.SemanticClassification errorClassification) {

    MethodSymbolSearchResult result = new MethodSymbolSearchResult();
    result = aggregateSymbol.resolveMatchingMethods(search, result);
    if (!result.isSingleBestMatchPresent()) {
      var msg = "wrt pipeline type '" + currentStreamType.getFriendlyName()
          + "':";
      errorListener.semanticError(errorLocation, msg, errorClassification);
    }
  }

  private void checkIsComparatorFunctionOrError(final Token errorLocation,
                                                final FunctionSymbol functionSymbol,
                                                final ISymbol currentStreamType) {
    //For this to be valid the function must take two arguments compatible with currentStreamType
    //And it must return an Integer.
    var errorMsg = "'" + functionSymbol.getFriendlyName() + "':";
    if (functionSymbol.getCallParameters().size() != 2) {
      errorListener.semanticError(errorLocation, errorMsg, FUNCTION_MUST_HAVE_TWO_PARAMETERS);
    } else {
      var argumentTypes = symbolTypeExtractor.apply(functionSymbol.getCallParameters());
      //Now check those types are compatible with currentStreamType
      argumentTypes.forEach(argumentType -> {
        if (!currentStreamType.isAssignableTo(argumentType)) {
          var typeErrorMsg = "wrt '" + functionSymbol.getFriendlyName()
              + "' and pipeline type '" + currentStreamType.getFriendlyName() + "':";
          errorListener.semanticError(errorLocation, typeErrorMsg, INCOMPATIBLE_TYPE_ARGUMENTS);
        }
      });
    }

    if (functionSymbol.getReturningSymbol().getType().isPresent()) {
      var returnType = functionSymbol.getReturningSymbol().getType().get();
      if (!returnType.isExactSameType(symbolAndScopeManagement.getEk9Types().ek9Integer())) {
        errorListener.semanticError(errorLocation, errorMsg, MUST_RETURN_INTEGER);
      }
    }

  }

  private void checkIsHashCodeFunctionOrError(final Token errorLocation,
                                              final FunctionSymbol functionSymbol,
                                              final ISymbol currentStreamType) {
    //For this to be valid the function must take one argument compatible with currentStreamType
    //And it must return an Integer.
    var errorMsg = "'" + functionSymbol.getFriendlyName() + "':";
    if (functionSymbol.getCallParameters().size() != 1) {
      errorListener.semanticError(errorLocation, errorMsg, FUNCTION_MUST_HAVE_SINGLE_PARAMETER);
    } else {
      var argumentTypes = symbolTypeExtractor.apply(functionSymbol.getCallParameters());
      //Now check that type is compatible with currentStreamType
      argumentTypes.forEach(argumentType -> {
        if (!currentStreamType.isAssignableTo(argumentType)) {
          var typeErrorMsg = "wrt '" + functionSymbol.getFriendlyName()
              + "' and pipeline type '" + currentStreamType.getFriendlyName() + "':";
          errorListener.semanticError(errorLocation, typeErrorMsg, INCOMPATIBLE_TYPE_ARGUMENTS);
        }
      });
    }

    if (functionSymbol.getReturningSymbol().getType().isPresent()) {
      var returnType = functionSymbol.getReturningSymbol().getType().get();
      if (!returnType.isExactSameType(symbolAndScopeManagement.getEk9Types().ek9Integer())) {
        errorListener.semanticError(errorLocation, errorMsg, MUST_RETURN_INTEGER);
      }
    }

  }

  private ISymbol acceptsTypeOrError(final Token errorLocation,
                                     final FunctionSymbol functionSymbol,
                                     final ISymbol currentStreamType) {

    if (functionSymbol.getCallParameters().size() == 1) {
      //OK so now check if the type that we have here could be received.
      var paramType = functionSymbol.getCallParameters().get(0).getType();
      if (currentStreamType.isAssignableTo(paramType)) {
        if (functionSymbol.getReturningSymbol().getType().isPresent()) {
          return functionSymbol.getReturningSymbol().getType().get();
        }
      } else {
        var msg = "'" + functionSymbol.getFriendlyName() + "' incompatible with argument of '"
            + currentStreamType.getFriendlyName() + "':";
        errorListener.semanticError(errorLocation, msg, INCOMPATIBLE_TYPES);
      }
    } else {
      var msg = "'" + functionSymbol.getFriendlyName() + "':";
      errorListener.semanticError(errorLocation, msg, REQUIRE_ONE_ARGUMENT);
    }

    return symbolAndScopeManagement.getEk9Types().ek9Void();

  }

  private void processStreamTermination(final StreamAssembly streamAssembly, final ISymbol streamType) {

    streamAssembly.termination().getType().ifPresent(
        terminationType -> processTermination(streamAssembly.termination(), terminationType, streamType));

  }

  private void processTermination(StreamCallSymbol termination,
                                  final ISymbol terminationType,
                                  final ISymbol streamType) {

    if (terminationType instanceof IAggregateSymbol terminationTypeAggregate) {
      var search = new MethodSymbolSearch("|").addTypeParameter(streamType);
      MethodSymbolSearchResult result = new MethodSymbolSearchResult();
      result = terminationTypeAggregate.resolveMatchingMethods(search, result);

      if (result.isSingleBestMatchPresent()) {
        result.getSingleBestMatchSymbol().ifPresent(bestTypeMatch -> {
          //Note that we set the consuming type, but it may be that a promotion of that type is needed.
          //i.e. Character to String promotion, but the type itself has the definition of what it can be promoted to.
          termination.setConsumesSymbolType(streamType);
          termination.setConsumesSymbolPromotionRequired(!bestTypeMatch.isExactSameType(streamType));
        });
      } else {
        var msg = "wrt pipeline type '" + streamType.getFriendlyName()
            + "' and terminal type '" + terminationType.getFriendlyName() + "':";
        errorListener.semanticError(termination.getSourceToken(), msg, UNABLE_TO_FIND_PIPE_FOR_TYPE);
      }
    }
  }

}
