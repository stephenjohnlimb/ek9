package org.ek9lang.compiler.main.rules;

import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTPACCESS;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTPPATH;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTPREQUEST;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTPSOURCE;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTPVERB;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ServiceOperationSymbol;
import org.ek9lang.core.exception.AssertValue;

/**
 * Check that a service operation complies with ek9 rules or return types etc.
 * Also incoming parameters are limited and therefore checked in here.
 */
public class CheckServiceOperation extends RuleSupport
    implements BiConsumer<ServiceOperationSymbol, EK9Parser.ServiceOperationDeclarationContext> {

  private final CheckForBody checkForBody = new CheckForBody();

  public CheckServiceOperation(final SymbolAndScopeManagement symbolAndScopeManagement,
                               final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
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
        operation.getSquirrelledData(HTTPVERB));

    operation.getMethodParameters().forEach(param -> {
      //If not set assume PATH and the source as the variable name. Later processing will check
      //PATH exists on the operation. So this is an assumption - that is later checked.
      //Because even if ek9 developer declared the PATH and the name - we still have to check it.
      if (param.getSquirrelledData(HTTPACCESS) == null) {
        param.putSquirrelledData(HTTPACCESS, HTTPPATH);
      }
      if (param.getSquirrelledData(HTTPSOURCE) == null) {
        param.putSquirrelledData(HTTPSOURCE, param.getName());
      }

      checkServiceOperationType(operation, param);
    });
  }

  private void checkServiceOperationType(ServiceOperationSymbol operation, ISymbol param) {
    param.getType().ifPresent(paramType -> {
      //Now ensure is of a suitable type - might support more in future like Date etc

      if (!paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9HttpRequest())
          && !paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9Integer())
          && !paramType.isAssignableTo(symbolAndScopeManagement.getEk9Types().ek9String())) {
        errorListener.semanticError(param.getSourceToken(), "'" + paramType.getFriendlyName() + "':",
            ErrorListener.SemanticClassification.SERVICE_INCOMPATIBLE_PARAM_TYPE);
      }
      if (param.getSquirrelledData(HTTPACCESS).equals(HTTPREQUEST)) {
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
}
