package org.ek9lang.compiler.phase7;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.CallMetaDataExtractor;
import org.ek9lang.compiler.ir.IRConstruct;
import org.ek9lang.compiler.ir.Operation;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.IRGenerationContext;
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

  protected final OperationDfnGenerator operationDfnGenerator;
  protected final IRGenerationContext stackContext;
  protected final String voidStr;

  /**
   * Primary constructor using stack context - the single source of truth.
   */
  AbstractDfnGenerator(final IRGenerationContext stackContext) {
    this.stackContext = stackContext;
    this.operationDfnGenerator = new OperationDfnGenerator(stackContext);
    this.voidStr = stackContext.getParsedModule().getEk9Types().ek9Void().getFullyQualifiedName();
  }

  /**
   * Get the parsed module from stack context - the single source of truth.
   */
  protected ParsedModule getParsedModule() {
    return stackContext.getParsedModule();
  }

  /**
   * Create CallMetaDataExtractor using stack context - the single source of truth.
   */
  protected CallMetaDataExtractor createCallMetaDataExtractor() {
    return new CallMetaDataExtractor(getParsedModule().getEk9Types());
  }

  /**
   * Create DebugInfoCreator using stack context - the single source of truth.
   */
  protected DebugInfoCreator createDebugInfoCreator() {
    return new DebugInfoCreator(stackContext.getCurrentIRContext());
  }

  protected void processAsMethodOrOperator(final IRConstruct construct,
                                           final ISymbol symbol,
                                           final EK9Parser.OperationDetailsContext ctx) {

    AssertValue.checkNotNull("IRConstruct cannot be null", construct);
    AssertValue.checkNotNull("ISymbol cannot be null", symbol);
    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    if (symbol instanceof MethodSymbol method) {
      final var debugInfo = createDebugInfoCreator().apply(method.getSourceToken());
      final var operation = new Operation(method, debugInfo);
      
      // Use stack context for method-level coordination with fresh IRContext
      var methodDebugInfo = stackContext.createDebugInfo(method.getSourceToken());
      stackContext.enterMethodScope(method.getName(), methodDebugInfo, org.ek9lang.compiler.phase7.support.IRFrameType.METHOD);
      
      operationDfnGenerator.accept(operation, ctx);
      
      stackContext.exitScope();
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
    return new IRContext(stackContext.getCurrentIRContext());
  }

  protected Operation newSyntheticInitOperation(final IScope scope,
                                                final String methodName) {

    final var method = newSyntheticInitMethodSymbol(scope, methodName);
    final var debugInfo = createDebugInfoCreator().apply(method.getSourceToken());
    return new Operation(method, debugInfo);

  }

  private MethodSymbol newSyntheticInitMethodSymbol(final IScope scope,
                                                    final String methodName) {
    final var methodSymbol = new MethodSymbol(methodName, scope);
    final ISymbol returnType = stackContext.getParsedModule().getEk9Types().ek9Void();
    methodSymbol.setType(returnType);
    methodSymbol.setReturningSymbol(new VariableSymbol(IRConstants.RETURN_VARIABLE, returnType));
    methodSymbol.setMarkedPure(true); // Initialization methods are pure

    return methodSymbol;
  }
}
