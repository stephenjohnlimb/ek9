package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.CallInstruction;
import org.ek9lang.compiler.ir.IRInstruction;
import org.ek9lang.compiler.ir.MemoryInstruction;
import org.ek9lang.compiler.ir.ScopeInstruction;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.Symbol;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for object access expressions.
 * Handles constructor calls and method calls using resolved symbols.
 * Generates new BasicBlock IR (IRInstructions) instead of old Block IR (INode).
 */
public final class ObjectAccessInstructionCreator {

  private final IRGenerationContext context;
  private final DebugInfoCreator debugInfoCreator;

  public ObjectAccessInstructionCreator(final IRGenerationContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);
  }

  /**
   * Generate IR instructions for object access expression using resolved symbols.
   * This follows the pattern used in ObjectAccessCreator.
   */
  public List<IRInstruction> apply(final EK9Parser.ObjectAccessExpressionContext ctx, final String resultVar,
                                   final String scopeId) {
    AssertValue.checkNotNull("ObjectAccessExpressionContext cannot be null", ctx);
    AssertValue.checkNotNull("resultVar cannot be null", resultVar);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    final var instructions = new ArrayList<IRInstruction>();

    // Check if this is a simple constructor call (like Stdout())
    if (ctx.objectAccessStart() != null && ctx.objectAccessStart().call() != null) {
      // Get the resolved symbol for the call
      final var callSymbol = context.getParsedModule().getRecordedSymbol(ctx.objectAccessStart().call());

      if (callSymbol instanceof CallSymbol resolvedCallSymbol) {
        final var toBeCalled = resolvedCallSymbol.getResolvedSymbolToCall();

        if (toBeCalled instanceof MethodSymbol methodSymbol && methodSymbol.isConstructor()) {
          // For constructor, the parent scope is the aggregate type being constructed
          final var parentScope = methodSymbol.getParentScope();
          final var typeName =
              (parentScope instanceof Symbol symbol) ? symbol.getFullyQualifiedName() : parentScope.toString();

          // Extract debug info if debugging instrumentation is enabled
          final var debugInfo = debugInfoCreator.apply(callSymbol);

          // Generate constructor call using actual resolved type name
          instructions.add(CallInstruction.constructor(resultVar, typeName, debugInfo));

          // Add memory management for LLVM targets (no-ops on JVM)
          instructions.add(MemoryInstruction.retain(resultVar, debugInfo));
          instructions.add(ScopeInstruction.register(resultVar, scopeId, debugInfo));
        }
      }
    } else if (ctx.objectAccess() != null) {
      // This is a method call on an object - handle chained access
      instructions.addAll(generateObjectAccess(ctx, resultVar, scopeId));
    }

    return instructions;
  }

  /**
   * Generate IR instructions for chained object access (like stdout.println()).
   */
  private List<IRInstruction> generateObjectAccess(final EK9Parser.ObjectAccessExpressionContext ctx,
                                                   final String resultVar, final String scopeId) {
    final var instructions = new ArrayList<IRInstruction>();

    // Get the target variable (like stdout)
    String targetVar = null;
    if (ctx.objectAccessStart() != null && ctx.objectAccessStart().identifier() != null) {
      targetVar = ctx.objectAccessStart().identifier().getText();
    }

    // Get the method call information
    if (ctx.objectAccess() != null && ctx.objectAccess().objectAccessType() != null) {
      final var accessType = ctx.objectAccess().objectAccessType();

      if (accessType.operationCall() != null) {
        // Get the resolved symbol for the method call
        final var methodSymbol = context.getParsedModule().getRecordedSymbol(accessType.operationCall());

        if (methodSymbol instanceof CallSymbol callSymbol && targetVar != null) {
          final var toBeCalled = (MethodSymbol) callSymbol.getResolvedSymbolToCall();

          if (toBeCalled != null) {
            final var methodName = toBeCalled.getName();

            // Extract debug info if debugging instrumentation is enabled
            final var debugInfo = debugInfoCreator.apply(methodSymbol);

            // Load the target object
            final var tempObj = context.generateTempName();
            instructions.add(MemoryInstruction.load(tempObj, targetVar, debugInfo));

            // Extract arguments from the method call
            final var arguments = extractMethodArguments(accessType.operationCall());

            // Generate the method call
            instructions.add(CallInstruction.call(resultVar, tempObj, methodName, debugInfo, arguments));
          }
        }
      }
    }

    return instructions;
  }

  /**
   * Extract method arguments from operation call context.
   */
  private String[] extractMethodArguments(final EK9Parser.OperationCallContext ctx) {
    final var args = new ArrayList<String>();

    if (ctx.paramExpression() != null && !ctx.paramExpression().expressionParam().isEmpty()) {
      // Extract arguments from the parameter expression
      ctx.paramExpression().expressionParam().forEach(paramCtx ->
          args.add(paramCtx.expression().getText()));
    }

    return args.toArray(new String[0]);
  }

}