package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NO_INCOMING_ARGUMENT_REASSIGNMENT;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.REASSIGNMENT_OF_INJECTED_COMPONENT;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks the assignments for operator : := = and :=? that's all.
 * Applies rules and emits error as appropriate.
 */
final class CheckAssignment extends TypedSymbolAccess implements BiConsumer<IToken, ISymbol> {
  private final CheckIsSet checkIsSet;

  CheckAssignment(SymbolAndScopeManagement symbolAndScopeManagement,
                  ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkIsSet
        = new CheckIsSet(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final IToken op, ISymbol leftHandSideSymbol) {
    //This is the apply the null/isSet coalescing operator. To apply that it is necessary that operator '?' isSet exists
    if (":=?".equals(op.getText())) {
      checkIsSet.test(op, leftHandSideSymbol);
    } else {
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
    }
  }
}
