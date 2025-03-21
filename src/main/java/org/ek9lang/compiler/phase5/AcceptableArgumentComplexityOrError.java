package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.EXCESSIVE_COMPLEXITY;

import java.util.function.ObjIntConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.support.AggregateManipulator;

/**
 * Emit error if there are too many arguments. More than 50 arguments is too many.
 */
class AcceptableArgumentComplexityOrError implements ObjIntConsumer<Token> {

  private final ComplexityCounter complexityCounter;
  private final ErrorListener errorListener;

  AcceptableArgumentComplexityOrError(final ComplexityCounter complexityCounter,
                                      final ErrorListener errorListener) {
    this.complexityCounter = complexityCounter;
    this.errorListener = errorListener;

  }

  @Override
  public void accept(final Token errorLocation, final int numArguments) {

    if (numArguments > AggregateManipulator.MAX_ARGUMENTS_PER_CALL) {
      emitTooManyArgumentsError(errorLocation, numArguments);
      //This will be a hard stop
    } else if (numArguments >= 5) {
      complexityCounter.incrementComplexity(2);
    } else if (numArguments >= 3) {
      complexityCounter.incrementComplexity();
    }

  }

  private void emitTooManyArgumentsError(final Token errorLocation, final Integer numArguments) {
    errorListener.semanticError(errorLocation, numArguments + " arguments is too many:", EXCESSIVE_COMPLEXITY);
  }
}
