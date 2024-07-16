package org.ek9lang.compiler.phase2;

import static org.ek9lang.compiler.support.SymbolFactory.HTTP_ACCESS;
import static org.ek9lang.compiler.support.SymbolFactory.HTTP_PATH;
import static org.ek9lang.compiler.support.SymbolFactory.HTTP_SOURCE;
import static org.ek9lang.compiler.support.SymbolFactory.URI_PROTO;

import java.util.function.BiPredicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.ServiceOperationSymbol;

/**
 * Checks the PATH value (if this is a path parameter) and ensures that the name appears in the path itself
 * on the operation.
 */
final class CheckPathParameter extends RuleSupport implements BiPredicate<ServiceOperationSymbol, ISymbol> {
  CheckPathParameter(final SymbolsAndScopes symbolsAndScopes,
                     final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  @Override
  public boolean test(final ServiceOperationSymbol operation, final ISymbol param) {

    //When it is a path variable, must check that the path contains the variable.
    if (param.getSquirrelledData(HTTP_ACCESS).equals(HTTP_PATH)) {
      final var sourceName = param.getSquirrelledData(HTTP_SOURCE);
      final var path = operation.getSquirrelledData(URI_PROTO);
      final var pathVar = String.format("{%s}", sourceName);
      if (!path.contains(pathVar)) {
        final var msg = "'" + pathVar + "' is not in '" + path + ':';
        errorListener.semanticError(param.getSourceToken(), msg,
            ErrorListener.SemanticClassification.SERVICE_HTTP_PATH_PARAM_INVALID);
      }
      return true;
    }

    return false;
  }
}
