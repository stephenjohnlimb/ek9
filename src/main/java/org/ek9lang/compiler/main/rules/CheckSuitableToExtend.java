package org.ek9lang.compiler.main.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;

/**
 * Checks for a type is resolved and is suitable to be extended from (in basic terms).
 * This does not include the 'allows only' graph that has to be done one the whole type hierarchy has been
 * established.
 */
public class CheckSuitableToExtend implements Function<ParserRuleContext, Optional<ISymbol>> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final List<ISymbol.SymbolGenus> supportedGenus = new ArrayList<>();
  private final ErrorListener errorListener;

  private final boolean issueErrorIfNotResolved;

  /**
   * Checks that the typedef/identifierReference passed in (when resolved) is suitable to be extended from.
   */
  public CheckSuitableToExtend(final SymbolAndScopeManagement symbolAndScopeManagement,
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
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;
    supportedGenus.addAll(genus);
    this.issueErrorIfNotResolved = issueErrorIfNotResolved;
  }

  @Override
  public Optional<ISymbol> apply(final ParserRuleContext ctx) {
    var theType = symbolAndScopeManagement.getRecordedSymbol(ctx);
    return checkIfValidSuper(ctx, theType) ? Optional.of(theType) : Optional.empty();
  }

  private boolean checkIfValidSuper(final ParserRuleContext ctx, final ISymbol proposedSuper) {
    if (!checkResolved(ctx, proposedSuper)) {
      return false;
    }

    if (!checkGenusOK(ctx, proposedSuper)) {
      return false;
    }

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

  private boolean checkResolved(final ParserRuleContext ctx, final ISymbol proposedSuper) {
    if (proposedSuper == null) {
      if (issueErrorIfNotResolved) {
        errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
      }
      return false;
    }
    return true;
  }

  private boolean checkGenusOK(final ParserRuleContext ctx, final ISymbol proposedSuper) {
    if (!supportedGenus.contains(proposedSuper.getGenus())) {
      var msg = "resolved as a '"
          + proposedSuper.getGenus().getDescription()
          + "' which is a '"
          + proposedSuper.getCategory().getDescription()
          + "' rather than a '"
          + supportedGenus.stream().map(ISymbol.SymbolGenus::getDescription).collect(Collectors.joining(", "))
          + "':";
      errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.INCOMPATIBLE_GENUS);
      return false;
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