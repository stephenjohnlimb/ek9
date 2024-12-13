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
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.STREAM_TYPE_NOT_DEFINED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TYPE_MUST_BE_FUNCTION;
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
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
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
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.CompilerException;

/**
 * Processes/updates and checks that a stream assembly is actually viable in terms of types.
 * This is a bit tricky because some pipeline parts can accept anything in and produce the same thing,
 * but in of themselves they have no idea what that type will be.
 * It's only when we get to this stage we can push the types down from the source through each of the parts.
 * Then finally check to see if the terminal part could receive that type.
 * There are some other constraints as well, like sorting and grouping.
 * This focuses on the checking/population of consumes/produces of types in each of
 * the stages of the pipeline from the sources, pipe-line-parts* and termination.
 * This is quite tricky because some things types are fixed and others more flexible.
 * {@link org.ek9lang.compiler.support.SymbolFactory#newStreamPart(EK9Parser.StreamPartContext, IScope)}
 * This populates the StreamCallSymbol.
 * <br/>
 * This is quite large and a little complex, but it is the main processor of the whole EK9 stream idea.
 */
public class StreamAssemblyOrError extends TypedSymbolAccess implements Consumer<StreamAssemblyData> {

  private final StreamFunctionOrError streamFunctionOrError;
  private final StreamFunctionArgumentsOrError streamFunctionArgumentsOrError;
  private final HeadTailSkipOrError headTailSkipOrError;
  private final GetIteratorType getIteratorType;
  private final ParameterisedLocator parameterisedLocator;
  private final ComparatorPresentOrError comparatorPresentOrError;
  private final HashCodePresentOrError hashCodePresentOrError;
  private final Map<Integer, BiFunction<EK9Parser.StreamPartContext, ISymbol, ISymbol>> streamFunctionMap;

  protected StreamAssemblyOrError(final SymbolsAndScopes symbolsAndScopes,
                                  final SymbolFactory symbolFactory,
                                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.streamFunctionOrError = new StreamFunctionOrError(symbolsAndScopes, errorListener);
    this.streamFunctionArgumentsOrError = new StreamFunctionArgumentsOrError(symbolsAndScopes, errorListener);
    this.headTailSkipOrError = new HeadTailSkipOrError(symbolsAndScopes, errorListener);
    this.getIteratorType = new GetIteratorType(symbolsAndScopes, errorListener);
    this.parameterisedLocator = new ParameterisedLocator(symbolsAndScopes, symbolFactory, errorListener, true);
    this.comparatorPresentOrError = new ComparatorPresentOrError(symbolsAndScopes, errorListener);
    this.hashCodePresentOrError = new HashCodePresentOrError(symbolsAndScopes, errorListener);

    this.streamFunctionMap = setupOperationToFunctionMapping();

  }

  /**
   * Rather than use a very large switch/if/else block a map is used to process the appropriate Stream operation.
   */
  private Map<Integer, BiFunction<EK9Parser.StreamPartContext, ISymbol, ISymbol>> setupOperationToFunctionMapping() {

    final Map<Integer, BiFunction<EK9Parser.StreamPartContext, ISymbol, ISymbol>> primaryMap
        = Map.of(EK9Parser.FILTER, this::selectFunctionOrError,
        EK9Parser.SELECT, this::selectFunctionOrError,
        EK9Parser.MAP, this::functionOrError,
        EK9Parser.GROUP, this::groupOrError,
        EK9Parser.JOIN, this::joinOrError,
        EK9Parser.SPLIT, this::splitOrError,
        EK9Parser.UNIQ, this::uniqOrError,
        EK9Parser.SORT, this::sortOrError
    );

    final Map<Integer, BiFunction<EK9Parser.StreamPartContext, ISymbol, ISymbol>> secondaryMap
        = Map.of(EK9Parser.FLATTEN, this::flattenOrError,
        EK9Parser.CALL, this::callFunctionOrError,
        EK9Parser.ASYNC, this::callFunctionOrError,
        EK9Parser.TEE, this::teeOrError,
        EK9Parser.SKIPPING, this::integerOrError,
        EK9Parser.HEAD, this::integerOrError,
        EK9Parser.TAIL, this::integerOrError);

    final Map<Integer, BiFunction<EK9Parser.StreamPartContext, ISymbol, ISymbol>> rtn = new HashMap<>();
    rtn.putAll(primaryMap);
    rtn.putAll(secondaryMap);

    return rtn;
  }

