package org.ek9lang.compiler.phase7.support;

import org.ek9lang.antlr.EK9Parser;

/**
 * A simple record that can be used when processing expressions.
 * Just gathers together the essential parts for processing.
 */
public record ExprProcessingDetails(EK9Parser.ExpressionContext ctx,
                                    VariableDetails variableDetails) {
}
