package org.ek9lang.compiler.phase3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.support.ToCommaSeparated;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Checks and assigns type a 'this' or 'super' symbol, but only if it valid.
 * For example, you cannot use super in a function - that makes no sense.
 * But allowing applications, functions and programs to refer to themselves might be useful in
 * passing themselves as parameters to other functions in some way.
 */
final class CheckValidThisOrSuper extends RuleSupport implements Consumer<ParserRuleContext> {

  private final SymbolFactory symbolFactory;
  private final List<ISymbol.SymbolGenus> supportedThisAndSuperGenus = List.of(ISymbol.SymbolGenus.CLASS,
      ISymbol.SymbolGenus.COMPONENT, ISymbol.SymbolGenus.RECORD);

  private final List<ISymbol.SymbolGenus> supportedThisOnlyGenus =
      List.of(ISymbol.SymbolGenus.CLASS_TRAIT, ISymbol.SymbolGenus.SERVICE,
          ISymbol.SymbolGenus.TEXT, ISymbol.SymbolGenus.FUNCTION, ISymbol.SymbolGenus.FUNCTION_TRAIT,
          ISymbol.SymbolGenus.GENERAL_APPLICATION, ISymbol.SymbolGenus.CLASS_ENUMERATION,
          ISymbol.SymbolGenus.SERVICE_APPLICATION, ISymbol.SymbolGenus.PROGRAM);

  /**
   * Checks that this/super passed in is a suitable genus.
   */
  CheckValidThisOrSuper(final SymbolAndScopeManagement symbolAndScopeManagement,
                        final SymbolFactory symbolFactory,
                        final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolFactory = symbolFactory;
  }

  @Override
  public void accept(final ParserRuleContext ctx) {

    //First check if in a dynamic scope - that is what 'this' would apply to.
    var appropriateScope = symbolAndScopeManagement.getTopScope().findNearestDynamicBlockScopeInEnclosingScopes();
    if (appropriateScope.isEmpty()) {
      appropriateScope = symbolAndScopeManagement.getTopScope().findNearestNonBlockScopeInEnclosingScopes();
    }

    if (appropriateScope.isPresent() && appropriateScope.get() instanceof ISymbol basicType) {
      if ("this".equals(ctx.getText())) {
        createAndRecordThisOrSuper(ctx, checkThisUsage(ctx, basicType));
      } else {
        //Check for the use of 'super'
        createAndRecordThisOrSuper(ctx, checkSuperUsage(ctx, basicType));
      }
    }
  }

  private void createAndRecordThisOrSuper(final ParserRuleContext ctx, final ISymbol theType) {
    if (theType != null) {
      //Then it is valid to be created and recorded.
      var symbol = symbolFactory.newGeneralSymbol(new Ek9Token(ctx.start), ctx.getText());
      symbol.setInitialisedBy(theType.getInitialisedBy());
      symbol.setReferenced(true);
      symbol.setType(theType);
      symbolAndScopeManagement.recordSymbol(symbol, ctx);
    }
  }

  private ISymbol checkThisUsage(final ParserRuleContext ctx, final ISymbol basicType) {
    if (!supportedThisAndSuperGenus.contains(basicType.getGenus())
        && !supportedThisOnlyGenus.contains(basicType.getGenus())) {
      emitGenusErrorForSymbol(ctx, basicType);
      return null;
    }
    return basicType;
  }

  private ISymbol checkSuperUsage(final ParserRuleContext ctx, final ISymbol basicType) {
    if (basicType instanceof IAggregateSymbol asAggregate && asAggregate.getSuperAggregateSymbol().isPresent()) {
      return asAggregate.getSuperAggregateSymbol().get();
    }

    //The rest are just more specific types of errors to be reported.
    if (basicType instanceof FunctionSymbol asFunction && asFunction.getSuperFunctionSymbol().isPresent()) {
      //Do not allow use of super in functions.
      var superType = asFunction.getSuperFunctionSymbol().get();
      emitGenusErrorForSymbol(ctx, superType);

    } else if (supportedThisOnlyGenus.contains(basicType.getGenus())) {
      if (basicType.getGenus().equals(ISymbol.SymbolGenus.CLASS_TRAIT)) {
        //It can be a bit misleading for traits - because they really have traits of traits and not a single super.
        //But if sometimes feels like they have a super and so that's likely to be what a developer will think and use.
        emitHasNoTraitSuper(ctx, basicType);
      } else {
        emitInappropriateUseOFSuper(ctx, basicType);
      }
    } else {
      emitHasNoSuper(ctx, basicType);
    }
    return null;
  }

  private void emitHasNoTraitSuper(final ParserRuleContext ctx, final ISymbol basicType) {
    AggregateWithTraitsSymbol theTrait = (AggregateWithTraitsSymbol) basicType;
    var toCommaSeparated = new ToCommaSeparated(false);
    List<ISymbol> asTypes = new ArrayList<>(theTrait.getTraits());
    var msg = "";
    if (asTypes.isEmpty()) {
      msg = getSymbolErrorWithName(basicType) + " it does not have any traits itself:";
    } else {
      String listOfTraitsAsString = toCommaSeparated.apply(asTypes);
      msg = getSymbolErrorWithName(basicType)
          + " use a trait name to address the appropriate trait '"
          + listOfTraitsAsString
          + "':";
    }
    errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.INAPPROPRIATE_USE_OF_SUPER);
  }

  private void emitHasNoSuper(final ParserRuleContext ctx, final ISymbol basicType) {
    var msg = getSymbolErrorWithName(basicType) + ":";
    errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.AGGREGATE_HAS_NO_SUPER);
  }

  private void emitInappropriateUseOFSuper(final ParserRuleContext ctx, final ISymbol basicType) {
    var msg = getSymbolErrorWithName(basicType) + ":";
    errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.INAPPROPRIATE_USE_OF_SUPER);
  }

  private void emitGenusErrorForSymbol(final ParserRuleContext ctx, final ISymbol symbol) {
    var msg = getSymbolErrorDetails(symbol)
        + "' which is a '"
        + symbol.getCategory().getDescription()
        + "' rather than a '"
        + supportedThisAndSuperGenus.stream().map(ISymbol.SymbolGenus::getDescription)
        .collect(Collectors.joining(", "))
        + "':";
    errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.INCOMPATIBLE_GENUS);
  }

  private String getSymbolErrorWithName(final ISymbol symbol) {
    return getSymbolErrorDetails(symbol)
        + " '"
        + symbol.getFriendlyName()
        + "'";
  }

  private String getSymbolErrorDetails(final ISymbol symbol) {
    return "resolved as a '" + symbol.getGenus().getDescription() + "'";
  }
}