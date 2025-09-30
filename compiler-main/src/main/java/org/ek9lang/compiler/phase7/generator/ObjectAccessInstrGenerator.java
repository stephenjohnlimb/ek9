package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.support.CallMetaDataExtractor;
import org.ek9lang.compiler.phase7.calls.CallDetailsBuilder;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.Symbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for object access expressions.
 * Handles constructor calls and method calls using resolved symbols.
 * Generates new BasicBlock IR (IRInstructions).
 */
final class ObjectAccessInstrGenerator extends AbstractGenerator {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final VariableMemoryManagement variableMemoryManagement;
  private final Function<ExprProcessingDetails, List<IRInstr>> exprProcessor;
  private final CallDetailsBuilder callDetailsBuilder;

  ObjectAccessInstrGenerator(final IRGenerationContext stackContext,
                             final Function<ExprProcessingDetails, List<IRInstr>> exprProcessor) {
    super(stackContext);
    this.variableMemoryManagement = new VariableMemoryManagement(stackContext);
    this.exprProcessor = exprProcessor;
    this.callDetailsBuilder = new CallDetailsBuilder(stackContext);
  }

  /**
   * Generate IR instructions for object access expression using resolved symbols.
   * This follows the pattern used in ObjectAccessCreator.
   */
  public List<IRInstr> apply(final EK9Parser.ObjectAccessExpressionContext ctx, final VariableDetails variableDetails) {

    AssertValue.checkNotNull("ObjectAccessExpressionContext cannot be null", ctx);
    AssertValue.checkNotNull("variableDetails cannot be null", variableDetails);

    final var instructions = new ArrayList<IRInstr>();

    // Check if this is a simple constructor call (like Stdout())
    if (ctx.objectAccessStart() != null && ctx.objectAccessStart().call() != null) {
      // Get the resolved symbol for the call
      final var callSymbol = getRecordedSymbolOrException(ctx.objectAccessStart().call());

      if (callSymbol instanceof CallSymbol resolvedCallSymbol) {
        final var toBeCalled = resolvedCallSymbol.getResolvedSymbolToCall();

        if (toBeCalled instanceof MethodSymbol methodSymbol && methodSymbol.isConstructor()) {
          // For constructor, the parent scope is the aggregate type being constructed
          final var parentScope = methodSymbol.getParentScope();
          final var typeName =
              (parentScope instanceof Symbol symbol) ? symbol.getFullyQualifiedName() : parentScope.toString();

          // Extract debug info if debugging instrumentation is enabled
          final var debugInfo = debugInfoCreator.apply(callSymbol.getSourceToken());

          // Extract parameter types from constructor parameters
          final var parameterTypes = methodSymbol.getCallParameters().stream()
              .map(typeNameOrException)
              .toList();

          // Create metadata for constructor call
          final var metaDataExtractor = new CallMetaDataExtractor(stackContext.getParsedModule().getEk9Types());
          final var constructorMetaData = metaDataExtractor.apply(methodSymbol);

          // Generate constructor call using actual resolved type name with complete type information
          final var callDetails = new CallDetails(typeName, typeName, IRConstants.INIT_METHOD,
              parameterTypes, typeName, List.of(), constructorMetaData, false);

          instructions.add(CallInstr.constructor(variableDetails.resultVariable(), debugInfo, callDetails));

        }
      }
    } else if (ctx.objectAccess() != null) {
      // This is a method call on an object - handle chained access
      instructions.addAll(generateObjectAccess(ctx, variableDetails));
    }

    return instructions;
  }

