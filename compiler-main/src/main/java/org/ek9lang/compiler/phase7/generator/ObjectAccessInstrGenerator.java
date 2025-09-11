package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.support.CallMetaDataExtractor;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.Symbol;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for object access expressions.
 * Handles constructor calls and method calls using resolved symbols.
 * Generates new BasicBlock IR (IRInstructions).
 */
final class ObjectAccessInstrGenerator extends AbstractGenerator {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();

  ObjectAccessInstrGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
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
              parameterTypes, typeName, List.of(), constructorMetaData);

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

            // Extract debug info if debugging instrumentation is enabled
            final var debugInfo = debugInfoCreator.apply(methodSymbol.getSourceToken());

            // Extract type information from resolved MethodSymbol
            final var parentScope = toBeCalled.getParentScope();
            final var targetTypeName = (parentScope instanceof ISymbol symbol)
                ? symbol.getFullyQualifiedName() : parentScope.toString();
            final var returnTypeName = typeNameOrException.apply(toBeCalled);
            final var parameterTypes = toBeCalled.getCallParameters().stream()
                .map(typeNameOrException)
                .toList();

            // Load the target object
            final var tempObj = stackContext.generateTempName();
            instructions.add(MemoryInstr.load(tempObj, targetVar, debugInfo));

            // Extract arguments from the method call
            final var arguments = extractMethodArguments(accessType.operationCall());

            // Generate the method call with complete type information
            final var callDetails = new CallDetails(tempObj, targetTypeName, methodName,
                parameterTypes, returnTypeName, Arrays.asList(arguments),
                CallMetaDataDetails.defaultMetaData());

            instructions.add(CallInstr.call(variableDetails.resultVariable(), debugInfo, callDetails));
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