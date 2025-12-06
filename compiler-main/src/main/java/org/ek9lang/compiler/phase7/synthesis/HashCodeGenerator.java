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
 * Generates synthetic IR for the _hashcode (#?) operator.
 *
 * <p>The generated code follows this pattern:</p>
 * <pre>
 * Integer _hashcode():
 *   // Guard: if object is unset, return UNSET
 *   if !this._isSet() -> return unset Integer
 *
 *   // Initialize with field set status as base hash
 *   status = this._fieldSetStatus()  // Returns Bits
 *   result = status._hashcode()      // Convert Bits to Integer
 *
 *   // For each field:
 *   if field._isSet():
 *     fieldHash = field.#?()
 *     result = result * 31
 *     result = result + fieldHash
 *
 *   return result
 * </pre>
 *
 * <p>Key semantic requirements:</p>
 * <ul>
 *   <li>Returns UNSET Integer if this._isSet() is false (empty objects have no hash)</li>
 *   <li>Only SET fields contribute to hash</li>
 *   <li>Field set status (as Bits) is hashed as base value</li>
 *   <li>Uses polynomial hash: result = result * 31 + fieldHash</li>
 *   <li>Objects with different set/unset patterns have different hashes</li>
 * </ul>
 *
 * <p>The inclusion of _fieldSetStatus()._hashcode() as the initial hash value
 * ensures that two objects with the same set field values but different unset
 * fields will produce different hash codes.</p>
 *
 * <p>The ? guard ensures consistency: operations on empty objects propagate uncertainty.</p>
 */
final class HashCodeGenerator extends AbstractSyntheticGenerator {

  private static final String RETURN_VAR = "rtn";
  private static final int HASH_MULTIPLIER = 31;

  HashCodeGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  /**
   * Generate the _hashcode operator IR for the given aggregate.
   *
   * @param operatorSymbol  The _hashcode operator symbol
   * @param aggregateSymbol The aggregate containing the operator
   * @return List of IR instructions implementing the operator
   */
  List<IRInstr> generate(final MethodSymbol operatorSymbol,
                         final AggregateSymbol aggregateSymbol) {

    AssertValue.checkNotNull("operatorSymbol cannot be null", operatorSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = createDebugInfo(operatorSymbol);
    final var scopeId = stackContext.generateScopeId("_hashcode");
    final var aggregateTypeName = aggregateSymbol.getFullyQualifiedName();

    // Label for returning UNSET when object is unset
    final var returnUnsetLabel = generateLabelName("return_unset");
    final var returnHashLabel = generateLabelName("return_hash");

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Reference return variable
    instructions.add(MemoryInstr.reference(RETURN_VAR, getIntegerTypeName(), debugInfo));

    // Guard: if no fields are set, return UNSET Integer
    // Empty objects (no fields set) have no meaningful hash
    // Uses _fieldSetStatus()._empty() directly - no dependency on ? operator
    instructions.addAll(generateAnyFieldSetGuard(aggregateSymbol, aggregateTypeName, debugInfo,
        returnUnsetLabel, scopeId));

    // Initialize result with _fieldSetStatus() as base hash
    // This ensures objects with different set/unset patterns have different hashes
    instructions.addAll(initializeWithFieldSetStatus(aggregateTypeName, debugInfo, scopeId));

    // Process each field
    final var fields = getSyntheticFields(aggregateSymbol);
    for (final var field : fields) {
      instructions.addAll(generateFieldHashContribution(field, debugInfo, scopeId));
    }

    // Return the accumulated hash
    instructions.add(BranchInstr.branch(returnHashLabel, debugInfo));

    // Return blocks
    instructions.addAll(generateUnsetReturnBlockWithLabel(returnUnsetLabel, getIntegerTypeName(),
        RETURN_VAR, debugInfo, scopeId));
    instructions.addAll(generateHashReturnBlock(returnHashLabel, debugInfo, scopeId));

    return instructions;
  }

  /**
   * Generate the return block for normal hash return.
   */
  private List<IRInstr> generateHashReturnBlock(final String labelName,
                                                 final DebugInfo debugInfo,
                                                 final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label
    instructions.add(LabelInstr.label(labelName));

    // Scope cleanup and return
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(RETURN_VAR, debugInfo));

    return instructions;
  }

  /**
   * Initialize result with _fieldSetStatus()._hashcode() call.
   *
   * <p>This provides the base hash that encodes which fields are set.
   * Since _fieldSetStatus() now returns Bits, we call _hashcode() on it
   * to get an Integer for the polynomial hash algorithm.</p>
   */
  private List<IRInstr> initializeWithFieldSetStatus(final String aggregateTypeName,
                                                      final DebugInfo debugInfo,
                                                      final String scopeId) {

    // Call this._fieldSetStatus() -> Bits
    final var statusVar = generateTempName();
    final var instructions = new ArrayList<>(generateMethodCall(
        statusVar,
        IRConstants.THIS,
        aggregateTypeName,
        "_fieldSetStatus",
        List.of(),
        List.of(),
        getBitsTypeName(),
        debugInfo,
        scopeId
    ));

    // Call status._hashcode() -> Integer
    final var statusHashVar = generateTempName();
    instructions.addAll(generateMethodCall(
        statusHashVar,
        statusVar,
        getBitsTypeName(),
        "_hashcode",
        List.of(),
        List.of(),
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Store as initial result
    instructions.add(MemoryInstr.store(RETURN_VAR, statusHashVar, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));

    return instructions;
  }

  /**
   * Generate hash contribution for a single field.
   *
   * <p>Pattern:</p>
   * <pre>
   *   fieldValue = LOAD this.field
   *   isSetResult = CALL fieldValue._isSet()
   *   isSetBool = CALL isSetResult._true()
   *   BRANCH_FALSE isSetBool, skip_label
   *
   *   // Field is set - include in hash
   *   fieldHash = CALL fieldValue.#?()
   *   // result = result * 31 + fieldHash
   *   multiplied = CALL rtn.*(31)
   *   combined = CALL multiplied.+(fieldHash)
   *   STORE rtn, combined
   *
   *   skip_label:
   * </pre>
   */
  private List<IRInstr> generateFieldHashContribution(final ISymbol field,
                                                       final DebugInfo debugInfo,
                                                       final String scopeId) {

    final var fieldName = field.getName();
    final var fieldTypeName = getTypeName(field);

    // Label to skip if field is unset
    final var skipLabel = generateLabelName("hash_skip");

    // Load field value
    final var fieldVar = generateTempName();
    final var instructions = new ArrayList<>(generateFieldLoad(fieldVar, IRConstants.THIS, fieldName, debugInfo, scopeId));

    // Call field._isSet()
    final var isSetResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        isSetResultVar,
        fieldVar,
        fieldTypeName,
        "_isSet",
        List.of(),
        List.of(),
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Extract boolean value via _true()
    final var isSetBoolVar = generateTempName();
    instructions.addAll(generateMethodCall(
        isSetBoolVar,
        isSetResultVar,
        getBooleanTypeName(),
        "_true",
        List.of(),
        List.of(),
        "boolean",
        debugInfo,
        scopeId
    ));

    // If not set, skip this field
    instructions.add(BranchInstr.branchIfFalse(isSetBoolVar, skipLabel, debugInfo));

    // Field is set - get its hash code via _hashcode method (#? operator)
    final var fieldHashVar = generateTempName();
    instructions.addAll(generateMethodCall(
        fieldHashVar,
        fieldVar,
        fieldTypeName,
        "_hashcode",
        List.of(),
        List.of(),
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Multiply current result by 31 via _mul method (* operator)
    final var multiplierVar = generateTempName();
    instructions.add(LiteralInstr.literal(multiplierVar, String.valueOf(HASH_MULTIPLIER),
        getIntegerTypeName(), debugInfo));
    instructions.add(MemoryInstr.retain(multiplierVar, debugInfo));
    instructions.add(ScopeInstr.register(multiplierVar, scopeId, debugInfo));

    final var multipliedVar = generateTempName();
    instructions.addAll(generateMethodCall(
        multipliedVar,
        RETURN_VAR,
        getIntegerTypeName(),
        "_mul",
        List.of(multiplierVar),
        List.of(getIntegerTypeName()),
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Add field hash to result via _add method (+ operator)
    final var combinedVar = generateTempName();
    instructions.addAll(generateMethodCall(
        combinedVar,
        multipliedVar,
        getIntegerTypeName(),
        "_add",
        List.of(fieldHashVar),
        List.of(getIntegerTypeName()),
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Store combined result - must RELEASE old value first for ARC
    instructions.add(MemoryInstr.release(RETURN_VAR, debugInfo));
    instructions.add(MemoryInstr.store(RETURN_VAR, combinedVar, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));

    // Skip label
    instructions.add(LabelInstr.label(skipLabel));

    return instructions;
  }
}
