package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NO_INCOMING_ARGUMENT_REASSIGNMENT;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NO_PURE_REASSIGNMENT;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.REASSIGNMENT_OF_INJECTED_COMPONENT;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks the assignments for operator : := = and :=? that's all.
 * Applies rules and emits error as appropriate.
 */
final class AssignmentOrError extends TypedSymbolAccess implements BiConsumer<IToken, ISymbol> {
  private final boolean isDeclaration;
  private final IsSetPresentOrError isSetPresentOrError;

  AssignmentOrError(final SymbolsAndScopes symbolsAndScopes,
                    final ErrorListener errorListener,
                    final boolean isDeclaration) {

    super(symbolsAndScopes, errorListener);
    this.isDeclaration = isDeclaration;
    this.isSetPresentOrError = new IsSetPresentOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final IToken op, final ISymbol leftHandSideSymbol) {

    if (leftHandSideSymbol == null) {
      //may not have resolved properly - so other errors would be emitted.
      return;
    }

    var isSetCoalescing = ":=?".equals(op.getText());

    //This is the apply the null/isSet coalescing operator. To apply that it is necessary that operator '?' isSet exists
    if (isSetCoalescing) {
      isSetPresentOrError.test(op, leftHandSideSymbol);
    }

    if (leftHandSideSymbol.isInjectionExpected()) {
      //So if one of the other assignments we don't just allow reassignment if the lhs is marked for injection.
      //We do allow that when used with :=? so that a variable can be set if it has not been injected.
      //This is to try and stop defects where a variable is marked for injection but gets assigned to.
      //With the :=? that reassignment only happens if the variable has not been assigned(injected) already.
      errorListener.semanticError(op, "", REASSIGNMENT_OF_INJECTED_COMPONENT);
    }

    if (leftHandSideSymbol.isIncomingParameter()) {
      errorListener.semanticError(op, "'" + leftHandSideSymbol.getFriendlyName() + "':",
          NO_INCOMING_ARGUMENT_REASSIGNMENT);
    }

    //If it is a declaration then it is not a reassignment - but an initial assignment.
    if (!isDeclaration && !isSetCoalescing && isPureReassignmentDisallowed(op, leftHandSideSymbol)) {
      errorListener.semanticError(op, "'" + leftHandSideSymbol.getFriendlyName() + "':",
          NO_PURE_REASSIGNMENT);
    }


  }

  /**
   * Now for any returning variable, we do have to let it be assigned at declaration (if specified).
   * But subsequent assignments can only be :=?. So in general you'd expect a pure function/method
   * to leave it as uninitialised and then only use the :=? once to set it.
   */
  private boolean isPureReassignmentDisallowed(final IToken op, final ISymbol leftHandSideSymbol) {

    if (leftHandSideSymbol.isReturningParameter() && leftHandSideSymbol.isInitialised()
        && leftHandSideSymbol.getSourceToken().getLine() == op.getLine()) {
      //Not disallowed irrespective of pure scope, as it is the first direct initialisation.
      return false;
    }

    return isProcessingScopePure();
  }
}
