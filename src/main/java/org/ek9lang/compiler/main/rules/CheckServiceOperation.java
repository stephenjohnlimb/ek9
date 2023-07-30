package org.ek9lang.compiler.main.rules;

import static org.ek9lang.compiler.symbols.support.SymbolFactory.HTTP_ACCESS;
import static org.ek9lang.compiler.symbols.support.SymbolFactory.HTTP_PATH;
import static org.ek9lang.compiler.symbols.support.SymbolFactory.HTTP_REQUEST;
import static org.ek9lang.compiler.symbols.support.SymbolFactory.HTTP_SOURCE;
import static org.ek9lang.compiler.symbols.support.SymbolFactory.HTTP_VERB;
import static org.ek9lang.compiler.symbols.support.SymbolFactory.URI_PROTO;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.ServiceOperationSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Check that a service operation complies with ek9 rules or return types etc.
 * Also incoming parameters are limited and therefore checked in here.
 */
public class CheckServiceOperation extends RuleSupport
    implements BiConsumer<ServiceOperationSymbol, EK9Parser.ServiceOperationDeclarationContext> {

  private final CheckForBody checkForBody = new CheckForBody();
  private final CheckPathParameter checkPathParameter;
  private final CheckHttpAccess checkHttpAccess;

  /**
   * Create a new Check Service Operation function.
   */
  public CheckServiceOperation(final SymbolAndScopeManagement symbolAndScopeManagement,
                               final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkPathParameter = new CheckPathParameter(symbolAndScopeManagement, errorListener);
    this.checkHttpAccess = new CheckHttpAccess(symbolAndScopeManagement, errorListener);
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

    var uriProto = operation.getSquirrelledData(URI_PROTO);
    var expectedNumberPathParameters = uriProto.chars().filter(ch -> ch == '{').count();
    long actualNumberOfPathParameters = 0L;

    for (var param : operation.getCallParameters()) {
      defaultPathParameterIfNecessary(param);

      if (checkPathParameter.test(operation, param)) {
        actualNumberOfPathParameters++;
      }

      checkServiceOperationType(operation, param);

      checkHttpAccess.accept(param);
    }
    testPathParameterCount(operation, expectedNumberPathParameters, actualNumberOfPathParameters);
  }

  private void defaultPathParameterIfNecessary(final ISymbol param) {
    //If not set assume PATH and the source as the variable name. Later processing will check
    //PATH exists on the operation. So this is an assumption - that is later checked.
    //Because even if ek9 developer declared the PATH and the name - we still have to check it.
    if (param.getSquirrelledData(HTTP_ACCESS) == null) {
      param.putSquirrelledData(HTTP_ACCESS, HTTP_PATH);
    }
    if (HTTP_PATH.equals(param.getSquirrelledData(HTTP_ACCESS)) && param.getSquirrelledData(HTTP_SOURCE) == null) {
      param.putSquirrelledData(HTTP_SOURCE, param.getName());
    }
  }

  private void checkServiceOperationType(final ServiceOperationSymbol operation, final ISymbol param) {

    param.getType().ifPresent(paramType -> {

      if (!isParameterTypeSupported(paramType)) {
        errorListener.semanticError(param.getSourceToken(), "'" + paramType.getFriendlyName() + "':",
            ErrorListener.SemanticClassification.SERVICE_INCOMPATIBLE_PARAM_TYPE);
      }

      if (param.getSquirrelledData(HTTP_ACCESS).equals(HTTP_REQUEST)) {

        if (operation.getCallParameters().size() > 1) {
          errorListener.semanticError(param.getSourceToken(), "'" + param + "':",
              ErrorListener.SemanticClassification.SERVICE_REQUEST_BY_ITSELF);
        }

        //Must be HTTPRequest
        if (!paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9HttpRequest())) {
          errorListener.semanticError(param.getSourceToken(), "'" + paramType.getFriendlyName() + "':",
              ErrorListener.SemanticClassification.SERVICE_INCOMPATIBLE_PARAM_TYPE_REQUEST);
        }
      } else {
        //Cannot be HTTPRequest
        if (paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9HttpRequest())) {
          errorListener.semanticError(param.getSourceToken(), "'" + paramType.getFriendlyName() + "':",
              ErrorListener.SemanticClassification.SERVICE_INCOMPATIBLE_PARAM_TYPE_NON_REQUEST);
        }
      }
    });
  }

  private boolean isParameterTypeSupported(final ISymbol paramType) {
    return paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9HttpRequest())
        || paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9Integer())
        || paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9String())
        || paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9Date())
        || paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9Time())
        || paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9DateTime())
        || paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9Millisecond())
        || paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9Duration());
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
        if (!returnType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9HttpResponse())) {
          errorListener.semanticError(operation.getSourceToken(), "",
              ErrorListener.SemanticClassification.SERVICE_INCOMPATIBLE_RETURN_TYPE);
        }
      });
    }
  }

  private void testServiceBody(final ServiceOperationSymbol operation,
                               final EK9Parser.ServiceOperationDeclarationContext ctx) {
    if (!checkForBody.test(ctx.operationDetails())) {
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