  @Override
  public void accept(final StreamAssemblyData streamAssembly) {

    //OK so need to keep track of the 'type' flowing through the pipeline
    //ensuring it can be used, also it may get transformed on the way through!.
    //So this is sort of like executing the pipeline, but not looking at values only the types.

    var streamType = streamAssembly.source().getProducesSymbolType();
    streamTypeOrError(streamAssembly.source().getSourceToken(), streamType);

    for (var streamPartCtx : streamAssembly.streamParts()) {
      streamType = streamPartOrError(streamPartCtx, streamType);
      streamTypeOrError(new Ek9Token(streamPartCtx.start), streamType);
    }

    streamTerminationOrError(streamAssembly, streamType);

  }

  private void streamTypeOrError(final IToken errorLocation, final ISymbol streamType) {

    //I would normally expect other errors to have been emitted before this, so we I see this it may mean
    //that other checks should be enhanced.
    if (streamType == null || symbolsAndScopes.getEk9Types().ek9Void().isExactSameType(streamType)) {
      errorListener.semanticError(errorLocation, "", STREAM_TYPE_NOT_DEFINED);
    }

  }

  private ISymbol streamPartOrError(final EK9Parser.StreamPartContext streamPartCtx, final ISymbol currentStreamType) {

    //Now depending on the pipeline part (map, filter, etc.) the resulting type might change.
    final var newStreamType = evaluateStreamType(streamPartCtx, currentStreamType);

    //Now update the call with what it should expect and what it will produce.
    //If there are type mismatches - then errors will be issued and Void type will be employed.
    if (symbolsAndScopes.getRecordedSymbol(streamPartCtx) instanceof StreamCallSymbol streamCallPart) {
      streamCallPart.setConsumesSymbolType(currentStreamType);
      streamCallPart.setProducesSymbolType(newStreamType);
      streamCallPart.setType(newStreamType);
    }

    return newStreamType;

  }

  /**
   * Upshot is look at the current stream part and the current stream type and check if that would work.
   * If it does work return the possibly altered stream type for the next part of the stream.
   *
   * @param streamPartCtx     The part of the stream being processed (i.e. the map, sort etc.).
   * @param currentStreamType The current type that is flowing through this part of the stream.
   * @return The possibly new current type that is going to be flowing out of this operation.
   */
  private ISymbol evaluateStreamType(final EK9Parser.StreamPartContext streamPartCtx,
                                     final ISymbol currentStreamType) {

    final var operation = streamFunctionMap.getOrDefault(streamPartCtx.op.getType(), this::invalidStreamOperation);

    return operation.apply(streamPartCtx, currentStreamType);
  }

  private ISymbol invalidStreamOperation(final EK9Parser.StreamPartContext streamPartCtx,
                                         final ISymbol currentStreamType) {

    throw new CompilerException("Stream part [" + streamPartCtx.op.getText() + "] not implemented");
  }

