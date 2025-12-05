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
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Generates synthetic IR for the _eq (==) operator.
 *
 * <p>The generated code follows this pattern:</p>
 * <pre>
 * Boolean _eq(T other):
 *   // Guard: check this is set
 *   if !this._isSet() -> return unset
 *
 *   // Guard: check other is set
 *   if !other._isSet() -> return unset
 *
 *   // Super check (if super is not Any)
 *   if super != Any:
 *     result = super._eq(other)
 *     if !result._isSet() -> return unset
 *     if !result._true() -> return false
 *
 *   // Field-by-field comparison
 *   for each field in properties:
 *     result = this.field._eq(other.field)
 *     if !result._isSet() -> return unset
 *     if !result._true() -> return false
 *
 *   return true
 * </pre>
 *
 * <p>Key semantic requirements:</p>
 * <ul>
 *   <li>If this or other is unset, return unset</li>
 *   <li>If any field comparison returns unset, return unset</li>
 *   <li>If any field comparison returns false, return false (short-circuit)</li>
 *   <li>Only if all comparisons return true, return true</li>
 *   <li>Super's _eq is called if super is not Any</li>
 * </ul>
 */
final class EqualsGenerator extends AbstractSyntheticGenerator {

  private static final String RETURN_VAR = "rtn";
  // Must match AggregateManipulator.PARAM - the actual parameter name in MethodSymbol
  private static final String OTHER_PARAM = "param";

  EqualsGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  /**
   * Generate the _eq operator IR for the given aggregate.
   *
   * @param operatorSymbol  The _eq operator method symbol
   * @param aggregateSymbol The aggregate containing the operator
   * @return List of IR instructions implementing the operator
   */
  List<IRInstr> generate(final MethodSymbol operatorSymbol,
                         final AggregateSymbol aggregateSymbol) {

    AssertValue.checkNotNull("operatorSymbol cannot be null", operatorSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = createDebugInfo(operatorSymbol);
    final var scopeId = stackContext.generateScopeId("_eq");
    final var aggregateTypeName = aggregateSymbol.getFullyQualifiedName();

    // Labels for control flow
    final var returnUnsetLabel = generateLabelName("return_unset");
    final var returnFalseLabel = generateLabelName("return_false");
    final var returnTrueLabel = generateLabelName("return_true");

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Reference return variable
    instructions.add(MemoryInstr.reference(RETURN_VAR, getBooleanTypeName(), debugInfo));

    // Generate isSet guards for this and other
    instructions.addAll(generateThisIsSetGuard(aggregateTypeName, debugInfo, returnUnsetLabel, scopeId));
    instructions.addAll(generateIsSetGuard(OTHER_PARAM, aggregateTypeName, debugInfo, returnUnsetLabel, scopeId));

    // Generate field set status check if aggregate has fields
    // This optimizes tri-state comparison by early-detecting set/unset mismatches
    final var fields = getSyntheticFields(aggregateSymbol);
    if (!fields.isEmpty()) {
      instructions.addAll(generateFieldSetStatusCheck(aggregateTypeName, debugInfo, scopeId, returnUnsetLabel));
    }

    // Check if super has the == operator
    final var superHasEq = superHasOperator(aggregateSymbol, "==");

    // Generate super._eq check only if super actually has the operator
    if (superHasEq) {
      instructions.addAll(generateSuperEqCheck(aggregateSymbol, debugInfo, scopeId,
          returnUnsetLabel, returnFalseLabel));
    }

    // Generate field-by-field comparison for this class's own fields only
    // We respect encapsulation: inherited fields are handled by super._eq() if it exists
    // If super doesn't have ==, it chose not to participate in equality - we respect that
    for (final var field : fields) {
      instructions.addAll(generateFieldEqCheck(field, debugInfo, scopeId,
          returnUnsetLabel, returnFalseLabel));
    }

    // All checks passed - branch to return true
    instructions.add(BranchInstr.branch(returnTrueLabel, debugInfo));

    // Return blocks - use same label names that were generated above
    instructions.addAll(generateUnsetReturnBlockWithLabel(returnUnsetLabel, getBooleanTypeName(),
        RETURN_VAR, debugInfo, scopeId));
    instructions.addAll(generateBooleanReturnBlockWithLabel(returnFalseLabel, false,
        RETURN_VAR, debugInfo, scopeId));
    instructions.addAll(generateBooleanReturnBlockWithLabel(returnTrueLabel, true,
        RETURN_VAR, debugInfo, scopeId));

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

    // Call this._fieldSetStatus() -> Bits
    final var thisStatusVar = generateTempName();
    instructions.addAll(generateMethodCall(
        thisStatusVar,
        IRConstants.THIS,
        aggregateTypeName,
        "_fieldSetStatus",
        List.of(),
        List.of(),
        getBitsTypeName(),
        debugInfo,
        scopeId
    ));

    // Call other._fieldSetStatus() -> Bits
    final var otherStatusVar = generateTempName();
    instructions.addAll(generateMethodCall(
        otherStatusVar,
        OTHER_PARAM,
        aggregateTypeName,
        "_fieldSetStatus",
        List.of(),
        List.of(),
        getBitsTypeName(),
        debugInfo,
        scopeId
    ));

    // Compare the Bits with _eq
    final var statusEqVar = generateTempName();
    instructions.addAll(generateMethodCall(
        statusEqVar,
        thisStatusVar,
        getBitsTypeName(),
        "_eq",
        List.of(otherStatusVar),
        List.of(getBitsTypeName()),
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
   * Generate super._eq check if super is not Any.
   */
  private List<IRInstr> generateSuperEqCheck(final AggregateSymbol aggregateSymbol,
                                              final DebugInfo debugInfo,
                                              final String scopeId,
                                              final String returnUnsetLabel,
                                              final String returnFalseLabel) {

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

    // Call super._eq(other)
    final var superResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        superResultVar,
        IRConstants.SUPER,
        superAggregate.getFullyQualifiedName(),
        "_eq",
        List.of(OTHER_PARAM),
        List.of(superAggregate.getFullyQualifiedName()),
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Check if super result is set
    instructions.addAll(generateIsSetGuard(superResultVar, getBooleanTypeName(), debugInfo, returnUnsetLabel, scopeId));

    // Check if super result is true
    instructions.addAll(generateTrueCheck(superResultVar, debugInfo, returnFalseLabel, scopeId));

    return instructions;
  }

  /**
   * Generate field equality check.
   */
  private List<IRInstr> generateFieldEqCheck(final ISymbol field,
                                              final DebugInfo debugInfo,
                                              final String scopeId,
                                              final String returnUnsetLabel,
                                              final String returnFalseLabel) {

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

    // Call thisField._eq(otherField)
    final var eqResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        eqResultVar,
        thisFieldVar,
        fieldTypeName,
        "_eq",
        List.of(otherFieldVar),
        List.of(fieldTypeName),
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Check if result is set
    instructions.addAll(generateIsSetGuard(eqResultVar, getBooleanTypeName(), debugInfo, returnUnsetLabel, scopeId));

    // Check if result is true (branch to false if not)
    instructions.addAll(generateTrueCheck(eqResultVar, debugInfo, returnFalseLabel, scopeId));

    return instructions;
  }

  /**
   * Generate check if boolean variable is true, branch to label if false.
   */
  private List<IRInstr> generateTrueCheck(final String booleanVar,
                                          final DebugInfo debugInfo,
                                          final String falseLabel,
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
        "boolean", // Primitive type
        debugInfo,
        scopeId
    ));

    // Branch if false
    instructions.add(BranchInstr.branchIfFalse(trueResultVar, falseLabel, debugInfo));

    return instructions;
  }

  // NOTE: generateUnsetReturnBlockWithLabel is now inherited from AbstractSyntheticGenerator

  /**
   * Generate the boolean return block using the provided label name.
   */
  private List<IRInstr> generateBooleanReturnBlockWithLabel(final String labelName,
                                                            final boolean value,
                                                            final String returnVarName,
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
    instructions.add(MemoryInstr.store(returnVarName, resultTemp, debugInfo));
    instructions.add(MemoryInstr.retain(returnVarName, debugInfo));
    // NO SCOPE_REGISTER for return var - ownership transfers to caller

    // Scope cleanup and return
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(returnVarName, debugInfo));

    return instructions;
  }
}
