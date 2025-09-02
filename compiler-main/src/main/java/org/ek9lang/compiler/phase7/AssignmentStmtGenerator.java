package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
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
    BiFunction<EK9Parser.AssignmentStatementContext, String, List<IRInstr>> {

  AssignmentStmtGenerator(final IRContext context) {
    super(context);

  }

  @Override
  public List<IRInstr> apply(final EK9Parser.AssignmentStatementContext ctx, final String scopeId) {

    AssertValue.checkNotNull("Ctx cannot be null", ctx);
    AssertValue.checkNotNull("ScopeId cannot be null", scopeId);

    final var instructions = new ArrayList<IRInstr>();
    if (ctx.primaryReference() != null) {
      throw new CompilerException("PrimaryReference assignment not implemented");
    } else if (ctx.identifier() != null) {
      processIdentifierAssignment(ctx, scopeId, instructions);
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
                                           final String scopeId, final List<IRInstr> instructions) {

    final var lhsSymbol = getRecordedSymbolOrException(ctx.identifier());

    final var generator = new AssignmentExprInstrGenerator(context, ctx.assignmentExpression(), scopeId);

    if (isMethodBasedAssignment(ctx.op)) {
      processMethodBasedAssignment(ctx, lhsSymbol, scopeId, instructions);
      return; // Early return since method-based assignment is complete
    }
    final var assignExpressionToSymbol = new AssignExpressionToSymbol(context, true, generator, scopeId);

    if (isGuardedAssignment(ctx.op)) {

      final var guardedDetails = new GuardedAssignmentGenerator.GuardedAssignmentDetails(
          lhsSymbol, ctx.assignmentExpression(), scopeId);

      final var guardedGenerator = createGuardedGenerator(generator, assignExpressionToSymbol);
      instructions.addAll(guardedGenerator.apply(guardedDetails));
      return; // Early return since guarded assignment is complete
    }

    //So it is a simple assignment.
    instructions.addAll(assignExpressionToSymbol.apply(lhsSymbol, ctx.assignmentExpression()));

  }

  private GuardedAssignmentGenerator createGuardedGenerator(final AssignmentExprInstrGenerator generator,
                                                            final AssignExpressionToSymbol assignExpressionToSymbol) {

    final Function<ExprProcessingDetails, List<IRInstr>>
        expressionProcessor = details -> generator.apply(details.variableDetails().resultVariable());
    final var questionBlockGenerator = new QuestionBlockGenerator(context, expressionProcessor);
    return new GuardedAssignmentGenerator(context, questionBlockGenerator, assignExpressionToSymbol);

  }

  private void processMethodBasedAssignment(final EK9Parser.AssignmentStatementContext ctx,
                                            final ISymbol lhsSymbol,
                                            final String scopeId,
                                            final List<IRInstr> instructions) {

    // Get assignment operator token for correct debug line numbers
    final var assignmentToken = new org.ek9lang.compiler.tokenizer.Ek9Token(ctx.op);
    final var debugInfo = debugInfoCreator.apply(assignmentToken);
    final var basicDetails = new BasicDetails(scopeId, debugInfo);

    // Get method name from operator map (e.g., "+=" -> "_addAss")
    final var operatorMap = new org.ek9lang.compiler.common.OperatorMap();
    final var methodName = operatorMap.getForward(ctx.op.getText());

    // Process left operand with proper memory management
    final var leftTemp = context.generateTempName();
    final var leftDetails = new VariableDetails(leftTemp, basicDetails);
    final var leftLoadInstr = MemoryInstr.load(leftTemp, lhsSymbol.getName(), debugInfo);
    final var variableMemoryManagement = new org.ek9lang.compiler.phase7.support.VariableMemoryManagement();
    final var leftInstructions = variableMemoryManagement.apply(() -> {
      final var list = new ArrayList<IRInstr>();
      list.add(leftLoadInstr);
      return list;
    }, leftDetails);
    instructions.addAll(leftInstructions);

    // Process right operand with proper memory management
    final var rightTemp = context.generateTempName();
    final var rightDetails = new VariableDetails(rightTemp, basicDetails);
    final var rightEvaluation = processAssignmentExpression(ctx.assignmentExpression(), rightDetails);
    instructions.addAll(variableMemoryManagement.apply(() -> rightEvaluation, rightDetails));

    // Get operand types and resolve actual return type from method
    final var rightSymbol = getRecordedSymbolOrException(ctx.assignmentExpression());
    final var typeNameOrException = new org.ek9lang.compiler.common.TypeNameOrException();
    final var leftType = typeNameOrException.apply(lhsSymbol);
    final var rightType = typeNameOrException.apply(rightSymbol);

    // Resolve actual return type of assignment method (may be Void for mutating operators)
    final var returnType = resolveBinaryMethodReturnType(lhsSymbol, rightSymbol, ctx.op.getText());
    
    // Assignment operators MUST return Void - enforced by ValidOperatorOrError semantic rules
    AssertValue.checkTrue("Assignment operator " + ctx.op.getText() + " must return Void, got: " + returnType,
        "org.ek9.lang::Void".equals(returnType));

    // Create CallDetails for assignment operation: leftVariable._addAss(rightValue)
    final var callDetails = new org.ek9lang.compiler.ir.CallDetails(
        leftTemp, leftType, methodName,
        List.of(rightType), returnType, List.of(rightTemp));

    // Assignment operators return Void - generate call without result variable
    // The method mutates the left operand in-place
    final var operatorDebugInfo = debugInfoCreator.apply(new Ek9Token(ctx.op));
    instructions.add(org.ek9lang.compiler.ir.CallInstr.operator(null, operatorDebugInfo, callDetails));
  }

  /**
   * Resolve the return type of a binary method by looking up the actual method on the left operand type.
   * Uses the same pattern as BinaryOperationGenerator.
   */
  private String resolveBinaryMethodReturnType(final ISymbol leftSymbol,
                                               final ISymbol rightSymbol,
                                               final String methodName) {

    // Create method search for binary operation (one parameter)
    final var search = new org.ek9lang.compiler.search.MethodSymbolSearch(methodName);
    search.addTypeParameter(rightSymbol.getType());

    // Get left operand type symbol for method resolution
    final var leftTypeSymbol = leftSymbol.getType().orElse(null);
    if (leftTypeSymbol instanceof org.ek9lang.compiler.symbols.AggregateSymbol aggregate) {

      // Resolve method on the left operand type
      final var results = aggregate.resolveMatchingMethods(search,
          new org.ek9lang.compiler.search.MethodSymbolSearchResult());
      final var bestMatch = results.getSingleBestMatchSymbol();

      if (bestMatch.isPresent()) {
        final var method = bestMatch.get();
        final var returningSymbol = method.getReturningSymbol();
        if (returningSymbol.getType().isPresent()) {
          final var typeNameOrException = new org.ek9lang.compiler.common.TypeNameOrException();
          return typeNameOrException.apply(returningSymbol);
        }
      }
    }
    throw new CompilerException("Must be able to resolve method for assignment operator");
  }

  private List<IRInstr> processAssignmentExpression(final EK9Parser.AssignmentExpressionContext assignExprCtx,
                                                    final VariableDetails variableDetails) {

    // Use existing assignment expression generator to handle the right-hand side
    final var generator =
        new AssignmentExprInstrGenerator(context, assignExprCtx, variableDetails.basicDetails().scopeId());
    return generator.apply(variableDetails.resultVariable());
  }
}
