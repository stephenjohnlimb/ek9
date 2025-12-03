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
 * Generates synthetic IR for the _fieldSetStatus() method.
 *
 * <p>This method returns an Integer bitmask where each bit represents
 * whether a field is set (1) or not set (0). The bit position
 * corresponds to the field's declaration order:</p>
 * <ul>
 *   <li>Bit 0 = field 0 status</li>
 *   <li>Bit 1 = field 1 status</li>
 *   <li>Bit N = field N status</li>
 * </ul>
 *
 * <p>The generated code follows this pattern:</p>
 * <pre>
 * Integer _fieldSetStatus():
 *   result = 0
 *
 *   // For each field at index i (mask = 2^i):
 *   if field[i]._isSet():
 *     result = result._or(mask)
 *
 *   return result
 * </pre>
 *
 * <p>Callers can check if field N is set using:</p>
 * <pre>
 *   status._and(MASK_N)._gt(0)
 * </pre>
 * <p>where MASK_N = 2^N (compile-time constant)</p>
 *
 * <p>This approach enables:</p>
 * <ul>
 *   <li>Efficient field status checking with a single method call</li>
 *   <li>Optimization opportunity: IR optimizer can cache results</li>
 *   <li>Compact representation: one Integer for up to 32 fields</li>
 * </ul>
 *
 * <p>Note: EK9 guarantees fields are always initialized (to unset values),
 * so null checks are not needed. The _isSet() call handles the tri-state
 * semantics (absent/unset/set) at the type level.</p>
 *
 * @see CompareGenerator
 * @see EqualsGenerator
 */
final class FieldSetStatusGenerator extends AbstractSyntheticGenerator {

  private static final String RETURN_VAR = "rtn";

  FieldSetStatusGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  /**
   * Generate the _fieldSetStatus method IR for the given aggregate.
   *
   * @param methodSymbol    The _fieldSetStatus method symbol
   * @param aggregateSymbol The aggregate containing the method
   * @return List of IR instructions implementing the method
   */
  List<IRInstr> generate(final MethodSymbol methodSymbol,
                         final AggregateSymbol aggregateSymbol) {

    AssertValue.checkNotNull("methodSymbol cannot be null", methodSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = createDebugInfo(methodSymbol);
    final var scopeId = stackContext.generateScopeId("_fieldSetStatus");

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Reference return variable
    instructions.add(MemoryInstr.reference(RETURN_VAR, getIntegerTypeName(), debugInfo));

    // Initialize result to 0
    final var resultVar = generateTempName();
    instructions.add(LiteralInstr.literal(resultVar, "0", getIntegerTypeName(), debugInfo));
    instructions.add(MemoryInstr.retain(resultVar, debugInfo));
    instructions.add(ScopeInstr.register(resultVar, scopeId, debugInfo));

    // Store initial value to return variable
    instructions.add(MemoryInstr.store(RETURN_VAR, resultVar, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));

    // Get fields for this aggregate (not inherited - those are handled by super's _fieldSetStatus)
    final var fields = getSyntheticFields(aggregateSymbol);

    // Generate field status check for each field
    int fieldIndex = 0;
    for (final var field : fields) {
      // Calculate mask for this field: 2^fieldIndex
      final int mask = 1 << fieldIndex;
      instructions.addAll(generateFieldStatusCheck(field, mask, debugInfo, scopeId));
      fieldIndex++;
    }

    // Return the accumulated status
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(RETURN_VAR, debugInfo));

    return instructions;
  }

  /**
   * Generate field status check and OR into result if set.
   *
   * <p>Pattern for each field:</p>
   * <pre>
   *   fieldValue = LOAD this.field
   *   isSetResult = CALL fieldValue._isSet()
   *   isSetBool = CALL isSetResult._true()
   *   BRANCH_FALSE isSetBool, skip_label  // If not set, skip
   *   // Field is set - OR the mask into result
   *   maskLiteral = LOAD_LITERAL mask
   *   newResult = CALL rtn._or(maskLiteral)
   *   STORE rtn, newResult
   *   skip_label:
   * </pre>
   */
  private List<IRInstr> generateFieldStatusCheck(final ISymbol field,
                                                  final int mask,
                                                  final DebugInfo debugInfo,
                                                  final String scopeId) {

    final var fieldName = field.getName();
    final var fieldTypeName = getTypeName(field);
    final var instructions = new ArrayList<IRInstr>();

    // Label to skip if field is unset
    final var skipLabel = generateLabelName("field_skip");

    // Load field value
    final var fieldVar = generateTempName();
    instructions.addAll(generateFieldLoad(fieldVar, IRConstants.THIS, fieldName, debugInfo, scopeId));

    // Call field._isSet()
    final var isSetResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        isSetResultVar,
        fieldVar,
        fieldTypeName,
        IRConstants.IS_SET,
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

    // If not set, skip
    instructions.add(BranchInstr.branchIfFalse(isSetBoolVar, skipLabel, debugInfo));

    // Field is set - OR the mask into result
    final var maskVar = generateTempName();
    instructions.add(LiteralInstr.literal(maskVar, String.valueOf(mask), getIntegerTypeName(), debugInfo));
    instructions.add(MemoryInstr.retain(maskVar, debugInfo));
    instructions.add(ScopeInstr.register(maskVar, scopeId, debugInfo));

    // Call rtn._or(mask)
    final var newResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        newResultVar,
        RETURN_VAR,
        getIntegerTypeName(),
        "_or",
        List.of(maskVar),
        List.of(getIntegerTypeName()),
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Store new result
    instructions.add(MemoryInstr.store(RETURN_VAR, newResultVar, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));

    // Skip label
    instructions.add(LabelInstr.label(skipLabel));

    return instructions;
  }
}
