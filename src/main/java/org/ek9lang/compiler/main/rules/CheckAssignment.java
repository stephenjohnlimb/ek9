package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Check an assignment again 'super' use and some operators against 'this' use.
 */
public class CheckAssignment implements Consumer<EK9Parser.AssignmentStatementContext> {

  private final ErrorListener errorListener;

  public CheckAssignment(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(EK9Parser.AssignmentStatementContext ctx) {
    if(ctx.primaryReference() != null)
    {
      //no assignment allowed and 'super' use is not appropriate only 'this'
      if(ctx.primaryReference().SUPER() != null)
      {
        errorListener.semanticError(ctx.primaryReference().start, "'super'", ErrorListener.SemanticClassification.USE_OF_SUPER_INAPPROPRIATE);
      }
      else
      {
        //Basically we allow a sort of assignment that accepts this 'this' has a value but can be mutated.
        //For example the merge operator or, copy (and others) but NOT these below.
        switch(ctx.op.getType())
        {
          case EK9Parser.ASSIGN:
          case EK9Parser.ASSIGN2:
          case EK9Parser.COLON:
          case EK9Parser.ASSIGN_UNSET:
            errorListener.semanticError(ctx.primaryReference().start, "'this'", ErrorListener.SemanticClassification.USE_OF_THIS_INAPPROPRIATE);
            break;
        }
      }
    }
  }
}
