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
 * Configures the trait declaration and completes a number of checks on that trait once configured.
 * For example, checks for appropriate method visibility, extendability and that there are no duplicate methods
 * with the same signatures.
 */
final class ProcessTraitDeclarationOrError extends RuleSupport
    implements Consumer<EK9Parser.TraitDeclarationContext> {

  private final VisibilityOfOperationsOrError visibilityOfOperationsOrError;
  private final NoDuplicateOperationsOrError noDuplicateOperationsOrError;
  private final SuitableToExtendOrError classTraitSuitableToExtendOrError;
  private final SuitableGenusOrError allowedClassGenusOrError;

  ProcessTraitDeclarationOrError(final SymbolsAndScopes symbolsAndScopes,
                                 final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.visibilityOfOperationsOrError = new VisibilityOfOperationsOrError(symbolsAndScopes, errorListener);
    this.noDuplicateOperationsOrError = new NoDuplicateOperationsOrError(errorListener);
    classTraitSuitableToExtendOrError =
        new SuitableToExtendOrError(symbolsAndScopes, errorListener, SymbolGenus.CLASS_TRAIT, true);
    this.allowedClassGenusOrError =
        new SuitableGenusOrError(symbolsAndScopes, errorListener, SymbolGenus.CLASS, false, true);

  }

  @SuppressWarnings("checkstyle:LambdaParameterName")
  @Override
  public void accept(final EK9Parser.TraitDeclarationContext ctx) {

    final var symbol = (AggregateWithTraitsSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    if (symbol != null) {
      visibilityOfOperationsOrError.accept(symbol);
      noDuplicateOperationsOrError.accept(new Ek9Token(ctx.start), symbol);

      if (ctx.traitsList() != null) {
        ctx.traitsList().traitReference().forEach(traitRef -> {
          final var resolved = classTraitSuitableToExtendOrError.apply(traitRef.identifierReference());
          resolved.ifPresent(theTrait -> symbol.addTrait((AggregateWithTraitsSymbol) theTrait));
        });
      }
      //Always set the super of a trait to 'Any'.
      symbol.getType()
          .ifPresent(_ -> symbol.setSuperAggregate(symbolsAndScopes.getEk9Types().ek9Any()));

      if (ctx.allowingOnly() != null) {
        ctx.allowingOnly().identifierReference().forEach(classRef -> {
          final var resolved = allowedClassGenusOrError.apply(classRef);
          resolved.ifPresent(theClass -> symbol.addAllowedExtender((IAggregateSymbol) theClass));
        });
      }
    }
  }
}
