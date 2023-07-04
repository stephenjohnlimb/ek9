package org.ek9lang.compiler.main.rules;

import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTP_ACCESS;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTP_PATH;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTP_SOURCE;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.URI_PROTO;

import java.util.function.BiPredicate;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ServiceOperationSymbol;

/**
 * Checks the PATH value (if this is a path parameter) and ensures that the name appears in the path itself
 * on the operation.
 */
public class CheckPathParameter extends RuleSupport implements BiPredicate<ServiceOperationSymbol, ISymbol> {
  public CheckPathParameter(final SymbolAndScopeManagement symbolAndScopeManagement,
                            final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public boolean test(final ServiceOperationSymbol operation, final ISymbol param) {
    //When it is a path variable, must check that the path contains the variable.
    if (param.getSquirrelledData(HTTP_ACCESS).equals(HTTP_PATH)) {
      var sourceName = param.getSquirrelledData(HTTP_SOURCE);
      var path = operation.getSquirrelledData(URI_PROTO);
      var pathVar = String.format("{%s}", sourceName);
      if (!path.contains(pathVar)) {
        var msg = "'" + pathVar + "' is not in '" + path + ':';
        errorListener.semanticError(param.getSourceToken(), msg,
            ErrorListener.SemanticClassification.SERVICE_HTTP_PATH_PARAM_INVALID);
      }
      return true;
    }
    return false;
  }
}
