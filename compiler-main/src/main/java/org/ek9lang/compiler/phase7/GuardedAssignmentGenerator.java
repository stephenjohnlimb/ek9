package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.LabelInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Generates IR instructions for guarded assignment operations (:=? - ASSIGN_UNSET).
 * <p>
 * Guarded assignment only assigns to the left-hand side if:
 * 1. LHS is null/uninitialized (checked via IS_NULL opcode - backend-agnostic), OR
 * 2. LHS is not null but _isSet() returns false
 * </p>
 * <p>
 * This generator creates a conditional block structure:
 * 1. Check if LHS IS_NULL (backend handles platform-specific null checking)
 * 2. If not null, check if LHS._isSet() is false
 * 3. Only perform assignment if either condition is true
 * 4. Skip assignment otherwise
 * </p>
 */
final class GuardedAssignmentGenerator
    implements Function<GuardedAssignmentGenerator.GuardedAssignmentDetails, List<IRInstr>> {

  private final IRContext context;
  private final DebugInfoCreator debugInfoCreator;
  private final AssignExpressionToSymbol assignExpressionToSymbol;
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();

  public GuardedAssignmentGenerator(final IRContext context,
                                    final AssignExpressionToSymbol assignExpressionToSymbol) {
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);
    this.assignExpressionToSymbol = assignExpressionToSymbol;
  }

  @Override
  public List<IRInstr> apply(final GuardedAssignmentDetails details) {
    final var lhsSymbol = details.lhsSymbol();
    final var assignmentExpression = details.assignmentExpression();

    // Get debug information from the assignment expression
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(assignmentExpression);
    final var debugInfo = debugInfoCreator.apply(exprSymbol.getSourceToken());

    final var instructions = new ArrayList<IRInstr>();

    // Generate unique labels for conditional blocks
    final var guardStartLabel = context.generateLabelName("guard_start");
    final var assignmentBlockLabel = context.generateLabelName("assignment");
    final var unsetCheckBlockLabel = context.generateLabelName("unset_check");
    final var skipBlockLabel = context.generateLabelName("skip");

    // Step 1: Mark the start of guard logic for better backend mapping and debugging
    instructions.add(LabelInstr.label(guardStartLabel));

    // Step 2: Check if LHS IS_NULL (backend-agnostic null checking)
    final var nullCheckResult = context.generateTempName();
    final var lhsVariableName = lhsSymbol.getName();

    instructions.add(MemoryInstr.isNull(nullCheckResult, lhsVariableName, debugInfo));
    instructions.add(new IRInstr(IROpcode.BRANCH_TRUE, null, debugInfo)
        .addOperands(nullCheckResult, assignmentBlockLabel));

    // Step 3: LHS is not null, check if _isSet() is false
    instructions.add(LabelInstr.label(unsetCheckBlockLabel));

    final var setCheckResult = context.generateTempName();

    final var symbolTypeName = typeNameOrException.apply(lhsSymbol);

    final var isSetCallDetails = new CallDetails(lhsVariableName, symbolTypeName,
        "_isSet", List.of(), "org.ek9.lang::Boolean", List.of());
    instructions.add(CallInstr.call(setCheckResult, debugInfo, isSetCallDetails));
    instructions.add(MemoryInstr.retain(setCheckResult, debugInfo));
    instructions.add(ScopeInstr.register(setCheckResult, details.scopeId(), debugInfo));

    // Convert Boolean object to primitive boolean condition
    final var primitiveSetCheck = context.generateTempName();
    final var trueCallDetails = new CallDetails(setCheckResult, "org.ek9.lang::Boolean",
        "_true", List.of(), "org.ek9.lang::Boolean", List.of());
    instructions.add(CallInstr.call(primitiveSetCheck, debugInfo, trueCallDetails));

    // Branch to assignment if _isSet() is false (meaning variable is unset)
    instructions.add(new IRInstr(IROpcode.BRANCH_FALSE, null, debugInfo)
        .addOperands(primitiveSetCheck, assignmentBlockLabel));
    instructions.add(new IRInstr(IROpcode.BRANCH, null, debugInfo)
        .addOperand(skipBlockLabel));

    // Step 4: Assignment block - perform the actual assignment using existing logic
    instructions.add(LabelInstr.label(assignmentBlockLabel));
    instructions.addAll(assignExpressionToSymbol.apply(lhsSymbol, assignmentExpression));
    instructions.add(new IRInstr(IROpcode.BRANCH, null, debugInfo)
        .addOperand(skipBlockLabel));

    // Step 5: Skip block - continue execution
    instructions.add(LabelInstr.label(skipBlockLabel));

    return instructions;
  }

  /**
   * Data class to hold parameters for guarded assignment generation.
   */
  public record GuardedAssignmentDetails(
      ISymbol lhsSymbol,
      EK9Parser.AssignmentExpressionContext assignmentExpression,
      String scopeId
  ) {
  }
}