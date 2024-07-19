package org.ek9lang.compiler.phase2;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.NoDuplicateOperationsOrError;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

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
        new SuitableToExtendOrError(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.CLASS_TRAIT, true);
    this.allowedClassGenusOrError =
        new SuitableGenusOrError(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.CLASS, false, true);

  }

  @Override
  public void accept(EK9Parser.TraitDeclarationContext ctx) {
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

      if (ctx.allowingOnly() != null) {
        ctx.allowingOnly().identifierReference().forEach(classRef -> {
          final var resolved = allowedClassGenusOrError.apply(classRef);
          resolved.ifPresent(theClass -> symbol.addAllowedExtender((IAggregateSymbol) theClass));
        });
      }
    }
  }
}
