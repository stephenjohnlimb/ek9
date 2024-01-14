package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CANNOT_CALL_ABSTRACT_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_MUST_RETURN_VALUE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_OR_DELEGATE_NOT_REQUIRED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_OR_DELEGATE_REQUIRED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.INCOMPATIBLE_TYPES;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MUST_RETURN_BOOLEAN;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.REQUIRE_NO_ARGUMENTS;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.REQUIRE_ONE_ARGUMENT;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TYPE_MUST_BE_FUNCTION;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.UNABLE_TO_FIND_PIPE_FOR_TYPE;

import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
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

  private ISymbol evaluateStreamType(final EK9Parser.StreamPartContext streamPartCtx,
                                     final ISymbol currentStreamType) {

    if (streamPartCtx.MAP() != null) {
      if (streamPartCtx.pipelinePart().size() == 1) {
        return checkViableMapFunctionOrError(streamPartCtx.pipelinePart(0), currentStreamType);
      }
      errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);
    } else if (streamPartCtx.FILTER() != null || streamPartCtx.SELECT() != null) {
      //Sugar for th same operation, basically needs a function that accepts the pipeline type and returns a boolean
      //In short a predicate.
      if (streamPartCtx.pipelinePart().size() == 1) {
        return checkViableSelectFunctionOrError(streamPartCtx.pipelinePart(0), currentStreamType);
      }
      errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_REQUIRED);
    } else if (streamPartCtx.CALL() != null || streamPartCtx.ASYNC() != null) {
      if (!streamPartCtx.pipelinePart().isEmpty()) {
        errorListener.semanticError(streamPartCtx.op, "", FUNCTION_OR_DELEGATE_NOT_REQUIRED);
      } else {
        return checkViableCallFunctionOrError(streamPartCtx, currentStreamType);
      }
      //This accepts a Function with no arguments and must return a value other than void.
    } else {
      throw new CompilerException("Stream part [" + streamPartCtx.getText() + "] not implemented");
    }
    return symbolAndScopeManagement.getEk9Types().ek9Void();
  }

  private ISymbol checkViableCallFunctionOrError(final EK9Parser.StreamPartContext streamPartCtx,
                                                 final ISymbol currentStreamType) {
    if (currentStreamType instanceof FunctionSymbol functionSymbol) {
      return acceptsNoArgumentsDoesNotReturnVoid(streamPartCtx.op, functionSymbol);
    } else {
      var msg = "expecting a function not type '" + currentStreamType.getFriendlyName() + "':";
      errorListener.semanticError(streamPartCtx.op, msg, TYPE_MUST_BE_FUNCTION);
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

    //But now for a select/filter, we need the function to return a boolean and only if that is true dow we allow
    //The existing object to flow down the pipeline, so this mean the returning type from this select/filter is
    //still the same type, i.e. the currentStreamType.
    return currentStreamType;

  }

  private ISymbol checkViableFunctionOrError(final EK9Parser.PipelinePartContext partCtx,
                                             final ISymbol currentStreamType) {

    var expectedMappingFunction = getRecordedAndTypedSymbol(partCtx);

    if (expectedMappingFunction != null && expectedMappingFunction.getType().isPresent()) {
      var expectedFunctionType = expectedMappingFunction.getType().get();

      if (!expectedMappingFunction.isMarkedAbstract()) {
        if (expectedFunctionType instanceof FunctionSymbol functionSymbol) {
          return acceptsTypeOrError(partCtx.start, functionSymbol, currentStreamType);
        } else {
          var msg = "type '" + expectedFunctionType.getFriendlyName() + "':";
          errorListener.semanticError(partCtx.start, msg, FUNCTION_OR_DELEGATE_REQUIRED);
        }
      } else {
        errorListener.semanticError(partCtx.start, "", CANNOT_CALL_ABSTRACT_TYPE);
      }
    }

    return symbolAndScopeManagement.getEk9Types().ek9Void();

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