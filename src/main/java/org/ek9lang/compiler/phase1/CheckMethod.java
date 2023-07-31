package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.common.CheckIfContextSupportsAbstractMethod;
import org.ek9lang.compiler.common.CheckNonTraitMethod;
import org.ek9lang.compiler.common.CheckTraitMethod;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks methods from various contexts, typically this is delegated to other functions.
 * Those functions do the detail check.
 */
final class CheckMethod extends RuleSupport implements BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {

  private final CommonMethodChecks commonMethodChecks;
  private final CheckTraitMethod checkTraitMethod;
  private final CheckNonTraitMethod checkNonTraitMethod;
  private final CheckIfContextSupportsAbstractMethod checkIfContextSupportsAbstractMethod;
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
    checkNonTraitMethod = new CheckNonTraitMethod(errorListener);
    checkIfContextSupportsAbstractMethod =
        new CheckIfContextSupportsAbstractMethod(symbolAndScopeManagement, errorListener);
    checkNonExtendableMethod = new CheckNonExtendableMethod(errorListener);
    checkTraitMethod = new CheckTraitMethod();
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
      checkTraitMethod.accept(method, ctx.operationDetails());
    }

    //If not in a class then method must not be marked as dispatcher.
    if (!(ctx.getParent().getParent() instanceof EK9Parser.ClassDeclarationContext)
        && !(ctx.getParent().getParent() instanceof EK9Parser.DynamicClassDeclarationContext)) {
      checkNotDispatcherMethod.accept(method, ctx);
    }

    if (!(ctx.getParent().getParent() instanceof EK9Parser.TraitDeclarationContext)) {
      checkNonTraitMethod.accept(method, ctx.operationDetails());
    }

    commonMethodChecks.accept(method, ctx);

    checkIfContextSupportsAbstractMethod.accept(method, ctx);

    checkMethodNotOperatorName.accept(method, ctx);
  }

  private void checkAsConstructor(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {
    checkNormalTermination.accept(ctx.start, method);
    checkGenericConstructor.accept(ctx.start, method);
    checkNoMethodReturn.accept(method, ctx);
  }

  private void checkAsProgram(MethodSymbol method, EK9Parser.MethodDeclarationContext ctx) {
    checkProgramReturns.accept(ctx.start, method);
    checkProgramArguments.accept(ctx.start, method);
    checkNonExtendableMethod.accept(method, ctx);
  }

  private void checkAsServiceMethod(MethodSymbol method, EK9Parser.MethodDeclarationContext ctx) {
    checkForImplementation.accept(ctx);
    checkNonExtendableMethod.accept(method, ctx);
  }
}
