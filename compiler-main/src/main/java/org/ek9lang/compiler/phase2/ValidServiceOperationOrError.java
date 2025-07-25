package org.ek9lang.compiler.phase2;

import static org.ek9lang.compiler.support.CommonValues.HTTP_ACCESS;
import static org.ek9lang.compiler.support.CommonValues.HTTP_PATH;
import static org.ek9lang.compiler.support.CommonValues.HTTP_REQUEST;
import static org.ek9lang.compiler.support.CommonValues.HTTP_SOURCE;
import static org.ek9lang.compiler.support.CommonValues.HTTP_VERB;
import static org.ek9lang.compiler.support.CommonValues.URI_PROTO;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ExternallyImplemented;
import org.ek9lang.compiler.common.ProcessingBodyPresent;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.ServiceOperationSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Check that a service operation complies with EK9 rules and return types etc.
 * Also, incoming parameters are limited in type and therefore checked in here.
 */
final class ValidServiceOperationOrError extends RuleSupport
    implements BiConsumer<ServiceOperationSymbol, EK9Parser.ServiceOperationDeclarationContext> {

  private final ExternallyImplemented externallyImplemented = new ExternallyImplemented();
  private final ProcessingBodyPresent processingBodyPresent = new ProcessingBodyPresent();
  private final ValidPathParameterOrError validPathParameterOrError;
  private final HttpAccessOrError httpAccessOrError;

  /**
   * Create a new Check Service Operation function.
   */
  ValidServiceOperationOrError(final SymbolsAndScopes symbolsAndScopes,
                               final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
    this.validPathParameterOrError = new ValidPathParameterOrError(symbolsAndScopes, errorListener);
    this.httpAccessOrError = new HttpAccessOrError(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final ServiceOperationSymbol operation, final EK9Parser.ServiceOperationDeclarationContext ctx) {

    testArgumentTypes(operation);
    testReturnTypePresence(operation);
    testReturnTypeCompatibility(operation);
    testServiceBody(operation, ctx);

  }

  private void testArgumentTypes(final ServiceOperationSymbol operation) {

    AssertValue.checkNotNull("Expecting httpVerb to have been defined for " + operation.getFriendlyName(),
        operation.getSquirrelledData(HTTP_VERB));

    final var uriProto = operation.getSquirrelledData(URI_PROTO);
    final var expectedNumberPathParameters = uriProto.chars().filter(ch -> ch == '{').count();
    long actualNumberOfPathParameters = 0L;

    for (var param : operation.getCallParameters()) {
      defaultPathParameterIfNecessary(param);

      if (validPathParameterOrError.test(operation, param)) {
        actualNumberOfPathParameters++;
      }

      checkServiceOperationType(operation, param);
      httpAccessOrError.accept(param);
    }
    testPathParameterCount(operation, expectedNumberPathParameters, actualNumberOfPathParameters);

  }

  private void defaultPathParameterIfNecessary(final ISymbol param) {

    //If not set assume PATH and the source as the variable name. Later processing will check
    //PATH exists on the operation. So this is an assumption - that is later checked.
    //Because even if ek9 developer declared the PATH and the name - we still have to check it.
    if (param.getSquirrelledData(HTTP_ACCESS) == null) {
      param.putSquirrelledData(HTTP_ACCESS, HTTP_PATH.toString());
    }

    if (param.getSquirrelledData(HTTP_ACCESS).equals(HTTP_PATH.toString())
        && param.getSquirrelledData(HTTP_SOURCE) == null) {
      param.putSquirrelledData(HTTP_SOURCE, param.getName());
    }

  }

  private void checkServiceOperationType(final ServiceOperationSymbol operation, final ISymbol param) {

    param.getType().ifPresent(paramType -> {

      if (!isParameterTypeSupported(paramType)) {
        errorListener.semanticError(param.getSourceToken(), "'" + paramType.getFriendlyName() + "':",
            ErrorListener.SemanticClassification.SERVICE_INCOMPATIBLE_PARAM_TYPE);
      }

      if (param.getSquirrelledData(HTTP_ACCESS).equals(HTTP_REQUEST.toString())) {

        if (operation.getCallParameters().size() > 1) {
          errorListener.semanticError(param.getSourceToken(), "'" + param + "':",
              ErrorListener.SemanticClassification.SERVICE_REQUEST_BY_ITSELF);
        }

        //Must be HTTPRequest
        if (!paramType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9HttpRequest())) {
          errorListener.semanticError(param.getSourceToken(), "'" + paramType.getFriendlyName() + "':",
              ErrorListener.SemanticClassification.SERVICE_INCOMPATIBLE_PARAM_TYPE_REQUEST);
        }
      } else {
        //Cannot be HTTPRequest
        if (paramType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9HttpRequest())) {
          errorListener.semanticError(param.getSourceToken(), "'" + paramType.getFriendlyName() + "':",
              ErrorListener.SemanticClassification.SERVICE_INCOMPATIBLE_PARAM_TYPE_NON_REQUEST);
        }
      }
    });

  }

  private boolean isParameterTypeSupported(final ISymbol paramType) {

    return paramType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9HttpRequest())
        || paramType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9Integer())
        || paramType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9String())
        || paramType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9Date())
        || paramType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9Time())
        || paramType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9DateTime())
        || paramType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9Millisecond())
        || paramType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9Duration());

  }

  private void testReturnTypePresence(final ServiceOperationSymbol operation) {

    if (!operation.isReturningSymbolPresent()) {
      //Well that's an error, service operation must always return a HTTPResponse
      errorListener.semanticError(operation.getSourceToken(), "",
          ErrorListener.SemanticClassification.SERVICE_MISSING_RETURN);
    }

  }

  private void testReturnTypeCompatibility(final ServiceOperationSymbol operation) {

    if (operation.isReturningSymbolPresent()) {
      operation.getReturningSymbol().getType().ifPresent(returnType -> {
        if (!returnType.isAssignableTo(symbolsAndScopes.getEk9Types().ek9HttpResponse())) {
          errorListener.semanticError(operation.getSourceToken(), "",
              ErrorListener.SemanticClassification.SERVICE_INCOMPATIBLE_RETURN_TYPE);
        }
      });
    }

  }

  private void testServiceBody(final ServiceOperationSymbol operation,
                               final EK9Parser.ServiceOperationDeclarationContext ctx) {

    //No requirement for body if the module is extern - i.e. the ek9 code is just a signature.
    if (!externallyImplemented.test(operation) && !processingBodyPresent.test(ctx.operationDetails())) {
      errorListener.semanticError(operation.getSourceToken(), "",
          ErrorListener.SemanticClassification.SERVICE_WITH_NO_BODY_PROVIDED);
    }

  }

  private void testPathParameterCount(final ServiceOperationSymbol operation,
                                      final long expectedNumber,
                                      final long actualNumber) {

    if (expectedNumber != actualNumber) {
      errorListener.semanticError(operation.getSourceToken(), "",
          ErrorListener.SemanticClassification.SERVICE_HTTP_PATH_PARAM_COUNT_INVALID);
    }

  }
}
