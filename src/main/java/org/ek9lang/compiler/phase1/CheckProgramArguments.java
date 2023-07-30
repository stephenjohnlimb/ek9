package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.support.ProgramArgumentPredicate;

/**
 * A program can only accept specific types of arguments to it.
 */
final class CheckProgramArguments implements BiConsumer<Token, MethodSymbol> {

  private final ProgramArgumentPredicate typePredicate = new ProgramArgumentPredicate();
  private final ErrorListener errorListener;

  CheckProgramArguments(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final Token token, final MethodSymbol methodSymbol) {

    int numParameters = methodSymbol.getCallParameters().size();

    for (var methodParameter : methodSymbol.getCallParameters()) {
      var parameterType = methodParameter.getType();
      if (parameterType.isPresent()) {
        var theType = parameterType.get();
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
