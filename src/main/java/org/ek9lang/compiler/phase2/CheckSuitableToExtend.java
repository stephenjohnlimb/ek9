package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.support.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;

/**
 * Checks for a type is resolved and is suitable to be extended from (in basic terms).
 * This does not include the 'allows only' graph that has to be done one the whole type hierarchy has been
 * established.
 */
final class CheckSuitableToExtend extends RuleSupport implements Function<ParserRuleContext, Optional<ISymbol>> {

  private final CheckSuitableGenus checkSuitableGenus;

  /**
   * Checks that the typedef/identifierReference passed in (when resolved) is suitable to be extended from.
   */
  CheckSuitableToExtend(final SymbolAndScopeManagement symbolAndScopeManagement,
                        final ErrorListener errorListener,
                        final ISymbol.SymbolGenus genus,
                        final boolean issueErrorIfNotResolved) {
    this(symbolAndScopeManagement, errorListener, List.of(genus), issueErrorIfNotResolved);
  }

  /**
   * Checks that the typedef/identifierReference passed in (when resolved) is suitable to be extended from.
   * Accepts multiple allowed genus. i.e. FUNCTION and FUNCTION_TRAIT.
   */
  public CheckSuitableToExtend(final SymbolAndScopeManagement symbolAndScopeManagement,
                               final ErrorListener errorListener,
                               final List<ISymbol.SymbolGenus> genus,
                               final boolean issueErrorIfNotResolved) {
    super(symbolAndScopeManagement, errorListener);
    this.checkSuitableGenus =
        new CheckSuitableGenus(symbolAndScopeManagement, errorListener, genus, true, issueErrorIfNotResolved);
  }

  @Override
  public Optional<ISymbol> apply(final ParserRuleContext ctx) {
    var rtn = checkSuitableGenus.apply(ctx);
    if (rtn.isPresent() && checkIfValidSuper(ctx, rtn.get())) {
      return rtn;
    }
    return Optional.empty();
  }

  private boolean checkIfValidSuper(final ParserRuleContext ctx, final ISymbol proposedSuper) {
    if (proposedSuper instanceof PossibleGenericSymbol possibleGenericSymbol) {
      if (!isOpenForExtension(ctx, possibleGenericSymbol)) {
        return false;
      }
      if (isParameterisedGenericWithinAGeneric(ctx)) {
        return true;
      }
      return isNotGenericInNature(ctx, possibleGenericSymbol);
    }

    return true;
  }

  private boolean isOpenForExtension(final ParserRuleContext ctx, final PossibleGenericSymbol possibleGenericSymbol) {
    if (!possibleGenericSymbol.isOpenForExtension()) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_OPEN_TO_EXTENSION);
      return false;
    }
    return true;
  }

  private boolean isParameterisedGenericWithinAGeneric(final ParserRuleContext ctx) {
    var scope = symbolAndScopeManagement.getTopScope();
    //So this is being used by a dynamic class of function
    //And it being used within a generic type in some way,
    //we let the generic nature pass.
    if (scope.getScopeType().equals(IScope.ScopeType.DYNAMIC_BLOCK)
        && scope instanceof PossibleGenericSymbol dynamic
        && dynamic.getOuterMostTypeOrFunction().isPresent()) {
      var outerType = dynamic.getOuterMostTypeOrFunction().get();
      return (outerType.isTemplateType() || outerType.isTemplateFunction())
          && ctx instanceof EK9Parser.ParameterisedTypeContext;
    }
    return false;
  }

  private boolean isNotGenericInNature(final ParserRuleContext ctx, final PossibleGenericSymbol possibleGenericSymbol) {
    if (possibleGenericSymbol.isGenericInNature()) {
      var msg = "is a '"
          + possibleGenericSymbol.getCategory().getDescription()
          + "' that has not been parameterised:";
      errorListener.semanticError(ctx.start, msg,
          ErrorListener.SemanticClassification.INCOMPATIBLE_CATEGORY);
      return false;
    }
    return true;
  }
}