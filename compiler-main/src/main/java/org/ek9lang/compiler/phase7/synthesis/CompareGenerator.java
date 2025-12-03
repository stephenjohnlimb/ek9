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
  // Must match AggregateManipulator.PARAM - the actual parameter name in MethodSymbol
  private static final String OTHER_PARAM = "param";

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
    final var aggregateTypeName = aggregateSymbol.getFullyQualifiedName();

    // Labels for control flow - generate ONCE and reuse
    final var returnUnsetLabel = generateLabelName("return_unset");
    final var returnZeroLabel = generateLabelName("return_zero");
    final var returnResultLabel = generateLabelName("return_result");

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Reference return variable
    instructions.add(MemoryInstr.reference(RETURN_VAR, getIntegerTypeName(), debugInfo));

    // Generate isSet guards for this and other
    instructions.addAll(generateThisIsSetGuard(aggregateTypeName, debugInfo, returnUnsetLabel, scopeId));
    instructions.addAll(generateIsSetGuard(OTHER_PARAM, aggregateTypeName, debugInfo, returnUnsetLabel, scopeId));

    // Generate field set status check if aggregate has fields
    // This optimizes tri-state comparison by early-detecting set/unset mismatches
    final var fields = getSyntheticFields(aggregateSymbol);
    if (!fields.isEmpty()) {
      instructions.addAll(generateFieldSetStatusCheck(aggregateTypeName, debugInfo, scopeId, returnUnsetLabel));
    }

    // Check if super has the <=> operator
    final var superHasCmp = superHasOperator(aggregateSymbol, "<=>");

    // Generate super._cmp check only if super actually has the operator
    if (superHasCmp) {
      instructions.addAll(generateSuperCmpCheck(aggregateSymbol, debugInfo, scopeId,
          returnUnsetLabel, returnResultLabel));
    }

    // Generate field-by-field comparison for this class's own fields only
    // We respect encapsulation: inherited fields are handled by super._cmp() if it exists
    // If super doesn't have <=>, it chose not to participate in comparison - we respect that
    for (final var field : fields) {
      instructions.addAll(generateFieldCmpCheck(field, debugInfo, scopeId,
          returnUnsetLabel, returnResultLabel));
    }

    // All checks passed (all returned zero) - branch to return zero
    instructions.add(BranchInstr.branch(returnZeroLabel, debugInfo));

    // Return blocks - all share single scope exit point
    instructions.addAll(generateUnsetReturnBlockWithLabel(returnUnsetLabel, getIntegerTypeName(),
        RETURN_VAR, debugInfo, scopeId));
    instructions.addAll(generateZeroReturnBlockWithLabel(returnZeroLabel,
        RETURN_VAR, debugInfo, scopeId));
    instructions.addAll(generateResultReturnBlock(returnResultLabel, debugInfo, scopeId));

    return instructions;
  }

  /**
   * Generate field set status comparison check.
   *
   * <p>This optimization compares the _fieldSetStatus() bitmasks of both objects.
   * If the bitmasks differ, it means different fields are set/unset between the objects,
   * so the comparison result should be unset (tri-state semantics).</p>
   *
   * <p>Pattern:</p>
   * <pre>
   *   _thisStatus = CALL this._fieldSetStatus() -> Integer
   *   _otherStatus = CALL other._fieldSetStatus() -> Integer
   *   _statusEq = CALL _thisStatus._eq(_otherStatus) -> Boolean
   *   _statusEqSet = CALL _statusEq._isSet() -> Boolean
   *   BRANCH_IF_FALSE _statusEqSet -> return_unset  // shouldn't happen but safe
   *   _statusEqVal = CALL _statusEq._true() -> boolean
   *   BRANCH_IF_FALSE _statusEqVal -> return_unset  // different field set patterns
   * </pre>
   */
  private List<IRInstr> generateFieldSetStatusCheck(final String aggregateTypeName,
                                                     final DebugInfo debugInfo,
                                                     final String scopeId,
                                                     final String returnUnsetLabel) {
    final var instructions = new ArrayList<IRInstr>();

    // Call this._fieldSetStatus()
    final var thisStatusVar = generateTempName();
    instructions.addAll(generateMethodCall(
        thisStatusVar,
        IRConstants.THIS,
        aggregateTypeName,
        "_fieldSetStatus",
        List.of(),
        List.of(),
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Call other._fieldSetStatus()
    final var otherStatusVar = generateTempName();
    instructions.addAll(generateMethodCall(
        otherStatusVar,
        OTHER_PARAM,
        aggregateTypeName,
        "_fieldSetStatus",
        List.of(),
        List.of(),
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Compare the bitmasks with _eq
    final var statusEqVar = generateTempName();
    instructions.addAll(generateMethodCall(
        statusEqVar,
        thisStatusVar,
        getIntegerTypeName(),
        "_eq",
        List.of(otherStatusVar),
        List.of(getIntegerTypeName()),
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Check if comparison result is set (defensive - should always be set for Integer)
    instructions.addAll(generateIsSetGuard(statusEqVar, getBooleanTypeName(), debugInfo, returnUnsetLabel, scopeId));

    // Check if bitmasks are equal - if not, return unset (set/unset mismatch)
    final var statusEqBoolVar = generateTempName();
    instructions.addAll(generateMethodCall(
        statusEqBoolVar,
        statusEqVar,
        getBooleanTypeName(),
        "_true",
        List.of(),
        List.of(),
        "boolean",
        debugInfo,
        scopeId
    ));

    // Branch to return unset if bitmasks differ
    instructions.add(BranchInstr.branchIfFalse(statusEqBoolVar, returnUnsetLabel, debugInfo));

    return instructions;
  }

  /**
   * Generate super._cmp check if super is not Any.
   */
  private List<IRInstr> generateSuperCmpCheck(final AggregateSymbol aggregateSymbol,
                                               final DebugInfo debugInfo,
                                               final String scopeId,
                                               final String returnUnsetLabel,
                                               final String returnResultLabel) {

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
        superAggregate.getFullyQualifiedName(),
        "_cmp",
        List.of(OTHER_PARAM),
        List.of(superAggregate.getFullyQualifiedName()),
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Check if super result is set
    instructions.addAll(generateIsSetGuard(superResultVar, getIntegerTypeName(), debugInfo, returnUnsetLabel, scopeId));

    // Check if super result is non-zero - if so, store and branch to return
    instructions.addAll(generateNonZeroCheck(superResultVar, debugInfo, scopeId, returnResultLabel));

    return instructions;
  }

  /**
   * Generate field comparison check.
   */
  private List<IRInstr> generateFieldCmpCheck(final ISymbol field,
                                               final DebugInfo debugInfo,
                                               final String scopeId,
                                               final String returnUnsetLabel,
                                               final String returnResultLabel) {

    final var fieldName = field.getName();
    final var fieldTypeName = getTypeName(field);

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
        fieldTypeName,
        "_cmp",
        List.of(otherFieldVar),
        List.of(fieldTypeName),
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Check if result is set
    instructions.addAll(generateIsSetGuard(cmpResultVar, getIntegerTypeName(), debugInfo, returnUnsetLabel, scopeId));

    // Check if result is non-zero - if so, store and branch to return
    instructions.addAll(generateNonZeroCheck(cmpResultVar, debugInfo, scopeId, returnResultLabel));

    return instructions;
  }

  /**
   * Generate check if Integer variable is non-zero. If non-zero, branch to return.
   *
   * <p>Pattern:</p>
   * <pre>
   *   _zero = LOAD_LITERAL 0 -> Integer
   *   _isZero = CALL cmpResult._eq(_zero) -> Boolean
   *   _isZeroVal = CALL _isZero._true() -> boolean
   *   BRANCH_TRUE _isZeroVal, continue_label  // If zero, continue
   *   // If not zero, store result and branch to return
   *   STORE rtn, cmpResult
   *   RETAIN rtn
   *   BRANCH return_result_label
   *   continue_label:
   * </pre>
   */
  private List<IRInstr> generateNonZeroCheck(final String integerVar,
                                              final DebugInfo debugInfo,
                                              final String scopeId,
                                              final String returnResultLabel) {

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
        getIntegerTypeName(),
        "_eq",
        List.of(zeroVar),
        List.of(getIntegerTypeName()),
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Extract boolean value via _true()
    final var isZeroBoolVar = generateTempName();
    instructions.addAll(generateMethodCall(
        isZeroBoolVar,
        isZeroVar,
        getBooleanTypeName(),
        "_true",
        List.of(),
        List.of(),
        "boolean",
        debugInfo,
        scopeId
    ));

    // Generate label for continue case (when zero)
    final var continueLabel = generateLabelName("continue_cmp");

    // If zero (equal to zero), branch to continue
    instructions.add(BranchInstr.branchIfTrue(isZeroBoolVar, continueLabel, debugInfo));

    // If not zero, store result and branch to shared return block
    instructions.add(MemoryInstr.store(RETURN_VAR, integerVar, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));
    // Branch to the shared return block (no inline SCOPE_EXIT/RETURN)
    instructions.add(BranchInstr.branch(returnResultLabel, debugInfo));

    // Continue label
    instructions.add(LabelInstr.label(continueLabel));

    return instructions;
  }

  /**
   * Generate the return result block - returns whatever is in RETURN_VAR.
   * This is a shared return point for non-zero comparison results.
   */
  private List<IRInstr> generateResultReturnBlock(final String labelName,
                                                   final DebugInfo debugInfo,
                                                   final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label
    instructions.add(LabelInstr.label(labelName));

    // Scope cleanup and return - RETURN_VAR was already set by caller
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(RETURN_VAR, debugInfo));

    return instructions;
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

    // Create unset value via constructor call
    // Pattern: CALL (Type)Type.<init>() - creates new instance with NEW + DUP + INVOKESPECIAL
    final var resultTemp = generateTempName();
    instructions.addAll(generateConstructorCall(
        resultTemp,
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
