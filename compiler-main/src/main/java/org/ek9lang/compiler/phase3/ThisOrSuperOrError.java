package org.ek9lang.compiler.phase3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.support.ToCommaSeparated;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Checks and assigns type a 'this' or 'super' symbol, but only if it valid.
 * But allowing applications, functions and programs to refer to themselves might be useful in
 * passing themselves as parameters to other functions in some way.
 */
final class ThisOrSuperOrError extends TypedSymbolAccess implements Consumer<EK9Parser.PrimaryReferenceContext> {
  private final SymbolFactory symbolFactory;
  private final List<SymbolGenus> supportedThisAndSuperGenus = List.of(
      SymbolGenus.CLASS,
      SymbolGenus.COMPONENT, SymbolGenus.RECORD);

  private final List<SymbolGenus> supportedThisOnlyGenus =
      List.of(SymbolGenus.CLASS_TRAIT, SymbolGenus.SERVICE,
          SymbolGenus.TEXT, SymbolGenus.FUNCTION, SymbolGenus.FUNCTION_TRAIT,
          SymbolGenus.GENERAL_APPLICATION, SymbolGenus.CLASS_ENUMERATION,
          SymbolGenus.SERVICE_APPLICATION, SymbolGenus.PROGRAM);

  /**
   * Checks that this/super passed in is a suitable genus.
   */
  ThisOrSuperOrError(final SymbolsAndScopes symbolsAndScopes,
                     final SymbolFactory symbolFactory,
                     final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.symbolFactory = symbolFactory;

  }

  @Override
  public void accept(final EK9Parser.PrimaryReferenceContext ctx) {

    //First check if in a dynamic scope - that is what 'this' would apply to.
    var appropriateScope = symbolsAndScopes.getTopScope().findNearestDynamicBlockScopeInEnclosingScopes();
    if (appropriateScope.isEmpty()) {
      appropriateScope = symbolsAndScopes.getTopScope().findNearestNonBlockScopeInEnclosingScopes();
    }

    if (appropriateScope.isPresent() && appropriateScope.get() instanceof ISymbol basicType) {
      if ("this".equals(ctx.getText())) {
        createAndRecordThisOrSuper(ctx, thisUsageOrError(ctx, basicType));
      } else {
        //Check for the use of 'super'
        createAndRecordThisOrSuper(ctx, superUsageOrError(ctx, basicType));
      }
    }
  }

  private void createAndRecordThisOrSuper(final ParserRuleContext ctx, final ISymbol theType) {

    if (theType != null) {
      //Then it is valid to be created and recorded.
      final var symbol = symbolFactory.newGeneralSymbol(new Ek9Token(ctx.start), ctx.getText());
      symbol.setInitialisedBy(theType.getInitialisedBy());
      symbol.setReferenced(true);
      symbol.setType(theType);
      recordATypedSymbol(symbol, ctx);
    }

  }

  private ISymbol thisUsageOrError(final ParserRuleContext ctx, final ISymbol basicType) {

    if (!supportedThisAndSuperGenus.contains(basicType.getGenus())
        && !supportedThisOnlyGenus.contains(basicType.getGenus())) {
      emitGenusErrorForSymbol(ctx, basicType);
      return null;
    }

    return basicType;
  }

  private ISymbol superUsageOrError(final ParserRuleContext ctx, final ISymbol basicType) {

    //This is the only happy route to use 'super'.
    if (basicType instanceof IAggregateSymbol asAggregate && asAggregate.getSuperAggregate().isPresent()) {
      return asAggregate.getSuperAggregate().get();
    }

    //So the rest of this block is just about trying to emit the most appropriate error message.
    if (basicType instanceof FunctionSymbol asFunction && asFunction.getSuperFunction().isPresent()) {
      final var superType = asFunction.getSuperFunction().get();
      emitGenusErrorForSymbol(ctx, superType);
    } else if (supportedThisOnlyGenus.contains(basicType.getGenus())) {
      if (basicType.getGenus().equals(SymbolGenus.CLASS_TRAIT)) {
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


  private String getNoTraitSuperError(final AggregateWithTraitsSymbol theTrait) {

    final List<ISymbol> asTypes = new ArrayList<>(theTrait.getTraits());

    if (asTypes.isEmpty()) {
      return getSymbolErrorWithName(theTrait) + " it does not have any traits itself:";
    }

    final var toCommaSeparated = new ToCommaSeparated(false);
    final var listOfTraitsAsString = toCommaSeparated.apply(asTypes);

    return getSymbolErrorWithName(theTrait)
        + " use a trait name to address the appropriate trait '"
        + listOfTraitsAsString
        + "':";
  }

  private void emitHasNoTraitSuper(final ParserRuleContext ctx, final ISymbol basicType) {

    final AggregateWithTraitsSymbol theTrait = (AggregateWithTraitsSymbol) basicType;
    final var msg = getNoTraitSuperError(theTrait);

    errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.INAPPROPRIATE_USE_OF_SUPER);

  }

  private void emitHasNoSuper(final ParserRuleContext ctx, final ISymbol basicType) {

    final var msg = getSymbolErrorWithName(basicType) + ":";
    errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.AGGREGATE_HAS_NO_SUPER);

  }

  private void emitInappropriateUseOFSuper(final ParserRuleContext ctx, final ISymbol basicType) {

    final var msg = getSymbolErrorWithName(basicType) + ":";
    errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.INAPPROPRIATE_USE_OF_SUPER);

  }

  private void emitGenusErrorForSymbol(final ParserRuleContext ctx, final ISymbol symbol) {

    final var msg = getSymbolErrorDetails(symbol)
        + "' which is a '"
        + symbol.getCategory().getDescription()
        + "' rather than a '"
        + supportedThisAndSuperGenus.stream().map(SymbolGenus::getDescription)
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