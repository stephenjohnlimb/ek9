package org.ek9lang.compiler.phase3;

import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Useful in expressions to contain the left and right hand symbols.
 * Mainly when processing checks and the like returning from functions.
 */
record ExprLeftAndRightData(ISymbol left, ISymbol right) {
}
