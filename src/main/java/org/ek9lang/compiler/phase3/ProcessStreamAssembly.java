package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_MUST_HAVE_SINGLE_PARAMETER;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_MUST_HAVE_TWO_PARAMETERS;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_MUST_RETURN_VALUE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_OR_DELEGATE_NOT_REQUIRED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_OR_DELEGATE_REQUIRED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.INCOMPATIBLE_TYPES;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.IS_NOT_AN_AGGREGATE_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MISSING_ITERATE_METHOD;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MUST_RETURN_BOOLEAN;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MUST_RETURN_INTEGER;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.REQUIRE_NO_ARGUMENTS;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.REQUIRE_ONE_ARGUMENT;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.RETURNING_MISSING;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TYPE_MUST_BE_FUNCTION;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.UNABLE_TO_FIND_COMPARATOR_FOR_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.UNABLE_TO_FIND_HASHCODE_FOR_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.UNABLE_TO_FIND_PIPE_FOR_TYPE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.ParameterisedLocator;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.StreamCallSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
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

  private final ProcessStreamFunctionOrError processStreamFunctionOrError;
  private final CheckStreamFunctionArguments checkStreamFunctionArguments;
  private final CheckHeadTailSkipOperation checkHeadTailSkipOperation;
  private final GetIteratorType getIteratorType;
  private final ParameterisedLocator parameterisedLocator;
  private final Map<Integer, BiFunction<EK9Parser.StreamPartContext, ISymbol, ISymbol>> streamFunctionMap;

  protected ProcessStreamAssembly(final SymbolAndScopeManagement symbolAndScopeManagement,
                                  final SymbolFactory symbolFactory,
                                  final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    this.processStreamFunctionOrError = new ProcessStreamFunctionOrError(symbolAndScopeManagement, errorListener);
    this.checkStreamFunctionArguments = new CheckStreamFunctionArguments(symbolAndScopeManagement, errorListener);
    this.checkHeadTailSkipOperation = new CheckHeadTailSkipOperation(symbolAndScopeManagement, errorListener);
    this.getIteratorType = new GetIteratorType(symbolAndScopeManagement, errorListener);
    this.parameterisedLocator = new ParameterisedLocator(symbolAndScopeManagement, symbolFactory, errorListener, true);
    this.streamFunctionMap = setupOperationToFunctionMapping();
  }

  /**
   * Rather than use a very large switch/if/else block a map is used to process the appropriate Stream operation.
   */
  private Map<Integer, BiFunction<EK9Parser.StreamPartContext, ISymbol, ISymbol>> setupOperationToFunctionMapping() {

    final Map<Integer, BiFunction<EK9Parser.StreamPartContext, ISymbol, ISymbol>> primaryMap
        = Map.of(EK9Parser.FILTER, this::checkViableSelectFunctionOrError,
        EK9Parser.SELECT, this::checkViableSelectFunctionOrError,
        EK9Parser.MAP, this::checkViableFunctionOrError,
        EK9Parser.GROUP, this::checkViableGroupOrError,
        EK9Parser.JOIN, this::checkViableJoinOrError,
        EK9Parser.SPLIT, this::checkViableSplitOrError,
        EK9Parser.UNIQ, this::checkViableUniqOrError,
        EK9Parser.SORT, this::checkViableSortOrError
    );

    final Map<Integer, BiFunction<EK9Parser.StreamPartContext, ISymbol, ISymbol>> secondaryMap
        = Map.of(EK9Parser.FLATTEN, this::checkViableFlattenOrError,
        EK9Parser.CALL, this::checkViableCallFunctionOrError,
        EK9Parser.ASYNC, this::checkViableCallFunctionOrError,
        EK9Parser.TEE, this::invalidStreamOperation,
        EK9Parser.SKIPPING, this::checkViableIntegerOrError,
        EK9Parser.HEAD, this::checkViableIntegerOrError,
        EK9Parser.TAIL, this::checkViableIntegerOrError);

    Map<Integer, BiFunction<EK9Parser.StreamPartContext, ISymbol, ISymbol>> rtn = new HashMap<>();
    rtn.putAll(primaryMap);
    rtn.putAll(secondaryMap);

    return rtn;
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

    var operation = streamFunctionMap.getOrDefault(streamPartCtx.op.getType(), this::invalidStreamOperation);
    return operation.apply(streamPartCtx, currentStreamType);

  }

  private ISymbol invalidStreamOperation(final EK9Parser.StreamPartContext streamPartCtx,
                                         final ISymbol currentStreamType) {
    throw new CompilerException("Stream part [" + streamPartCtx.op.getText() + "] not implemented");
  }

  /**
   * This check if fore skip, head and tail.
   * No argument means default to 1, or a positive integer literal, or a function that returns an integer.
   * Will be a run time check if that value is zero or negative.
   */
  private ISymbol checkViableIntegerOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                            final ISymbol currentStreamType) {

    checkHeadTailSkipOperation.accept(streamPartCtx);
    //Always the same type.
    return currentStreamType;

  }

  private ISymbol checkViableGroupOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                          final ISymbol currentStreamType) {
    if (streamPartCtx.pipelinePart().size() == 1) {
      var functionReturnType = checkFunctionOrError(streamPartCtx.pipelinePart(0), currentStreamType);
      //Now it is essential that the return type has hashcode.
      checkTypeAsSuitableHashCodeOrError(streamPartCtx, functionReturnType);
    } else {
      checkTypeAsSuitableHashCodeOrError(streamPartCtx, currentStreamType);
    }

    //But this will return a type of 'List of symbolType'. So that needs to be resolved.
    return resolveParameterisedListType(streamPartCtx.op, currentStreamType);
  }

  private ISymbol checkViableSplitOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                          final ISymbol currentStreamType) {
    if (streamPartCtx.pipelinePart().size() == 1) {
      checkFunctionAndReturnsBooleanOrError(streamPartCtx.pipelinePart(0), currentStreamType);
    } else {
      errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);
    }

    //But this will return a type of 'List of symbolType'. So that needs to be resolved.
    return resolveParameterisedListType(streamPartCtx.op, currentStreamType);
  }

  private ISymbol checkViableFunctionOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                             final ISymbol currentStreamType) {
    if (streamPartCtx.pipelinePart().size() == 1) {
      return checkFunctionOrError(streamPartCtx.pipelinePart(0), currentStreamType);
    }
    errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);
    return symbolAndScopeManagement.getEk9Types().ek9Void();
  }

  private ISymbol checkFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                       final ISymbol currentStreamType) {

    var possibleFunction = processStreamFunctionOrError.apply(partCtx);
    if (possibleFunction.isEmpty()) {
      return symbolAndScopeManagement.getEk9Types().ek9Void();
    }
    return acceptsTypeOrError(new StreamFunctionCheckData(partCtx.start, possibleFunction.get(), currentStreamType));

  }

  private ISymbol checkViableSelectFunctionOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                                   final ISymbol currentStreamType) {
    if (streamPartCtx.pipelinePart().size() == 1) {
      return checkFunctionAndReturnsBooleanOrError(streamPartCtx.pipelinePart(0), currentStreamType);
    }
    errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);
    return symbolAndScopeManagement.getEk9Types().ek9Void();
  }

  private ISymbol checkFunctionAndReturnsBooleanOrError(final EK9Parser.PipelinePartContext partCtx,
                                                        final ISymbol currentStreamType) {

    var functionReturnType = checkFunctionOrError(partCtx, currentStreamType);
    if (!symbolAndScopeManagement.getEk9Types().ek9Boolean().isExactSameType(functionReturnType)) {
      errorListener.semanticError(partCtx.start, "", MUST_RETURN_BOOLEAN);
    }

    //Does not alter the current stream type flowing through the pipeline.
    return currentStreamType;

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

  private ISymbol checkViableSortOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                         final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      checkTypeAsSuitableComparatorOrError(streamPartCtx, currentStreamType);
    } else if (streamPartCtx.pipelinePart().size() == 1) {
      checkViableSortFunctionOrError(streamPartCtx.pipelinePart().get(0), currentStreamType);
    }

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

    //No matter what 'uniq' not alter the stream type being used, so return the same type.
    return currentStreamType;
  }

  private ISymbol checkViableJoinOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                         final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);
    } else if (streamPartCtx.pipelinePart().size() == 1) {
      checkViableJoinFunctionOrError(streamPartCtx.pipelinePart().get(0), currentStreamType);
    }

    //No matter what 'join' not alter the stream type being used, so return the same type.
    return currentStreamType;
  }

  private ISymbol checkViableFlattenOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                            final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      return accessAggregateAsTypeOrError(streamPartCtx, currentStreamType)
          .map(aggregate -> new StreamAggregateCheckData(streamPartCtx.op, aggregate, currentStreamType))
          .map(this::getIteratorTypeOrError)
          .orElseGet(() -> symbolAndScopeManagement.getEk9Types().ek9Void());
    }
    errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_NOT_REQUIRED);
    return symbolAndScopeManagement.getEk9Types().ek9Void();

  }

  private ISymbol getIteratorTypeOrError(final StreamAggregateCheckData streamAggregateCheckData) {
    var iteratorType = getIteratorType.apply(streamAggregateCheckData.aggregateSymbol());
    if (iteratorType.isPresent()) {
      return iteratorType.get();
    }

    errorListener.semanticError(streamAggregateCheckData.errorLocation(), "", MISSING_ITERATE_METHOD);
    return symbolAndScopeManagement.getEk9Types().ek9Void();
  }

  private void checkTypeAsSuitableComparatorOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                                    final ISymbol currentStreamType) {

    accessAggregateAsTypeOrError(streamPartCtx, currentStreamType)
        .map(aggregate -> new StreamAggregateCheckData(streamPartCtx.op, aggregate, currentStreamType))
        .ifPresent(this::checkComparatorPresentOrError);

  }

  private void checkTypeAsSuitableHashCodeOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                                  final ISymbol symbolType) {

    accessAggregateAsTypeOrError(streamPartCtx, symbolType)
        .map(aggregate -> new StreamAggregateCheckData(streamPartCtx.op, aggregate, symbolType))
        .ifPresent(this::checkHashCodePresentOrError);

  }

  private void checkViableHashCodeFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                                  final ISymbol currentStreamType) {

    //For this mechanism to work the identifier reference used as the function must have a type of function
    //it must also accept specific arguments and return an Integer (i.e. must be a comparator function).
    processStreamFunctionOrError.apply(partCtx)
        .map(function -> new StreamFunctionCheckData(partCtx.start, function, currentStreamType))
        .ifPresent(this::checkIsHashCodeFunctionOrError);

  }

  private void checkViableJoinFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                              final ISymbol currentStreamType) {

    //For this mechanism to work the identifier reference used as the function must have a type of function
    //it must also accept specific arguments and return a type that is the same as the arguments.
    processStreamFunctionOrError.apply(partCtx)
        .map(function -> new StreamFunctionCheckData(partCtx.start, function, currentStreamType))
        .ifPresent(this::checkIsJoinFunctionOrError);

  }

  private void checkViableSortFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                              final ISymbol currentStreamType) {

    //For this mechanism to work the identifier reference used as the function must have a type of function
    //it must also accept specific arguments and return an Integer (i.e. must be a comparator function).
    processStreamFunctionOrError.apply(partCtx)
        .map(function -> new StreamFunctionCheckData(partCtx.start, function, currentStreamType))
        .ifPresent(this::checkIsComparatorFunctionOrError);
  }


  private Optional<IAggregateSymbol> accessAggregateAsTypeOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                                                  final ISymbol symbolType) {
    if (symbolType instanceof IAggregateSymbol aggregateSymbol) {
      return Optional.of(aggregateSymbol);
    }
    var msg = "need 'aggregate' type, but '" + symbolType.getFriendlyName() + "':";
    errorListener.semanticError(streamPartCtx.op, msg, IS_NOT_AN_AGGREGATE_TYPE);
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

  private void checkHashCodePresentOrError(final StreamAggregateCheckData streamAggregateCheckData) {

    //Need to search on the aggregateSymbol for a hashcode '#?' operator.
    var search = new MethodSymbolSearch("#?");
    locateMethodOrError(search, streamAggregateCheckData, UNABLE_TO_FIND_HASHCODE_FOR_TYPE);

  }

  private void checkComparatorPresentOrError(final StreamAggregateCheckData streamAggregateCheckData) {

    //Need to search on the aggregateSymbol for a comparator '<=>' operator with compatible type arguments.
    //Accepts a single argument compatible with the symbolType
    var search = new MethodSymbolSearch("<=>")
        .addTypeParameter(streamAggregateCheckData.symbolType());

    locateMethodOrError(search, streamAggregateCheckData, UNABLE_TO_FIND_COMPARATOR_FOR_TYPE);

  }

  private void locateMethodOrError(final MethodSymbolSearch search,
                                   final StreamAggregateCheckData streamAggregateCheckData,
                                   final ErrorListener.SemanticClassification errorClassification) {

    MethodSymbolSearchResult result = new MethodSymbolSearchResult();
    result = streamAggregateCheckData.aggregateSymbol().resolveMatchingMethods(search, result);
    if (!result.isSingleBestMatchPresent()) {
      var msg = "wrt pipeline type '" + streamAggregateCheckData.symbolType().getFriendlyName()
          + "':";
      errorListener.semanticError(streamAggregateCheckData.errorLocation(), msg, errorClassification);
    }
  }

  private void checkIsComparatorFunctionOrError(final StreamFunctionCheckData functionData) {
    //For this to be valid the function must take two arguments compatible with symbolType
    //And it must return an Integer.
    var errorMsg = "'" + functionData.functionSymbol().getFriendlyName() + "':";
    if (functionData.functionSymbol().getCallParameters().size() != 2) {
      errorListener.semanticError(functionData.errorLocation(), errorMsg, FUNCTION_MUST_HAVE_TWO_PARAMETERS);
    } else {
      checkStreamFunctionArguments.accept(functionData);
    }

    if (functionData.functionSymbol().getReturningSymbol().getType().isPresent()) {
      var returnType = functionData.functionSymbol().getReturningSymbol().getType().get();
      if (!returnType.isExactSameType(symbolAndScopeManagement.getEk9Types().ek9Integer())) {
        errorListener.semanticError(functionData.errorLocation(), errorMsg, MUST_RETURN_INTEGER);
      }
    }

  }

  private void checkIsHashCodeFunctionOrError(final StreamFunctionCheckData functionData) {
    //For this to be valid the function must take one argument compatible with symbolType
    //And it must return an Integer.
    var errorMsg = "'" + functionData.functionSymbol().getFriendlyName() + "':";
    if (functionData.functionSymbol().getCallParameters().size() != 1) {
      errorListener.semanticError(functionData.errorLocation(), errorMsg, FUNCTION_MUST_HAVE_SINGLE_PARAMETER);
    } else {
      checkStreamFunctionArguments.accept(functionData);
    }

    if (functionData.functionSymbol().getReturningSymbol().getType().isPresent()) {
      var returnType = functionData.functionSymbol().getReturningSymbol().getType().get();
      if (!returnType.isExactSameType(symbolAndScopeManagement.getEk9Types().ek9Integer())) {
        errorListener.semanticError(functionData.errorLocation(), errorMsg, MUST_RETURN_INTEGER);
      }
    }

  }

  private void checkIsJoinFunctionOrError(final StreamFunctionCheckData functionData) {
    //For this to be valid the function must take two arguments compatible with symbolType
    //And it must return a variable that is the same type as the symbolType (or compatible with it.)
    var errorMsg = "'" + functionData.functionSymbol().getFriendlyName() + "':";
    if (functionData.functionSymbol().getCallParameters().size() != 2) {
      errorListener.semanticError(functionData.errorLocation(), errorMsg, FUNCTION_MUST_HAVE_TWO_PARAMETERS);
    } else {
      checkStreamFunctionArguments.accept(functionData);
    }

    if (functionData.functionSymbol().getReturningSymbol().getType().isPresent()) {
      var returnType = functionData.functionSymbol().getReturningSymbol().getType().get();
      if (returnType.isExactSameType(symbolAndScopeManagement.getEk9Types().ek9Void())) {
        errorListener.semanticError(functionData.errorLocation(), errorMsg, RETURNING_MISSING);
      } else if (!returnType.isAssignableTo(functionData.currentStreamType())) {
        var typeErrorMsg = "wrt '" + functionData.functionSymbol().getFriendlyName()
            + "' and pipeline type '" + functionData.currentStreamType().getFriendlyName() + "':";
        errorListener.semanticError(functionData.errorLocation(), typeErrorMsg, INCOMPATIBLE_TYPES);
      }
    }

  }

  private ISymbol resolveParameterisedListType(Token opLocation, final ISymbol currentStreamType) {
    //Access the generic List type - this has been pre-located for quicker use.
    final var listType = symbolAndScopeManagement.getEk9Types().ek9List();

    //Now get the parameterised type.
    final var typeData = new ParameterisedTypeData(new Ek9Token(opLocation), listType, List.of(currentStreamType));
    final var resolvedNewType = parameterisedLocator.resolveOrDefine(typeData);

    return resolvedNewType.orElseGet(() -> symbolAndScopeManagement.getEk9Types().ek9Void());

  }

  private ISymbol acceptsTypeOrError(final StreamFunctionCheckData functionData) {

    if (functionData.functionSymbol().getCallParameters().size() == 1) {
      //OK so now check if the type that we have here could be received.
      var paramType = functionData.functionSymbol().getCallParameters().get(0).getType();
      if (functionData.currentStreamType().isAssignableTo(paramType)) {
        if (functionData.functionSymbol().getReturningSymbol().getType().isPresent()) {
          return functionData.functionSymbol().getReturningSymbol().getType().get();
        }
      } else {
        var msg = "'" + functionData.functionSymbol().getFriendlyName() + "' incompatible with argument of '"
            + functionData.currentStreamType().getFriendlyName() + "':";
        errorListener.semanticError(functionData.errorLocation(), msg, INCOMPATIBLE_TYPES);
      }
    } else {
      var msg = "'" + functionData.functionSymbol().getFriendlyName() + "':";
      errorListener.semanticError(functionData.errorLocation(), msg, REQUIRE_ONE_ARGUMENT);
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
