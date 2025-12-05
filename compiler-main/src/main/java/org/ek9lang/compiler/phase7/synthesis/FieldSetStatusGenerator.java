package org.ek9lang.compiler.phase7.synthesis;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
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
 * <p>This method returns a Bits value where each bit represents whether a field
 * is set (1) or not set (0). The bit position corresponds to the field's
 * declaration order:</p>
 * <ul>
 *   <li>Bit 0 = field 0 status</li>
 *   <li>Bit 1 = field 1 status</li>
 *   <li>Bit N = field N status</li>
 * </ul>
 *
 * <p>Using Bits instead of Integer removes the 32-field limit and provides
 * a cleaner API for checking field status.</p>
 *
 * <p>The generated code follows this pattern:</p>
 * <pre>
 * Bits _fieldSetStatus():
 *   result = Bits()    // Empty Bits (set but with 0 bits)
 *
 *   // For each field:
 *   isSet = field._isSet()
 *   result._addAss(isSet)  // Append bit: result += isSet
 *
 *   return result
 * </pre>
 *
 * <p>Callers can use:</p>
 * <ul>
 *   <li>{@code status._eq(other)} - compare if two objects have identical set fields</li>
 *   <li>{@code status._empty()} - check if no fields are set</li>
 *   <li>{@code status._xor(other)} - find fields set differently</li>
 * </ul>
 *
 * <p>This approach enables:</p>
 * <ul>
 *   <li>Unlimited field count (no 32-field limit)</li>
 *   <li>Efficient field status comparison with single method call</li>
 *   <li>IR optimizer can cache results across expressions</li>
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

    // Reference return variable with Bits type
    instructions.add(MemoryInstr.reference(RETURN_VAR, getBitsTypeName(), debugInfo));

    // Create new empty-but-set Bits by calling Bits(String) with empty string.
    // Using Bits() creates an unset Bits, but Bits("") creates a set Bits with 0 bits.
    // This is critical because _addAss(Boolean) requires this.isSet == true to work.
    final var emptyStringVar = generateTempName();
    instructions.addAll(generateStringLiteralLoad(emptyStringVar, "", debugInfo, scopeId));

    final var bitsVar = generateTempName();
    instructions.addAll(generateConstructorCallWithArgs(
        bitsVar,
        getBitsTypeName(),
        List.of(emptyStringVar),
        List.of(getStringTypeName()),
        debugInfo,
        scopeId
    ));

    // Store to return variable
    instructions.add(MemoryInstr.store(RETURN_VAR, bitsVar, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));

    // Get fields for this aggregate (not inherited - those are handled by super's _fieldSetStatus)
    final var fields = getSyntheticFields(aggregateSymbol);

    // Generate field status append for each field
    for (final var field : fields) {
      instructions.addAll(generateFieldStatusAppend(field, debugInfo, scopeId));
    }

    // Return the accumulated status
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(RETURN_VAR, debugInfo));

    return instructions;
  }

  /**
   * Generate field status check and append to Bits result.
   *
   * <p>Pattern for each field:</p>
   * <pre>
   *   fieldValue = LOAD this.field
   *   isSetResult = CALL fieldValue._isSet()
   *   CALL rtn._addAss(isSetResult)  // rtn += isSetResult
   * </pre>
   *
   * <p>This is simpler than the old Integer bitmask approach because:</p>
   * <ul>
   *   <li>No mask calculation needed</li>
   *   <li>No conditional branching - always append</li>
   *   <li>_addAss mutates in place</li>
   * </ul>
   */
  private List<IRInstr> generateFieldStatusAppend(final ISymbol field,
                                                   final DebugInfo debugInfo,
                                                   final String scopeId) {

    final var fieldName = field.getName();
    final var fieldTypeName = getTypeName(field);
    final var instructions = new ArrayList<IRInstr>();

    // Load field value
    final var fieldVar = generateTempName();
    instructions.addAll(generateFieldLoad(fieldVar, IRConstants.THIS, fieldName, debugInfo, scopeId));

    // Call field._isSet() -> Boolean
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

    // Call rtn._addAss(isSetResult) to append the boolean as a bit
    // This is a mutating call (returns void), so we pass null for result var
    instructions.addAll(generateMethodCall(
        null,  // void return - no result variable
        RETURN_VAR,
        getBitsTypeName(),
        "_addAss",
        List.of(isSetResultVar),
        List.of(getBooleanTypeName()),
        getVoidTypeName(),
        debugInfo,
        scopeId
    ));

    return instructions;
  }
}
