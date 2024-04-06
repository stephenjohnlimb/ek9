package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;

/**
 * Checks the covariance compatibility of two variables, this will traverse them to get their types.
 * Emits errors if they are not compatible.
 * This is a check that excludes coercion of types.
 */
final class CheckTypeCovariance extends TypedSymbolAccess implements Consumer<CovarianceCheckData> {
  CheckTypeCovariance(final SymbolAndScopeManagement symbolAndScopeManagement,
                      final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final CovarianceCheckData data) {

    if (data.fromVar() == null && data.toVar() == null) {
      //Both not returning a value so nothing to check, or types are missing so previous errors will be shown.
      //So this is a no-operation
      return;
    }

    if (data.fromVar() == null) {
      //Cannot alter return to be Void (nothing)
      emitCovarianceMismatch(data, "missing return type/value; ");
    } else if (data.toVar() == null) {
      //Cannot do reverse either if base was Void cannot not add a return type
      emitCovarianceMismatch(data, "unexpected return type/value; ");
    } else if (data.fromVar().getType().isPresent() && data.toVar().getType().isPresent()) {

      //But if they both return something - then lets check the types and see if they are compatible (without coercion).
      final var fromReturnType = data.fromVar().getType();
      final var toMethodReturnType = data.toVar().getType();
      final var assignableWeight = fromReturnType.get().getUnCoercedAssignableWeightTo(toMethodReturnType.get());

      if (assignableWeight < 0.0) {
        emitCovarianceMismatch(data, "incompatible return types; ");
      }
    }

  }

  private void emitCovarianceMismatch(final CovarianceCheckData data, final String additionalInformation) {
    errorListener.semanticError(data.token(), additionalInformation + data.errorMessage(),
        ErrorListener.SemanticClassification.COVARIANCE_MISMATCH);
  }
}
