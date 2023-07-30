package org.ek9lang.compiler.support;

import java.util.function.Predicate;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;

/**
 * Is the token a simple assignment, i.e. not a deep assignment, but just an object pointer assignment.
 */
public class OperationIsAssignment implements Predicate<Token> {
  @Override
  public boolean test(Token op) {
    return op.getType() == EK9Parser.ASSIGN
        || op.getType() == EK9Parser.ASSIGN2
        || op.getType() == EK9Parser.COLON
        || op.getType() == EK9Parser.ASSIGN_UNSET;
  }
}
