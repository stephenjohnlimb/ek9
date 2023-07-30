package org.ek9lang.compiler.main.rules;

import static org.ek9lang.compiler.errors.ErrorListener.SemanticClassification.COMPONENT_INJECTION_NOT_POSSIBLE;

import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks that the variable supplied (if not null and has a type), does not have a type that
 * is a genericTypeParameter as those cannot be used with injection '!'.
 */
public class CheckNotGenericTypeParameter implements Consumer<ISymbol> {

  private final ErrorListener errorListener;

  public CheckNotGenericTypeParameter(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(ISymbol variable) {
    if (variable != null && variable.getType().isPresent()) {
      variable.getType().ifPresent(type -> {
        if (type.isConceptualTypeParameter()) {
          errorListener.semanticError(variable.getSourceToken(), "", COMPONENT_INJECTION_NOT_POSSIBLE);
        }
      });
    }
  }
}