  /**
   * Generate IR instructions for chained object access (like stdout.println()).
   */
  private List<IRInstr> generateObjectAccess(final EK9Parser.ObjectAccessExpressionContext ctx,
                                             final VariableDetails variableDetails) {
    final var instructions = new ArrayList<IRInstr>();

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
        final var methodSymbol = getRecordedSymbolOrException(accessType.operationCall());

        if (methodSymbol instanceof CallSymbol callSymbol && targetVar != null) {
          final var toBeCalled = (MethodSymbol) callSymbol.getResolvedSymbolToCall();

          if (toBeCalled != null) {
            final var methodName = toBeCalled.getName();

            final var objectDebugInfo = debugInfoCreator.apply(new Ek9Token(ctx.objectAccessStart().start));
            // Extract debug info if debugging instrumentation is enabled
            final var methodDebugInfo = debugInfoCreator.apply(methodSymbol.getSourceToken());

            // Load the target object
            final var tempObj = stackContext.generateTempName();
            instructions.add(MemoryInstr.load(tempObj, targetVar, objectDebugInfo));
            final var argDetails = new VariableDetails(tempObj, objectDebugInfo);
            variableMemoryManagement.apply(() -> instructions, argDetails);

            // Extract arguments from the method call
            final var arguments = extractMethodArguments(accessType.operationCall(), instructions);

            // Get target type from method's parent scope (this is the type the method was resolved on)
            final var parentScope = toBeCalled.getParentScope();
            final var targetType = (parentScope instanceof ISymbol symbol) ? symbol : null;

            if (targetType == null) {
              throw new org.ek9lang.core.CompilerException(
                  "Method parent scope must be a symbol, but got: " + parentScope.getClass().getSimpleName());
            }

            // Extract type information for CallDetails
            final var targetTypeName = targetType.getFullyQualifiedName();
            final var returnTypeName = typeNameOrException.apply(toBeCalled);
            final var parameterTypes = toBeCalled.getCallParameters().stream()
                .map(typeNameOrException)
                .toList();

            // Check if this is a trait call (requires invokeinterface in JVM bytecode)
            final var isTraitCall = targetType.getGenus() == org.ek9lang.compiler.symbols.SymbolGenus.CLASS_TRAIT;

            // Extract metadata from the resolved method
            final var metaDataExtractor = new org.ek9lang.compiler.ir.support.CallMetaDataExtractor(
                stackContext.getParsedModule().getEk9Types());
            final var metaData = metaDataExtractor.apply(toBeCalled);

            // Generate the method call with complete type information including trait flag
            final var callDetails = new CallDetails(tempObj, targetTypeName, methodName,
                parameterTypes, returnTypeName, java.util.Arrays.asList(arguments),
                metaData, isTraitCall);

            // Only assign result variable for non-void returning methods
            if ("org.ek9.lang::Void".equals(returnTypeName)) {
              // Void-returning method - don't assign to any variable
              instructions.add(CallInstr.call(null, methodDebugInfo, callDetails));
            } else {
              // Non-void returning method - assign to result variable
              instructions.add(CallInstr.call(variableDetails.resultVariable(), methodDebugInfo, callDetails));
            }
          }
        }
      }
    }

    return instructions;
  }

  /**
   * Extract method arguments from operation call context using proper expression processing.
   * This generates LOAD_LITERAL instructions for literals and proper memory management.
   */
  private String[] extractMethodArguments(final EK9Parser.OperationCallContext ctx,
                                          final List<IRInstr> instructions) {
    final var args = new ArrayList<String>();

    if (ctx.paramExpression() != null && !ctx.paramExpression().expressionParam().isEmpty()) {
      // Process each argument expression using ExprInstrGenerator (like CallInstrGenerator.processParameters)
      for (var exprParam : ctx.paramExpression().expressionParam()) {
        final var exprCtx = exprParam.expression();
        final var argTemp = stackContext.generateTempName();
        final var argDebugInfo = debugInfoCreator.apply(new Ek9Token(exprCtx.start));
        final var argDetails = new VariableDetails(argTemp, argDebugInfo);

        // Generate instructions to evaluate the argument expression
        final var exprDetails = new ExprProcessingDetails(exprCtx, argDetails);
        final var argEvaluation = exprProcessor != null ? exprProcessor.apply(exprDetails) : new ArrayList<IRInstr>();

        // Add variable memory management (RETAIN, SCOPE_REGISTER)
        final var argInstructions = variableMemoryManagement.apply(
            () -> argEvaluation, argDetails);
        instructions.addAll(argInstructions);

        // Collect the temp variable name (not the literal value)
        args.add(argTemp);
      }
    }

    return args.toArray(new String[0]);
  }

}