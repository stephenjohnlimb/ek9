package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.UNSAFE_METHOD_ACCESS;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * In the case of specific variables of type like 'Result' and 'Optional' it is important to check
 * if the method being called needs a previous call to isOk or isError or just '?' before accessing
 * specific methods.
 * This is a bit gnarly - because the method chaining calls are recursive by nature and also this
 * needs to check that any returning type might be a Result or Optional and then also test if the methods
 * that are being called are ones that need safe access calls before being called.
 * i.e. Result.ok(), Result.error(), Optional.get(). Other methods can just be called but those require a
 * safety check call of isOk()/'?', isError() or '?' (for Optional).
 */
final class ObjectAccessExpressionValidOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.ObjectAccessExpressionContext> {

  private final SafeGenericAccessMarker safeGenericAccessMarker;

  //Used to map a specific method name to a squirrelled data element.
  //If present then that method call has to be preceded by a check call so as not to trigger
  //a possible exception. We emit compiler errors to try and prevent this occurring.
  private final Map<String, CommonValues> methodNameLookup =
      Map.of("ok", CommonValues.RESULT_OK_ACCESS_REQUIRES_SAFE_ACCESS,
          "error", CommonValues.RESULT_ERROR_ACCESS_REQUIRES_SAFE_ACCESS,
          "get", CommonValues.OPTIONAL_GET_ACCESS_REQUIRES_SAFE_ACCESS);

  private final Map<CommonValues, Predicate<ISymbol>> methodLookup;

  /**
   * Constructor to provided typed access.
   */
  ObjectAccessExpressionValidOrError(final SymbolsAndScopes symbolsAndScopes,
                                     final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
    this.safeGenericAccessMarker = new SafeGenericAccessMarker(symbolsAndScopes);

    //Now create the map of methods to call for specific checks.
    methodLookup = Map.of(CommonValues.RESULT_OK_ACCESS_REQUIRES_SAFE_ACCESS, symbolsAndScopes::isOkResultAccessSafe,
        CommonValues.RESULT_ERROR_ACCESS_REQUIRES_SAFE_ACCESS, symbolsAndScopes::isErrorResultAccessSafe,
        CommonValues.OPTIONAL_GET_ACCESS_REQUIRES_SAFE_ACCESS, symbolsAndScopes::isGetOptionalAccessSafe);
  }

  @Override
  public void accept(final EK9Parser.ObjectAccessExpressionContext ctx) {
    final var objectAccessStartSymbol = getRecordedAndTypedSymbol(ctx.objectAccessStart());

    if (objectAccessStartSymbol != null) {
      safeGenericAccessMarker.accept(objectAccessStartSymbol);
      checkObjectAccessContext(objectAccessStartSymbol, ctx.objectAccess());
    }

  }

  /**
   * Note that object access is recursive.
   * This allows for arbitrary long method chains.
   * In this context I'm interested in whether a method that might trigger an exception
   * via Optional.get() or Result.ok() or Result.error() is called within a method chain.
   * <pre>
   * objectAccess
   *     : objectAccessType objectAccess?
   *     ;
   *
   * objectAccessType
   *     : DOT (identifier | operationCall)
   *     ;
   * </pre>
   */
  private void checkObjectAccessContext(final ISymbol calledFromSymbol, final EK9Parser.ObjectAccessContext ctx) {
    final var possibleCallSymbol = getRecordedAndTypedSymbol(ctx);
    //Should now be valid and typed by the time we get to this phase.
    if (possibleCallSymbol != null) {
      checkValidAccessOrError(calledFromSymbol, possibleCallSymbol);

      //Now the call might return a value of a type that we need to check so record it (if it does)
      safeGenericAccessMarker.accept(possibleCallSymbol);
      //Now make the recursive call. So this is the chaining and on we go until then end of the call chain.
      if (ctx.objectAccess() != null) {
        checkObjectAccessContext(possibleCallSymbol, ctx.objectAccess());
      }
    }
  }

  private void checkValidAccessOrError(final ISymbol calledFromSymbol, final ISymbol possibleCallSymbol) {
    if (possibleCallSymbol instanceof CallSymbol asCallSymbol) {
      final var resolvedToCall = asCallSymbol.getResolvedSymbolToCall();
      //At this point we know for sure that the method name is valid on the calledFromSymbol - because it has
      //been resolved. The question is, can it just be called without a check call before.
      final var methodName = resolvedToCall.getName();
      final var squirrelledLookupValue = methodNameLookup.get(methodName);
      if (squirrelledLookupValue != null && calledFromSymbol.getSquirrelledData(squirrelledLookupValue) != null) {
        final var toCall = methodLookup.get(squirrelledLookupValue);

        if (!toCall.test(calledFromSymbol)) {
          final var msg = "'" + calledFromSymbol.getFriendlyName() + "':";
          errorListener.semanticError(asCallSymbol.getSourceToken(), msg, UNSAFE_METHOD_ACCESS);
        }
      }
    }
  }

}
