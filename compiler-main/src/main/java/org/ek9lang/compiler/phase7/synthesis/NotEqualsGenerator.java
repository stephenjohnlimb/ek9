package org.ek9lang.compiler.phase7.synthesis;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.LabelInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Generates synthetic IR for the _neq ({@literal <>}) operator.
 *
 * <p>The not-equals operator is implemented by delegating to _eq and negating the result.
 * This follows the principle that {@literal <>} should always be the logical negation of ==.</p>
 *
 * <p>The generated code follows this pattern:</p>
 * <pre>
 * Boolean _neq(T other):
 *   result = this._eq(other)
 *
 *   // If eq returned unset, we return unset
 *   if !result._isSet() -> return unset
 *
 *   // Negate the result: if eq was true, return false; if eq was false, return true
 *   if result._true() -> return false
 *   return true
 * </pre>
 *
 * <p>Key semantic requirements:</p>
 * <ul>
 *   <li>If _eq returns unset (either operand unset), return unset</li>
 *   <li>If _eq returns true (objects equal), return false</li>
 *   <li>If _eq returns false (objects not equal), return true</li>
 * </ul>
 *
 * <p>This approach ensures consistency: (a == b) and (a {@literal <>} b) are always
 * logical opposites, and both handle unset values identically.</p>
 */
final class NotEqualsGenerator extends AbstractSyntheticGenerator {

  private static final String RETURN_VAR = "rtn";
  private static final String OTHER_PARAM = "param";

  NotEqualsGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  /**
   * Generate the _neq operator IR for the given aggregate.
   *
   * @param operatorSymbol  The _neq operator method symbol
   * @param aggregateSymbol The aggregate containing the operator
   * @return List of IR instructions implementing the operator
   */
  List<IRInstr> generate(final MethodSymbol operatorSymbol,
                         final AggregateSymbol aggregateSymbol) {

    AssertValue.checkNotNull("operatorSymbol cannot be null", operatorSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = createDebugInfo(operatorSymbol);
    final var scopeId = stackContext.generateScopeId("_neq");
    final var aggregateTypeName = aggregateSymbol.getFullyQualifiedName();

    // Labels for control flow
    final var returnUnsetLabel = generateLabelName("return_unset");
    final var returnFalseLabel = generateLabelName("return_false");
    final var returnTrueLabel = generateLabelName("return_true");

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Reference return variable
    instructions.add(MemoryInstr.reference(RETURN_VAR, getBooleanTypeName(), debugInfo));

    // Call this._eq(param) to get equality result
    final var eqResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        eqResultVar,
        "this",
        aggregateTypeName,
        "_eq",
        List.of(OTHER_PARAM),
        List.of(aggregateTypeName),
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Check if eq result is set
    instructions.addAll(generateIsSetGuard(eqResultVar, getBooleanTypeName(), debugInfo,
        returnUnsetLabel, scopeId));

    // Check if eq result is true - if so, neq should return false
    instructions.addAll(generateTrueCheckWithNegation(eqResultVar, debugInfo,
        returnFalseLabel, returnTrueLabel, scopeId));

    // Return blocks
    instructions.addAll(generateUnsetReturnBlockWithLabel(returnUnsetLabel, debugInfo, scopeId));
    instructions.addAll(generateBooleanReturnBlockWithLabel(returnFalseLabel, false, debugInfo, scopeId));
    instructions.addAll(generateBooleanReturnBlockWithLabel(returnTrueLabel, true, debugInfo, scopeId));

    return instructions;
  }

  /**
   * Generate check if boolean variable is true, branch to falseLabel if true, trueLabel if false.
   * This inverts the logic for not-equals.
   */
  private List<IRInstr> generateTrueCheckWithNegation(final String booleanVar,
                                                       final DebugInfo debugInfo,
                                                       final String falseLabel,
                                                       final String trueLabel,
                                                       final String scopeId) {

    // Call _true() to get the primitive boolean value
    final var trueResultVar = generateTempName();
    final var instructions = new ArrayList<>(generateMethodCall(
        trueResultVar,
        booleanVar,
        getBooleanTypeName(),
        "_true",
        List.of(),
        List.of(),
        "boolean",
        debugInfo,
        scopeId
    ));

    // If eq was true, neq should be false - branch to false return
    instructions.add(BranchInstr.branchIfTrue(trueResultVar, falseLabel, debugInfo));

    // If eq was false, neq should be true - branch to true return
    instructions.add(BranchInstr.branch(trueLabel, debugInfo));

    return instructions;
  }

  /**
   * Generate the unset return block with a specific label.
   */
  private List<IRInstr> generateUnsetReturnBlockWithLabel(final String labelName,
                                                           final DebugInfo debugInfo,
                                                           final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label
    instructions.add(LabelInstr.label(labelName));

    // Create unset Boolean via constructor
    final var resultTemp = generateTempName();
    instructions.addAll(generateConstructorCall(resultTemp, getBooleanTypeName(), debugInfo, scopeId));

    // Store to return variable and retain for ownership transfer
    instructions.add(MemoryInstr.store(RETURN_VAR, resultTemp, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));

    // Scope cleanup and return
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(RETURN_VAR, debugInfo));

    return instructions;
  }

  /**
   * Generate a boolean return block with a specific label.
   */
  private List<IRInstr> generateBooleanReturnBlockWithLabel(final String labelName,
                                                             final boolean value,
                                                             final DebugInfo debugInfo,
                                                             final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label
    instructions.add(LabelInstr.label(labelName));

    // Create Boolean via Boolean._ofTrue() or _ofFalse()
    final var resultTemp = generateTempName();
    instructions.addAll(generateMethodCall(
        resultTemp,
        null,
        getBooleanTypeName(),
        value ? "_ofTrue" : "_ofFalse",
        List.of(),
        List.of(),
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Store to return variable and retain for ownership transfer
    instructions.add(MemoryInstr.store(RETURN_VAR, resultTemp, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));

    // Scope cleanup and return
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(RETURN_VAR, debugInfo));

    return instructions;
  }
}
