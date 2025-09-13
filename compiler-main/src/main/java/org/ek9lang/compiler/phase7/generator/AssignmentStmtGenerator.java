package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.calls.CallDetailsBuilder;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Process assignment statement: variable = expression
 * Uses RELEASE-then-RETAIN pattern for memory-safe assignments.
 * Handles assignments like someLocal = "Hi" and cross-scope assignments like rtn: claude.
 * For property fields, uses "this.fieldName" naming convention.
 * <p>
 * From the ANTLR grammar, we're looking at processing this.
 * </p>
 * <pre>
 *   assignmentStatement
 *     : (primaryReference | identifier | objectAccessExpression) op=(ASSIGN | ASSIGN2 | COLON |
 *     ASSIGN_UNSET | ADD_ASSIGN | SUB_ASSIGN | DIV_ASSIGN | MUL_ASSIGN | MERGE | REPLACE | COPY) assignmentExpression
 *     ;
 * </pre>
 */
final class AssignmentStmtGenerator extends AbstractGenerator implements
    Function<EK9Parser.AssignmentStatementContext, List<IRInstr>> {

  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();
  private final VariableMemoryManagement variableMemoryManagement;
  private final OperatorMap operatorMap = new OperatorMap();

  AssignmentStmtGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
    this.variableMemoryManagement = new VariableMemoryManagement(stackContext);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.AssignmentStatementContext ctx) {

    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    final var instructions = new ArrayList<IRInstr>();
    if (ctx.primaryReference() != null) {
      throw new CompilerException("PrimaryReference assignment not implemented");
    } else if (ctx.identifier() != null) {
      processIdentifierAssignment(ctx, instructions);
    } else if (ctx.objectAccessExpression() != null) {
      throw new CompilerException("ObjectAccessExpression assignment not implemented");
    }

    return instructions;

  }

  private boolean isNotSimpleAssignment(final Token op) {
    return op.getType() != EK9Parser.ASSIGN
        && op.getType() != EK9Parser.ASSIGN2
        && op.getType() != EK9Parser.COLON;
  }

  private boolean isGuardedAssignment(final Token op) {
    return op.getType() == EK9Parser.ASSIGN_UNSET;
  }

  private boolean isMethodBasedAssignment(final Token op) {
    return isNotSimpleAssignment(op) && !isGuardedAssignment(op);
  }

  private void processIdentifierAssignment(final EK9Parser.AssignmentStatementContext ctx,
                                           final List<IRInstr> instructions) {

    final var lhsSymbol = getRecordedSymbolOrException(ctx.identifier());

    // STACK-BASED: AssignmentExprInstrGenerator now uses stack context directly 
    final var generator = new AssignmentExprInstrGenerator(stackContext, ctx.assignmentExpression());

    if (isMethodBasedAssignment(ctx.op)) {
      processMethodBasedAssignment(ctx, lhsSymbol, instructions);
      return; // Early return since method-based assignment is complete
    }
    // STACK-BASED: AssignExpressionToSymbol now uses stack context directly
    final var assignExpressionToSymbol = new AssignExpressionToSymbol(stackContext, true, generator);

    if (isGuardedAssignment(ctx.op)) {

      final var guardedDetails = new GuardedAssignmentGenerator.GuardedAssignmentDetails(
          lhsSymbol, ctx.assignmentExpression());

      final var guardedGenerator = createGuardedGenerator(generator, assignExpressionToSymbol);
      instructions.addAll(guardedGenerator.apply(guardedDetails));
      return; // Early return since guarded assignment is complete
    }

    //So it is a simple assignment.
    instructions.addAll(assignExpressionToSymbol.apply(lhsSymbol, ctx.assignmentExpression()));

  }

  private GuardedAssignmentGenerator createGuardedGenerator(final AssignmentExprInstrGenerator generator,
                                                            final AssignExpressionToSymbol assignExpressionToSymbol) {

    return new GuardedAssignmentGenerator(stackContext, assignExpressionToSymbol);

  }

  private void processMethodBasedAssignment(final EK9Parser.AssignmentStatementContext ctx,
                                            final ISymbol lhsSymbol,
                                            final List<IRInstr> instructions) {

    // Get assignment operator token for correct debug line numbers
    final var debugInfo = stackContext.createDebugInfo(ctx.op);
    final var basicDetails = new BasicDetails(debugInfo);

    // Get method name from operator map (e.g., "+=" -> "_addAss")
    final var methodName = operatorMap.getForward(ctx.op.getText());

    // Process left operand with proper memory management
    final var leftTemp = stackContext.generateTempName();
    final var leftDetails = new VariableDetails(leftTemp, basicDetails);
    final var leftLoadInstr = MemoryInstr.load(leftTemp, lhsSymbol.getName(), debugInfo);
    final var leftInstructions = variableMemoryManagement.apply(() -> {
      final var list = new ArrayList<IRInstr>();
      list.add(leftLoadInstr);
      return list;
    }, leftDetails);
    instructions.addAll(leftInstructions);

    // Process right operand with proper memory management
    final var rightTemp = stackContext.generateTempName();
    final var rightDetails = new VariableDetails(rightTemp, basicDetails);
    final var rightEvaluation = processAssignmentExpression(ctx.assignmentExpression(), rightDetails);
    instructions.addAll(variableMemoryManagement.apply(() -> rightEvaluation, rightDetails));

    // Get operand symbols for method resolution
    final var rightSymbol = getRecordedSymbolOrException(ctx.assignmentExpression());

    // Create call context for cost-based resolution
    final var callContext = CallContext.forBinaryOperation(
        symbolTypeOrException.apply(lhsSymbol),     // Target type (left operand type)
        symbolTypeOrException.apply(rightSymbol),   // Argument type (right operand type)
        methodName,                                  // Method name (from operator map)
        leftTemp,                                   // Target variable (left operand variable)
        rightTemp,                                  // Argument variable (right operand variable)
        stackContext.currentScopeId()              // STACK-BASED: Get scope ID from current stack frame
    );

    // Use CallDetailsBuilder for cost-based method resolution and promotion
    final var callDetailsBuilder = new CallDetailsBuilder(stackContext);
    final var callDetailsResult = callDetailsBuilder.apply(callContext);

    // Add any promotion instructions that were generated
    instructions.addAll(callDetailsResult.allInstructions());

    // Assignment operators MUST return Void - enforced by ValidOperatorOrError semantic rules
    final var returnType = callDetailsResult.callDetails().returnTypeName();
    AssertValue.checkTrue("Assignment operator " + ctx.op.getText() + " must return Void, got: " + returnType,
        "org.ek9.lang::Void".equals(returnType));

    // Assignment operators return Void - generate call without result variable
    // The method mutates the left operand in-place
    final var operatorDebugInfo = stackContext.createDebugInfo(ctx.op);
    instructions.add(CallInstr.operator(null, operatorDebugInfo, callDetailsResult.callDetails()));
  }


  private List<IRInstr> processAssignmentExpression(final EK9Parser.AssignmentExpressionContext assignExprCtx,
                                                    final VariableDetails variableDetails) {

    // Use existing assignment expression generator to handle the right-hand side
    // STACK-BASED: AssignmentExprInstrGenerator now uses stack context directly
    final var generator = new AssignmentExprInstrGenerator(stackContext, assignExprCtx);
    return generator.apply(variableDetails.resultVariable());
  }
}
