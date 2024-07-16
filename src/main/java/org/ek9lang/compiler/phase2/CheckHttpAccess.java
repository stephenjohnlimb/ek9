package org.ek9lang.compiler.phase2;

import static org.ek9lang.compiler.support.SymbolFactory.HTTP_ACCESS;
import static org.ek9lang.compiler.support.SymbolFactory.HTTP_HEADER;
import static org.ek9lang.compiler.support.SymbolFactory.HTTP_PATH;
import static org.ek9lang.compiler.support.SymbolFactory.HTTP_QUERY;
import static org.ek9lang.compiler.support.SymbolFactory.HTTP_SOURCE;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks HTTP Access for service operations because some require HTTP_SOURCE, but others do not support it.
 */
final class CheckHttpAccess extends RuleSupport implements Consumer<ISymbol> {
  CheckHttpAccess(final SymbolsAndScopes symbolsAndScopes,
                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final ISymbol param) {
    //When it is a path variable, must check that the path contains the variable.
    final var access = param.getSquirrelledData(HTTP_ACCESS);
    final var sourceName = param.getSquirrelledData(HTTP_SOURCE);

    if ((HTTP_PATH.equals(access) || HTTP_QUERY.equals(access) || HTTP_HEADER.equals(access))) {
      if (sourceName == null) {
        //These need a qualifier, so we know when path/query/header value to extract and use
        final var msg = "'" + access + "' requires additional qualifier name:";
        errorListener.semanticError(param.getSourceToken(), msg,
            ErrorListener.SemanticClassification.SERVICE_HTTP_PARAM_NEEDS_QUALIFIER);
      }
    } else {
      //The other types of access do not and cannot be further qualified
      if (sourceName != null) {
        final var msg = "'" + access + "' does not require qualifier '" + sourceName + "':";
        errorListener.semanticError(param.getSourceToken(), msg,
            ErrorListener.SemanticClassification.SERVICE_HTTP_PARAM_QUALIFIER_NOT_ALLOWED);
      }
    }

  }
}
