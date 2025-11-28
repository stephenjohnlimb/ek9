package org.ek9lang.compiler.phase7.synthesis;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.LabelInstr;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Generates synthetic IR for the _cmp ({@literal <=>}) operator.
 *
 * <p>The generated code follows this pattern:</p>
 * <pre>
 * Integer _cmp(T other):
 *   // Guard: check this is set
 *   if !this._isSet() -> return unset
 *
 *   // Guard: check other is set
 *   if !other._isSet() -> return unset
 *
 *   // Super check (if super is not Any)
 *   if super != Any:
 *     result = super._cmp(other)
 *     if !result._isSet() -> return unset
 *     if result != 0 -> return result
 *
 *   // Field-by-field comparison (in declaration order)
 *   for each field in properties:
 *     result = this.field._cmp(other.field)
 *     if !result._isSet() -> return unset
 *     if result != 0 -> return result
 *
 *   return 0  // All fields equal
 * </pre>
 *
 * <p>Key semantic requirements:</p>
 * <ul>
 *   <li>If this or other is unset, return unset</li>
 *   <li>If any field comparison returns unset, return unset</li>
 *   <li>If any field comparison returns non-zero, return that value (short-circuit)</li>
 *   <li>Only if all comparisons return zero, return zero</li>
 *   <li>Super's _cmp is called first if super is not Any</li>
 *   <li>Fields are compared in declaration order</li>
 * </ul>
 */
final class CompareGenerator extends AbstractSyntheticGenerator {

  private static final String RETURN_VAR = "rtn";
  private static final String OTHER_PARAM = "other";

  CompareGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  /**
   * Generate the _cmp operator IR for the given aggregate.
   *
   * @param operatorSymbol  The _cmp operator method symbol
   * @param aggregateSymbol The aggregate containing the operator
   * @return List of IR instructions implementing the operator
   */
  List<IRInstr> generate(final MethodSymbol operatorSymbol,
                         final AggregateSymbol aggregateSymbol) {

    AssertValue.checkNotNull("operatorSymbol cannot be null", operatorSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = createDebugInfo(operatorSymbol);
    final var scopeId = stackContext.generateScopeId("_cmp");

    // Labels for control flow - generate ONCE and reuse
    final var returnUnsetLabel = generateLabelName("return_unset");
    final var returnZeroLabel = generateLabelName("return_zero");

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Reference return variable
    instructions.add(MemoryInstr.reference(RETURN_VAR, getIntegerTypeName(), debugInfo));

    // Generate isSet guards for this and other
    instructions.addAll(generateThisIsSetGuard(debugInfo, returnUnsetLabel, scopeId));
    instructions.addAll(generateIsSetGuard(OTHER_PARAM, debugInfo, returnUnsetLabel, scopeId));

    // Generate super._cmp check if super is not Any
    instructions.addAll(generateSuperCmpCheck(aggregateSymbol, debugInfo, scopeId,
        returnUnsetLabel));

    // Generate field-by-field comparison
    final var fields = getSyntheticFields(aggregateSymbol);
    for (final var field : fields) {
      instructions.addAll(generateFieldCmpCheck(field, debugInfo, scopeId,
          returnUnsetLabel));
    }

    // All checks passed (all returned zero) - branch to return zero
    instructions.add(BranchInstr.branch(returnZeroLabel, debugInfo));

    // Return blocks
    instructions.addAll(generateUnsetReturnBlockWithLabel(returnUnsetLabel, getIntegerTypeName(),
        RETURN_VAR, debugInfo, scopeId));
    instructions.addAll(generateZeroReturnBlockWithLabel(returnZeroLabel,
        RETURN_VAR, debugInfo, scopeId));

    return instructions;
  }

  /**
   * Generate super._cmp check if super is not Any.
   */
  private List<IRInstr> generateSuperCmpCheck(final AggregateSymbol aggregateSymbol,
                                               final DebugInfo debugInfo,
                                               final String scopeId,
                                               final String returnUnsetLabel) {

    final var instructions = new ArrayList<IRInstr>();

    // Get super aggregate if present
    final var superOpt = aggregateSymbol.getSuperAggregate();
    if (superOpt.isEmpty()) {
      return instructions;
    }

    final var superAggregate = superOpt.get();

    // Skip if super is Any
    if (isAnyType(superAggregate)) {
      return instructions;
    }

    // Call super._cmp(other)
    final var superResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        superResultVar,
        IRConstants.SUPER,
        "_cmp",
        List.of(OTHER_PARAM),
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Check if super result is set
    instructions.addAll(generateIsSetGuard(superResultVar, debugInfo, returnUnsetLabel, scopeId));

    // Check if super result is non-zero - if so, return it
    instructions.addAll(generateNonZeroCheck(superResultVar, debugInfo, scopeId));

    return instructions;
  }

  /**
   * Generate field comparison check.
   */
  private List<IRInstr> generateFieldCmpCheck(final ISymbol field,
                                               final DebugInfo debugInfo,
                                               final String scopeId,
                                               final String returnUnsetLabel) {

    final var fieldName = field.getName();

    // Load this.field
    final var thisFieldVar = generateTempName();
    final var instructions = new ArrayList<>(generateFieldLoad(thisFieldVar, IRConstants.THIS, fieldName,
        debugInfo, scopeId));

    // Load other.field
    final var otherFieldVar = generateTempName();
    instructions.addAll(generateFieldLoad(otherFieldVar, OTHER_PARAM, fieldName,
        debugInfo, scopeId));

    // Call thisField._cmp(otherField)
    final var cmpResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        cmpResultVar,
        thisFieldVar,
        "_cmp",
        List.of(otherFieldVar),
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Check if result is set
    instructions.addAll(generateIsSetGuard(cmpResultVar, debugInfo, returnUnsetLabel, scopeId));

    // Check if result is non-zero - if so, return it immediately
    instructions.addAll(generateNonZeroCheck(cmpResultVar, debugInfo, scopeId));

    return instructions;
  }

  /**
   * Generate check if Integer variable is non-zero. If non-zero, return immediately.
   *
   * <p>Pattern:</p>
   * <pre>
   *   _zero = LOAD_LITERAL 0 -> Integer
   *   _isZero = CALL cmpResult._eq(_zero) -> Boolean
   *   _isZeroSet = CALL _isZero._isSet() -> Boolean
   *   BRANCH_FALSE _isZeroSet, return_unset  // propagate unset
   *   _isZeroVal = CALL _isZero._true() -> boolean
   *   BRANCH_TRUE _isZeroVal, continue_label  // If zero, continue
   *   // If not zero, return the result
   *   STORE rtn, cmpResult
   *   SCOPE_EXIT scope_id
   *   RETURN rtn
   *   continue_label:
   * </pre>
   */
  private List<IRInstr> generateNonZeroCheck(final String integerVar,
                                              final DebugInfo debugInfo,
                                              final String scopeId) {

    final var instructions = new ArrayList<IRInstr>();

    // Load zero literal for comparison
    final var zeroVar = generateTempName();
    instructions.add(LiteralInstr.literal(zeroVar, "0", getIntegerTypeName(), debugInfo));
    instructions.add(MemoryInstr.retain(zeroVar, debugInfo));
    instructions.add(ScopeInstr.register(zeroVar, scopeId, debugInfo));

    // Call _eq(0) to check if result is zero
    final var isZeroVar = generateTempName();
    instructions.addAll(generateMethodCall(
        isZeroVar,
        integerVar,
        "_eq",
        List.of(zeroVar),
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Extract boolean value via _true()
    final var isZeroBoolVar = generateTempName();
    instructions.addAll(generateMethodCall(
        isZeroBoolVar,
        isZeroVar,
        "_true",
        List.of(),
        "boolean",
        debugInfo,
        scopeId
    ));

    // Generate label for continue case (when zero)
    final var continueLabel = generateLabelName("continue_cmp");

    // If zero (equal to zero), branch to continue
    instructions.add(BranchInstr.branchIfTrue(isZeroBoolVar, continueLabel, debugInfo));

    // If not zero, return the result immediately
    instructions.add(MemoryInstr.store(RETURN_VAR, integerVar, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));
    // NO SCOPE_REGISTER for return var - ownership transfers to caller
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(RETURN_VAR, debugInfo));

    // Continue label
    instructions.add(LabelInstr.label(continueLabel));

    return instructions;
  }

  /**
   * Check if the given type is the Any type.
   */
  private boolean isAnyType(final IAggregateSymbol type) {
    final var anyType = stackContext.getParsedModule().getEk9Types().ek9Any();
    return anyType.isExactSameType(type);
  }

  /**
   * Generate the unset return block using the provided label name.
   */
  private List<IRInstr> generateUnsetReturnBlockWithLabel(final String labelName,
                                                          final String returnTypeName,
                                                          final String returnVarName,
                                                          final DebugInfo debugInfo,
                                                          final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label
    instructions.add(LabelInstr.label(labelName));

    // Create unset value via Integer._new()
    final var resultTemp = generateTempName();
    instructions.addAll(generateMethodCall(
        resultTemp,
        null,
        "_new",
        List.of(),
        returnTypeName,
        debugInfo,
        scopeId
    ));

    // Store to return variable and retain for ownership transfer
    instructions.add(MemoryInstr.store(returnVarName, resultTemp, debugInfo));
    instructions.add(MemoryInstr.retain(returnVarName, debugInfo));
    // NO SCOPE_REGISTER for return var - ownership transfers to caller

    // Scope cleanup and return
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(returnVarName, debugInfo));

    return instructions;
  }

  /**
   * Generate the zero return block using the provided label name.
   */
  private List<IRInstr> generateZeroReturnBlockWithLabel(final String labelName,
                                                          final String returnVarName,
                                                          final DebugInfo debugInfo,
                                                          final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label
    instructions.add(LabelInstr.label(labelName));

    // Create Integer zero via literal
    final var resultTemp = generateTempName();
    instructions.add(LiteralInstr.literal(resultTemp, "0", getIntegerTypeName(), debugInfo));
    instructions.add(MemoryInstr.retain(resultTemp, debugInfo));
    instructions.add(ScopeInstr.register(resultTemp, scopeId, debugInfo));

    // Store to return variable and retain for ownership transfer
    instructions.add(MemoryInstr.store(returnVarName, resultTemp, debugInfo));
    instructions.add(MemoryInstr.retain(returnVarName, debugInfo));
    // NO SCOPE_REGISTER for return var - ownership transfers to caller

    // Scope cleanup and return
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(returnVarName, debugInfo));

    return instructions;
  }
}
