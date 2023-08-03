package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.support.TypeListComparator;

/**
 * Checks the number and types of the two sets of parameter list match exactly.
 */
final class CheckParameterTypesExactMatch extends RuleSupport implements Consumer<ParametersCheckData> {

  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final TypeListComparator typeListComparator = new TypeListComparator();

  CheckParameterTypesExactMatch(final SymbolAndScopeManagement symbolAndScopeManagement,
                                final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final ParametersCheckData data) {
    if (data.from().size() != data.to().size()) {
      //Well there is no point in checking types as the number of parameters is incorrect.
      errorListener.semanticError(data.token(), "mismatch in number of parameters; " + data.errorMessage(),
          ErrorListener.SemanticClassification.FUNCTION_SIGNATURE_DOES_NOT_MATCH_SUPER);
    } else {
      //OK now need to get the types of each set of parameters and then check they are an exact match.
      var fromTypes = symbolTypeExtractor.apply(data.from());
      var toTypes = symbolTypeExtractor.apply(data.to());
      //Only if all types are present - other errors issued if types are missing.
      if (fromTypes.size() == data.from().size()
          && toTypes.size() == data.to().size()
          && !typeListComparator.test(fromTypes, toTypes)) {
        errorListener.semanticError(data.token(), "mismatch in parameters; " + data.errorMessage(),
            ErrorListener.SemanticClassification.FUNCTION_SIGNATURE_DOES_NOT_MATCH_SUPER);
      }
    }
  }
}
