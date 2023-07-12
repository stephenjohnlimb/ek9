package org.ek9lang.compiler.main.rules;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Used for checking covariance based on a variable.
 * Navigate the variable to get the type, then check the type.
 * Clearly it is also possible that either the from or to could be missing or even their types could be missing.
 */
public record CovarianceCheckData(Token token, String errorMessage, ISymbol fromVar, ISymbol toVar) {
}
