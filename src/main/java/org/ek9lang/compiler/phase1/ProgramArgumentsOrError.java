package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.support.ProgramArgumentPredicate;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * A program can only accept specific types of arguments to it.
 */
final class ProgramArgumentsOrError implements BiConsumer<IToken, MethodSymbol> {

  private final ProgramArgumentPredicate typePredicate = new ProgramArgumentPredicate();
  private final ErrorListener errorListener;

  ProgramArgumentsOrError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final IToken token, final MethodSymbol methodSymbol) {

    final int numParameters = methodSymbol.getCallParameters().size();

    for (var methodParameter : methodSymbol.getCallParameters()) {
      final var parameterType = methodParameter.getType();

      if (parameterType.isPresent()) {
        final var theType = parameterType.get();

        if (theType.isParameterisedType() && numParameters > 1) {
          errorListener.semanticError(methodParameter.getSourceToken(), "'" + theType.getFriendlyName() + "'",
              ErrorListener.SemanticClassification.PROGRAM_ARGUMENTS_INAPPROPRIATE);
        }

        if (!typePredicate.test(theType)) {
          errorListener.semanticError(methodParameter.getSourceToken(),
              "'" + theType.getFriendlyName() + "' is inappropriate,",
              ErrorListener.SemanticClassification.PROGRAM_ARGUMENT_TYPE_INVALID);
        }

      } else {
        errorListener.semanticError(methodParameter.getSourceToken(), "",
            ErrorListener.SemanticClassification.PROGRAM_ARGUMENT_TYPE_INVALID);
      }
    }
  }

}
