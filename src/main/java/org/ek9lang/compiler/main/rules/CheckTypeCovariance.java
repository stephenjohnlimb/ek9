package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;

/**
 * Checks the covariance compatibility of two variables, this will traverse them to get their types.
 * Emits errors if they are not compatible.
 * This is a check that excludes coercion of types.
 */
public class CheckTypeCovariance extends RuleSupport implements Consumer<CovarianceCheckData> {
  public CheckTypeCovariance(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final CovarianceCheckData data) {
    //If types are missing then errors will already exist.
    if (data.fromVar() != null && data.toVar() != null
        && data.fromVar().getType().isPresent() && data.toVar().getType().isPresent()) {
      //But if they both return something - then lets check the types and see if they are compatible (without coercion).
      var fromReturnType = data.fromVar().getType();
      var toMethodReturnType = data.toVar().getType();

      //Check using no coercion - for compatibility.
      var assignableWeight = fromReturnType.get().getUnCoercedAssignableWeightTo(toMethodReturnType.get());
      if (assignableWeight < 0.0) {
        errorListener.semanticError(data.token(), "incompatible return types; " + data.errorMessage(),
            ErrorListener.SemanticClassification.COVARIANCE_MISMATCH);
      }
    } else if (data.fromVar() == null && data.toVar() != null) {
      //Cannot alter return to be Void (nothing)
      errorListener.semanticError(data.token(), "missing return type/value; " + data.errorMessage(),
          ErrorListener.SemanticClassification.COVARIANCE_MISMATCH);
    } else if (data.fromVar() != null && data.toVar() == null) {
      //Cannot do reverse either if base was Void cannot no add a return type
      errorListener.semanticError(data.token(), "unexpected return type/value; " + data.errorMessage(),
          ErrorListener.SemanticClassification.COVARIANCE_MISMATCH);
    }
    //else both not returning a value so nothing to check, or types are missing so previous errors will be shown.
  }
}
