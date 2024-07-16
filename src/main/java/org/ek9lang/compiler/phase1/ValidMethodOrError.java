package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ContextSupportsAbstractMethodOrError;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ProcessTraitMethodOrError;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TraitMethodAcceptableOrError;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Checks methods from various contexts, typically this is delegated to other functions.
 * Those functions do the detail check.
 */
final class ValidMethodOrError extends RuleSupport implements
    BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {

  private final ProcessCommonMethodsOrError processCommonMethodsOrError;
  private final ProcessTraitMethodOrError processTraitMethodOrError;
  private final TraitMethodAcceptableOrError traitMethodAcceptableOrError;
  private final ContextSupportsAbstractMethodOrError contextSupportsAbstractMethodOrError;
  private final NonExtendableMethodOrError nonExtendableMethodOrError;
  private final NotDispatcherMethodOrError notDispatcherMethodOrError;
  private final GenericConstructorOrError genericConstructorOrError;
  private final ProgramReturnOrError programReturnOrError;
  private final ProgramArgumentsOrError programArgumentsOrError;
  private final ImplementationPresentOrError implementationPresentOrError;
  private final NormalTerminationOrError normalTerminationOrError;
  private final NoMethodReturnOrError noMethodReturnOrError;
  private final MethodNotOperatorOrError methodNotOperatorOrError;

  /**
   * Create a new method checker.
   */
  ValidMethodOrError(final SymbolsAndScopes symbolsAndScopes,
                     final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    processCommonMethodsOrError = new ProcessCommonMethodsOrError(symbolsAndScopes, errorListener);
    traitMethodAcceptableOrError = new TraitMethodAcceptableOrError(errorListener);
    contextSupportsAbstractMethodOrError =
        new ContextSupportsAbstractMethodOrError(symbolsAndScopes, errorListener);
    nonExtendableMethodOrError = new NonExtendableMethodOrError(errorListener);
    processTraitMethodOrError = new ProcessTraitMethodOrError(errorListener);
    notDispatcherMethodOrError = new NotDispatcherMethodOrError(errorListener);
    genericConstructorOrError = new GenericConstructorOrError(errorListener);
    programReturnOrError = new ProgramReturnOrError(errorListener);
    programArgumentsOrError = new ProgramArgumentsOrError(errorListener);
    implementationPresentOrError = new ImplementationPresentOrError(errorListener);
    normalTerminationOrError = new NormalTerminationOrError(errorListener);
    noMethodReturnOrError = new NoMethodReturnOrError(symbolsAndScopes, errorListener);
    methodNotOperatorOrError = new MethodNotOperatorOrError(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    if (method.isConstructor()) {
      checkAsConstructor(method, ctx);
    }

    if (ctx.getParent() instanceof EK9Parser.ProgramBlockContext) {
      checkAsProgram(method, ctx);
    }

    if (ctx.getParent() instanceof EK9Parser.ServiceDeclarationContext) {
      checkAsServiceMethod(method, ctx);
    }

    if (ctx.getParent().getParent() instanceof EK9Parser.TraitDeclarationContext) {
      processTraitMethodOrError.accept(method, ctx.operationDetails());
    }

    //If not in a class then method must not be marked as dispatcher.
    if (!(ctx.getParent().getParent() instanceof EK9Parser.ClassDeclarationContext)
        && !(ctx.getParent().getParent() instanceof EK9Parser.DynamicClassDeclarationContext)) {
      notDispatcherMethodOrError.accept(method, ctx);
    }

    if (!(ctx.getParent().getParent() instanceof EK9Parser.TraitDeclarationContext)) {
      traitMethodAcceptableOrError.accept(method, ctx.operationDetails());
    }

    processCommonMethodsOrError.accept(method, ctx);
    contextSupportsAbstractMethodOrError.accept(method, ctx);
    methodNotOperatorOrError.accept(method, ctx);

  }

  private void checkAsConstructor(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    var startToken = new Ek9Token(ctx.start);
    normalTerminationOrError.accept(startToken, method);
    genericConstructorOrError.accept(startToken, method);
    noMethodReturnOrError.accept(method, ctx);

  }

  private void checkAsProgram(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    var startToken = new Ek9Token(ctx.start);
    programReturnOrError.accept(startToken, method);
    programArgumentsOrError.accept(startToken, method);
    nonExtendableMethodOrError.accept(method, ctx);

  }

  private void checkAsServiceMethod(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    implementationPresentOrError.accept(ctx);
    nonExtendableMethodOrError.accept(method, ctx);
  }

}
