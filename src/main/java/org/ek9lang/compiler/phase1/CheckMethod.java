package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ContextSupportsAbstractMethodOrError;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ProcessTraitMethodOrError;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TraitMethodAcceptableOrError;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Checks methods from various contexts, typically this is delegated to other functions.
 * Those functions do the detail check.
 */
final class CheckMethod extends RuleSupport implements BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {

  private final CommonMethodChecks commonMethodChecks;
  private final ProcessTraitMethodOrError processTraitMethodOrError;
  private final TraitMethodAcceptableOrError traitMethodAcceptableOrError;
  private final ContextSupportsAbstractMethodOrError contextSupportsAbstractMethodOrError;
  private final CheckNonExtendableMethod checkNonExtendableMethod;
  private final CheckNotDispatcherMethod checkNotDispatcherMethod;
  private final CheckGenericConstructor checkGenericConstructor;
  private final CheckProgramReturns checkProgramReturns;
  private final CheckProgramArguments checkProgramArguments;
  private final CheckForImplementation checkForImplementation;
  private final CheckNormalTermination checkNormalTermination;
  private final CheckNoMethodReturn checkNoMethodReturn;
  private final CheckMethodNotOperatorName checkMethodNotOperatorName;

  /**
   * Create a new method checker.
   */
  CheckMethod(final SymbolAndScopeManagement symbolAndScopeManagement,
              final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    commonMethodChecks = new CommonMethodChecks(symbolAndScopeManagement, errorListener);
    traitMethodAcceptableOrError = new TraitMethodAcceptableOrError(errorListener);
    contextSupportsAbstractMethodOrError =
        new ContextSupportsAbstractMethodOrError(symbolAndScopeManagement, errorListener);
    checkNonExtendableMethod = new CheckNonExtendableMethod(errorListener);
    processTraitMethodOrError = new ProcessTraitMethodOrError(errorListener);
    checkNotDispatcherMethod = new CheckNotDispatcherMethod(errorListener);
    checkGenericConstructor = new CheckGenericConstructor(errorListener);
    checkProgramReturns = new CheckProgramReturns(errorListener);
    checkProgramArguments = new CheckProgramArguments(errorListener);
    checkForImplementation = new CheckForImplementation(errorListener);
    checkNormalTermination = new CheckNormalTermination(errorListener);
    checkNoMethodReturn = new CheckNoMethodReturn(symbolAndScopeManagement, errorListener);
    checkMethodNotOperatorName = new CheckMethodNotOperatorName(symbolAndScopeManagement, errorListener);
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
      checkNotDispatcherMethod.accept(method, ctx);
    }

    if (!(ctx.getParent().getParent() instanceof EK9Parser.TraitDeclarationContext)) {
      traitMethodAcceptableOrError.accept(method, ctx.operationDetails());
    }

    commonMethodChecks.accept(method, ctx);

    contextSupportsAbstractMethodOrError.accept(method, ctx);

    checkMethodNotOperatorName.accept(method, ctx);

  }

  private void checkAsConstructor(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    var startToken = new Ek9Token(ctx.start);
    checkNormalTermination.accept(startToken, method);
    checkGenericConstructor.accept(startToken, method);
    checkNoMethodReturn.accept(method, ctx);

  }

  private void checkAsProgram(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    var startToken = new Ek9Token(ctx.start);
    checkProgramReturns.accept(startToken, method);
    checkProgramArguments.accept(startToken, method);
    checkNonExtendableMethod.accept(method, ctx);

  }

  private void checkAsServiceMethod(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    checkForImplementation.accept(ctx);
    checkNonExtendableMethod.accept(method, ctx);
  }

}
