package org.ek9lang.compiler.phase7.support;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Data class to hold parameters for guarded assignment generation.
 * STACK-BASED: Scope information is now managed via stack context.
 */
public record GuardedAssignmentDetails(
    ISymbol lhsSymbol,
    EK9Parser.AssignmentExpressionContext assignmentExpression
) {
}