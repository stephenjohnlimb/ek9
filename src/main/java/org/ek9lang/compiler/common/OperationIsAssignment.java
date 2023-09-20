package org.ek9lang.compiler.common;

import java.util.function.Predicate;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Is the token a simple assignment, i.e. not a deep assignment, but just an object pointer assignment.
 */
public class OperationIsAssignment implements Predicate<IToken> {
  @Override
  public boolean test(IToken op) {
    return op.getType() == EK9Parser.ASSIGN
        || op.getType() == EK9Parser.ASSIGN2
        || op.getType() == EK9Parser.COLON
        || op.getType() == EK9Parser.ASSIGN_UNSET;
  }
}
