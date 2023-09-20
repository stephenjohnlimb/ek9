package org.ek9lang.compiler.phase3;

import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Used for checking covariance based on a variable.
 * Navigate the variable to get the type, then check the type.
 * Clearly it is also possible that either the from or to could be missing or even their types could be missing.
 */
record CovarianceCheckData(IToken token, String errorMessage, ISymbol fromVar, ISymbol toVar) {
}
