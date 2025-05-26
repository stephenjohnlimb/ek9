package org.ek9lang.compiler.phase2;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.COMPONENT_INJECTION_NOT_POSSIBLE;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks that the variable supplied (if not null and has a type), does not have a type that
 * is a genericTypeParameter. (those cannot be used with injection '!')
 */
final class NotGenericTypeParameterOrError implements Consumer<ISymbol> {

  private final ErrorListener errorListener;

  NotGenericTypeParameterOrError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final ISymbol variable) {

    if (variable != null && variable.getType().isPresent()) {
      variable.getType().ifPresent(type -> {
        if (type.isConceptualTypeParameter()) {
          errorListener.semanticError(variable.getSourceToken(), "", COMPONENT_INJECTION_NOT_POSSIBLE);
        }
      });
    }

  }
}
