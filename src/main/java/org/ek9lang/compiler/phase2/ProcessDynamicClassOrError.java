package org.ek9lang.compiler.phase2;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.NoDuplicateOperationsOrError;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Configures a dynamic class in terms of settings its super and configured traits.
 * This does use a number of other components to check the suitability of the dynamic class creation
 * as defined in the EK9 source code.
 */
final class ProcessDynamicClassOrError extends RuleSupport implements
    Consumer<EK9Parser.DynamicClassDeclarationContext> {

  private final VisibilityOfOperationsOrError visibilityOfOperationsOrError;
  private final NoDuplicateOperationsOrError noDuplicateOperationsOrError;
  private final SuitableToExtendOrError classSuitableToExtendOrError;
  private final SuitableToExtendOrError classTraitSuitableToExtendOrError;

  ProcessDynamicClassOrError(final SymbolsAndScopes symbolsAndScopes,
                             final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.visibilityOfOperationsOrError = new VisibilityOfOperationsOrError(symbolsAndScopes, errorListener);
    this.noDuplicateOperationsOrError = new NoDuplicateOperationsOrError(errorListener);
    this.classSuitableToExtendOrError =
        new SuitableToExtendOrError(symbolsAndScopes, errorListener, SymbolGenus.CLASS, true);
    this.classTraitSuitableToExtendOrError =
        new SuitableToExtendOrError(symbolsAndScopes, errorListener, SymbolGenus.CLASS_TRAIT, true);

  }

  @Override
  public void accept(final EK9Parser.DynamicClassDeclarationContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof AggregateWithTraitsSymbol asAggregate) {

      visibilityOfOperationsOrError.accept(asAggregate);
      noDuplicateOperationsOrError.accept(new Ek9Token(ctx.start), asAggregate);

      if (ctx.parameterisedType() != null) {
        final var resolved = classSuitableToExtendOrError.apply(ctx.parameterisedType());
        resolved.ifPresent(theSuper -> asAggregate.setSuperAggregate((IAggregateSymbol) theSuper));
      } else {
        //Else we give it the implicit super of 'Any'.
        asAggregate.setSuperAggregate((IAggregateSymbol) symbolsAndScopes.getEk9Any());
      }

      if (ctx.traitsList() != null) {
        ctx.traitsList().traitReference().forEach(traitRef -> {
          final var resolved = classTraitSuitableToExtendOrError.apply(traitRef.identifierReference());
          resolved.ifPresent(theTrait -> asAggregate.addTrait((AggregateWithTraitsSymbol) theTrait));
        });
      }
    }
  }

}
