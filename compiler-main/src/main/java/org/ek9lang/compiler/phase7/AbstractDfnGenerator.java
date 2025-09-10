package org.ek9lang.compiler.phase7;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Acts as a base for the main construct definitions.
 * As there are lots of common processing aspects across constructs and, they are
 * only really very focussed on IR generation (and not reusable elsewhere) hence defined in this base.
 */
abstract class AbstractDfnGenerator {

  protected OperationDfnGenerator operationDfnGenerator;
  protected final IRContext irContext;
  protected final String voidStr;

  AbstractDfnGenerator(final IRContext irContext) {
    this.irContext = irContext;
    this.voidStr = irContext.getParsedModule().getEk9Types().ek9Void().getFullyQualifiedName();
  }

  protected void processAsMethodOrOperator(final IRConstruct construct,
                                           final ISymbol symbol,
                                           final EK9Parser.OperationDetailsContext ctx) {

    AssertValue.checkNotNull("IRConstruct cannot be null", construct);
    AssertValue.checkNotNull("ISymbol cannot be null", symbol);
    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    if (symbol instanceof MethodSymbol method) {
      final var debugInfo =
          new DebugInfoCreator(irContext).apply(method.getSourceToken());
      final var operation = new Operation(method, debugInfo);
      operationDfnGenerator.accept(operation, ctx);
      construct.add(operation);
    } else {
      throw new CompilerException("Expecting a Method Symbol");
    }
  }

  /**
   * Create a new IRContext for per-construct isolation.
   * This ensures each construct gets fresh block labeling (_entry_1, _temp_1, etc.)
   * while sharing the core parsedModule and compilerFlags.
   */
  protected IRContext newPerConstructContext() {
    return new IRContext(irContext);
  }

  protected Operation newSyntheticInitOperation(final IRContext context,
                                                final IScope scope,
                                                final String methodName) {

    final var method = newSyntheticInitMethodSymbol(scope, methodName);
    final var debugInfo = new DebugInfoCreator(context).apply(method.getSourceToken());
    return new Operation(method, debugInfo);

  }

  private MethodSymbol newSyntheticInitMethodSymbol(final IScope scope,
                                                    final String methodName) {
    final var methodSymbol = new MethodSymbol(methodName, scope);
    final ISymbol returnType = irContext.getParsedModule().getEk9Types().ek9Void();
    methodSymbol.setType(returnType);
    methodSymbol.setReturningSymbol(new VariableSymbol(IRConstants.RETURN_VARIABLE, returnType));
    methodSymbol.setMarkedPure(true); // Initialization methods are pure

    return methodSymbol;
  }
}