  /**
   * This check is for skip, head and tail.
   * No argument means default to 1, or a positive integer literal, or a function that returns an integer.
   * Will be a run time check if that value is zero or negative.
   */
  private ISymbol integerOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                 final ISymbol currentStreamType) {

    headTailSkipOrError.accept(streamPartCtx);
    //Always the same type.
    return currentStreamType;

  }

  private ISymbol groupOrError(final EK9Parser.StreamPartContext streamPartCtx,
                               final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().size() == 1) {
      final var functionReturnType = functionOrError(streamPartCtx.pipelinePart(0), currentStreamType);
      //Now it is essential that the return type has hashcode.
      hasHashCodeOrError(streamPartCtx, functionReturnType);
    } else {
      hasHashCodeOrError(streamPartCtx, currentStreamType);
    }

    //But this will return a type of 'List of symbolType'. So that needs to be resolved.
    return parameterisedListTypeOrError(streamPartCtx.op, currentStreamType);
  }

  private ISymbol splitOrError(final EK9Parser.StreamPartContext streamPartCtx,
                               final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().size() == 1) {
      functionAndReturnsBooleanOrError(streamPartCtx.pipelinePart(0), currentStreamType);
    } else {
      errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);
    }

    //But this will return a type of 'List of symbolType'. So that needs to be resolved.
    return parameterisedListTypeOrError(streamPartCtx.op, currentStreamType);
  }

  private ISymbol teeOrError(final EK9Parser.StreamPartContext streamPartCtx,
                             final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);
    } else if (streamPartCtx.pipelinePart().size() == 1) {
      final var terminalPipeLinePartCtx = streamPartCtx.pipelinePart(0);
      teeTerminationOrError(streamPartCtx, terminalPipeLinePartCtx, currentStreamType);
    } else {
      //Else there will be two parts. The first part is a mapping, the second the terminal.
      final var mapPipeLinePartCtx = streamPartCtx.pipelinePart(0);
      final var terminalPipeLinePartCtx = streamPartCtx.pipelinePart(1);
      //This will be the type that the terminal part will need to use.
      final var teeStreamType = functionOrError(mapPipeLinePartCtx, currentStreamType);

      teeTerminationOrError(streamPartCtx, terminalPipeLinePartCtx, teeStreamType);

    }

    //Does not alter the current stream type flowing through the pipeline.
    return currentStreamType;
  }

  private void teeTerminationOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                     final EK9Parser.PipelinePartContext terminalPipeLinePartCtx,
                                     final ISymbol streamSymbolType) {

    final var terminalSymbol = getRecordedAndTypedSymbol(terminalPipeLinePartCtx);
    final var terminalStreamCallSymbol = (StreamCallSymbol) symbolsAndScopes.getRecordedSymbol(streamPartCtx);

    terminalSymbol.getType().ifPresent(
        terminalSymbolType -> terminationOrError(terminalStreamCallSymbol, terminalSymbolType, streamSymbolType));
  }

  private ISymbol functionOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                  final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().size() == 1) {
      return functionOrError(streamPartCtx.pipelinePart(0), currentStreamType);
    }

    errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);

    return symbolsAndScopes.getEk9Types().ek9Void();
  }

  private ISymbol functionOrError(final EK9Parser.PipelinePartContext partCtx,
                                  final ISymbol currentStreamType) {

    final var possibleFunction = streamFunctionOrError.apply(partCtx);

    if (possibleFunction.isEmpty()) {
      return symbolsAndScopes.getEk9Types().ek9Void();
    }

    final var function = possibleFunction.get();
    function.getReturningSymbol().getType().ifPresent(returnType -> {

      if (returnType.isExactSameType(symbolsAndScopes.getEk9Types().ek9Void())) {
        errorListener.semanticError(partCtx.start, "", RETURNING_MISSING);
      }
    });

    return acceptsTypeOrError(new StreamFunctionCheckData(partCtx.start, function, currentStreamType));
  }

  private ISymbol selectFunctionOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                        final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().size() == 1) {
      return functionAndReturnsBooleanOrError(streamPartCtx.pipelinePart(0), currentStreamType);
    }

    errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);

    return symbolsAndScopes.getEk9Types().ek9Void();
  }

  private ISymbol functionAndReturnsBooleanOrError(final EK9Parser.PipelinePartContext partCtx,
                                                   final ISymbol currentStreamType) {

    final var functionReturnType = functionOrError(partCtx, currentStreamType);

    if (!symbolsAndScopes.getEk9Types().ek9Boolean().isExactSameType(functionReturnType)) {
      errorListener.semanticError(partCtx.start, "", MUST_RETURN_BOOLEAN);
    }

    //Does not alter the current stream type flowing through the pipeline.
    return currentStreamType;
  }

  private ISymbol callFunctionOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                      final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      if (currentStreamType instanceof FunctionSymbol functionSymbol) {
        return acceptsNoArgumentsDoesNotReturnVoidOrError(streamPartCtx.op, functionSymbol);
      } else {
        final var msg = "expecting a function not type '" + currentStreamType.getFriendlyName() + "':";
        errorListener.semanticError(streamPartCtx.op, msg, TYPE_MUST_BE_FUNCTION);
      }
    } else {
      errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_NOT_REQUIRED);
    }

    return symbolsAndScopes.getEk9Types().ek9Void();
  }

  private ISymbol sortOrError(final EK9Parser.StreamPartContext streamPartCtx,
                              final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      typeAsSuitableComparatorOrError(streamPartCtx, currentStreamType);
    } else if (streamPartCtx.pipelinePart().size() == 1) {
      sortFunctionOrError(streamPartCtx.pipelinePart().get(0), currentStreamType);
    }

    //No matter what 'sort' not alter the stream type being used, so return the same type.
    return currentStreamType;
  }

  private ISymbol uniqOrError(final EK9Parser.StreamPartContext streamPartCtx,
                              final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      hasHashCodeOrError(streamPartCtx, currentStreamType);
    } else if (streamPartCtx.pipelinePart().size() == 1) {
      hashCodeFunctionOrError(streamPartCtx.pipelinePart().get(0), currentStreamType);
    }

    //No matter what 'uniq' not alter the stream type being used, so return the same type.
    return currentStreamType;
  }

  private ISymbol joinOrError(final EK9Parser.StreamPartContext streamPartCtx,
                              final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);
    } else if (streamPartCtx.pipelinePart().size() == 1) {
      joinFunctionOrError(streamPartCtx.pipelinePart().get(0), currentStreamType);
    }

    //No matter what 'join' not alter the stream type being used, so return the same type.
    return currentStreamType;
  }

  private ISymbol flattenOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                 final ISymbol currentStreamType) {

    if (streamPartCtx.pipelinePart().isEmpty()) {
      return accessAggregateAsTypeOrError(streamPartCtx, currentStreamType)
          .map(aggregate -> new StreamAggregateCheckData(streamPartCtx.op, aggregate, currentStreamType))
          .map(this::getIteratorTypeOrError)
          .orElseGet(() -> symbolsAndScopes.getEk9Types().ek9Void());
    }

    errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_NOT_REQUIRED);

    return symbolsAndScopes.getEk9Types().ek9Void();
  }

  private ISymbol getIteratorTypeOrError(final StreamAggregateCheckData streamAggregateCheckData) {

    final var iteratorType = getIteratorType.apply(streamAggregateCheckData.aggregateSymbol());
    if (iteratorType.isPresent()) {
      return iteratorType.get();
    }

    errorListener.semanticError(streamAggregateCheckData.errorLocation(), "", MISSING_ITERATE_METHOD);

    return symbolsAndScopes.getEk9Types().ek9Void();
  }

  private void typeAsSuitableComparatorOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                               final ISymbol currentStreamType) {

    accessAggregateAsTypeOrError(streamPartCtx, currentStreamType)
        .map(aggregate -> new StreamAggregateCheckData(streamPartCtx.op, aggregate, currentStreamType))
        .ifPresent(this::comparatorPresentOrError);

  }

  private void hasHashCodeOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                  final ISymbol symbolType) {

    accessAggregateAsTypeOrError(streamPartCtx, symbolType)
        .map(aggregate -> new StreamAggregateCheckData(streamPartCtx.op, aggregate, symbolType))
        .ifPresent(this::hashCodePresentOrError);

  }

  private void hashCodeFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                       final ISymbol currentStreamType) {

    streamFunctionOrError.apply(partCtx)
        .map(function -> new StreamFunctionCheckData(partCtx.start, function, currentStreamType))
        .ifPresent(this::hashCodeFunctionOrError);

  }

  private void hashCodeFunctionOrError(final StreamFunctionCheckData functionData) {

    final var errorMsg = "'" + functionData.functionSymbol().getFriendlyName() + "':";

    if (functionData.functionSymbol().getCallParameters().size() != 1) {
      errorListener.semanticError(functionData.errorLocation(), errorMsg, FUNCTION_MUST_HAVE_SINGLE_PARAMETER);
    } else {
      streamFunctionArgumentsOrError.accept(functionData);
    }

    if (functionData.functionSymbol().getReturningSymbol().getType().isPresent()) {
      final var returnType = functionData.functionSymbol().getReturningSymbol().getType().get();

      if (returnType instanceof IAggregateSymbol) {
        hashCodePresentOrError.test(new Ek9Token(functionData.errorLocation()), returnType);
      } else {
        final var msg = "need 'aggregate' type, but '" + returnType.getFriendlyName() + "':";
        errorListener.semanticError(functionData.errorLocation(), msg, IS_NOT_AN_AGGREGATE_TYPE);
      }

    }

  }


  private void joinFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                   final ISymbol currentStreamType) {

    //For this mechanism to work the identifier reference used as the function must have a type of function
    //it must also accept specific arguments and return a type that is the same as the arguments.
    streamFunctionOrError.apply(partCtx)
        .map(function -> new StreamFunctionCheckData(partCtx.start, function, currentStreamType))
        .ifPresent(this::joinFunctionOrError);

  }

  private void joinFunctionOrError(final StreamFunctionCheckData functionData) {

    //For this to be valid the function must take two arguments compatible with symbolType
    //And it must return a variable that is the same type as the symbolType (or compatible with it.)
    final var errorMsg = "'" + functionData.functionSymbol().getFriendlyName() + "':";
    if (functionData.functionSymbol().getCallParameters().size() != 2) {
      errorListener.semanticError(functionData.errorLocation(), errorMsg, FUNCTION_MUST_HAVE_TWO_PARAMETERS);
    } else {
      streamFunctionArgumentsOrError.accept(functionData);
    }

    if (functionData.functionSymbol().getReturningSymbol().getType().isPresent()) {
      final var returnType = functionData.functionSymbol().getReturningSymbol().getType().get();

      if (returnType.isExactSameType(symbolsAndScopes.getEk9Types().ek9Void())) {
        errorListener.semanticError(functionData.errorLocation(), errorMsg, RETURNING_MISSING);
      } else if (!returnType.isAssignableTo(functionData.currentStreamType())) {
        var typeErrorMsg = "wrt '" + functionData.functionSymbol().getFriendlyName()
            + "' and pipeline type '" + functionData.currentStreamType().getFriendlyName() + "':";
        errorListener.semanticError(functionData.errorLocation(), typeErrorMsg, INCOMPATIBLE_TYPES);
      }

    }

  }

  private void sortFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                   final ISymbol currentStreamType) {

    //For this mechanism to work the identifier reference used as the function must have a type of function
    //it must also accept specific arguments and return an Integer (i.e. must be a comparator function).
    streamFunctionOrError.apply(partCtx)
        .map(function -> new StreamFunctionCheckData(partCtx.start, function, currentStreamType))
        .ifPresent(this::comparatorFunctionOrError);
  }

  private Optional<IAggregateSymbol> accessAggregateAsTypeOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                                                  final ISymbol symbolType) {

    if (symbolType instanceof IAggregateSymbol aggregateSymbol) {
      return Optional.of(aggregateSymbol);
    }

    final var msg = "need 'aggregate' type, but '" + symbolType.getFriendlyName() + "':";
    errorListener.semanticError(streamPartCtx.op, msg, IS_NOT_AN_AGGREGATE_TYPE);

    return Optional.empty();
  }


  private ISymbol acceptsNoArgumentsDoesNotReturnVoidOrError(final Token errorLocation,
                                                             final FunctionSymbol functionSymbol) {

    var errorMsg = "'" + functionSymbol.getFriendlyName() + "':";
    if (!functionSymbol.getCallParameters().isEmpty()) {
      errorListener.semanticError(errorLocation, errorMsg, REQUIRE_NO_ARGUMENTS);
    } else if (functionSymbol.getReturningSymbol().getType().isPresent()) {
      final var returnType = functionSymbol.getReturningSymbol().getType().get();
      if (returnType.isExactSameType(symbolsAndScopes.getEk9Types().ek9Void())) {
        errorListener.semanticError(errorLocation, errorMsg, FUNCTION_MUST_RETURN_VALUE);
      }

      return returnType;
    }

    return symbolsAndScopes.getEk9Types().ek9Void();
  }

  private void hashCodePresentOrError(final StreamAggregateCheckData streamAggregateCheckData) {

    hashCodePresentOrError.test(new Ek9Token(streamAggregateCheckData.errorLocation()),
        streamAggregateCheckData.symbolType());
  }

  private void comparatorPresentOrError(final StreamAggregateCheckData streamAggregateCheckData) {

    //Need to search on the aggregateSymbol for a comparator '<=>' operator with compatible type arguments.
    //Accepts a single argument compatible with the symbolType

    comparatorPresentOrError.test(new Ek9Token(streamAggregateCheckData.errorLocation()),
        streamAggregateCheckData.symbolType());

  }

  private void comparatorFunctionOrError(final StreamFunctionCheckData functionData) {

    //For this to be valid the function must take two arguments compatible with symbolType
    //And it must return an Integer.
    final var errorMsg = "'" + functionData.functionSymbol().getFriendlyName() + "':";
    if (functionData.functionSymbol().getCallParameters().size() != 2) {
      errorListener.semanticError(functionData.errorLocation(), errorMsg, FUNCTION_MUST_HAVE_TWO_PARAMETERS);
    } else {
      streamFunctionArgumentsOrError.accept(functionData);
    }

    if (functionData.functionSymbol().getReturningSymbol().getType().isPresent()) {
      var returnType = functionData.functionSymbol().getReturningSymbol().getType().get();
      if (!returnType.isExactSameType(symbolsAndScopes.getEk9Types().ek9Integer())) {
        errorListener.semanticError(functionData.errorLocation(), errorMsg, MUST_RETURN_INTEGER);
      }
    }

  }


  private ISymbol parameterisedListTypeOrError(final Token opLocation, final ISymbol currentStreamType) {

    //Access the generic List type - this has been pre-located for quicker use.
    final var listType = symbolsAndScopes.getEk9Types().ek9List();

    //Now get the parameterised type.
    final var typeData = new ParameterisedTypeData(new Ek9Token(opLocation), listType, List.of(currentStreamType));
    final var resolvedNewType = parameterisedLocator.resolveOrDefine(typeData);

    return resolvedNewType.orElseGet(() -> symbolsAndScopes.getEk9Types().ek9Void());
  }

  private ISymbol acceptsTypeOrError(final StreamFunctionCheckData functionData) {

    if (functionData.functionSymbol().getCallParameters().size() == 1) {
      //OK so now check if the type that we have here could be received.
      final var paramType = functionData.functionSymbol().getCallParameters().get(0).getType();

      if (functionData.currentStreamType().isAssignableTo(paramType)) {
        if (functionData.functionSymbol().getReturningSymbol().getType().isPresent()) {
          return functionData.functionSymbol().getReturningSymbol().getType().get();
        }
      } else {
        final var msg = "'" + functionData.functionSymbol().getFriendlyName() + "' incompatible with argument of '"
            + functionData.currentStreamType().getFriendlyName() + "':";
        errorListener.semanticError(functionData.errorLocation(), msg, INCOMPATIBLE_TYPES);
      }
    } else {
      final var msg = "'" + functionData.functionSymbol().getFriendlyName() + "':";
      errorListener.semanticError(functionData.errorLocation(), msg, REQUIRE_ONE_ARGUMENT);
    }

    return symbolsAndScopes.getEk9Types().ek9Void();
  }

  private void streamTerminationOrError(final StreamAssemblyData streamAssembly, final ISymbol streamType) {

    streamAssembly.termination().getType().ifPresent(
        terminationType -> terminationOrError(streamAssembly.termination(), terminationType, streamType));

  }

  private void terminationOrError(final StreamCallSymbol termination,
                                  final ISymbol terminationType,
                                  final ISymbol streamType) {

    if (terminationType instanceof IAggregateSymbol terminationTypeAggregate) {
      final var search = new MethodSymbolSearch("|").addTypeParameter(streamType);
      final var result = terminationTypeAggregate.resolveMatchingMethods(search, new MethodSymbolSearchResult());

      if (result.isSingleBestMatchPresent()) {

        result.getSingleBestMatchSymbol().ifPresent(bestTypeMatch -> {
          //Note that we set the consuming type, but it may be that a promotion of that type is needed.
          //i.e. Character to String promotion, but the type itself has the definition of what it can be promoted to.
          termination.setConsumesSymbolType(streamType);
          termination.setConsumesSymbolPromotionRequired(!bestTypeMatch.isExactSameType(streamType));
        });

      } else {
        final var msg = "wrt pipeline type '" + streamType.getFriendlyName()
            + "' and terminal type '" + terminationType.getFriendlyName() + "':";
        errorListener.semanticError(termination.getSourceToken(), msg, UNABLE_TO_FIND_PIPE_FOR_TYPE);
      }

    } else {
      final var msg = "termination/tee requires an aggregate type, '" + terminationType.getFriendlyName() + ":";
      errorListener.semanticError(termination.getSourceToken(), msg, IS_NOT_AN_AGGREGATE_TYPE);
    }
  }

}
