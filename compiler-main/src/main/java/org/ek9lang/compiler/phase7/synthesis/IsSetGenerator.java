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
 * Generates synthetic IR for the _isSet (?) operator.
 *
 * <p>The generated code follows this pattern:</p>
 * <pre>
 * Boolean _isSet():
 *   // For each field in properties:
 *   //   result = this.field._isSet()
 *   //   if !result._true() -> return false
 *
 *   return true  // All fields are set
 * </pre>
 *
 * <p>Key semantic requirements:</p>
 * <ul>
 *   <li>Returns true only if ALL fields are set</li>
 *   <li>Returns false if ANY field is unset (short-circuit)</li>
 *   <li>No parameter checking needed (no "other" parameter)</li>
 *   <li>Super's _isSet is not called - each class handles its own fields</li>
 * </ul>
 */
final class IsSetGenerator extends AbstractSyntheticGenerator {

  private static final String RETURN_VAR = "rtn";

  IsSetGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  /**
   * Generate the _isSet operator IR for the given aggregate.
   *
   * @param methodSymbol    The _isSet method symbol
   * @param aggregateSymbol The aggregate containing the method
   * @return List of IR instructions implementing the method
   */
  List<IRInstr> generate(final MethodSymbol methodSymbol,
                         final AggregateSymbol aggregateSymbol) {

    AssertValue.checkNotNull("methodSymbol cannot be null", methodSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = createDebugInfo(methodSymbol);
    final var scopeId = stackContext.generateScopeId("_isSet");

    // Labels for control flow
    final var returnFalseLabel = generateLabelName("return_false");
    final var returnTrueLabel = generateLabelName("return_true");

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Reference return variable
    instructions.add(MemoryInstr.reference(RETURN_VAR, getBooleanTypeName(), debugInfo));

    // Generate field-by-field isSet check for this class's own fields only
    final var fields = getSyntheticFields(aggregateSymbol);
    for (final var field : fields) {
      instructions.addAll(generateFieldIsSetCheck(field, debugInfo, scopeId, returnFalseLabel));
    }

    // All checks passed - branch to return true
    instructions.add(BranchInstr.branch(returnTrueLabel, debugInfo));

    // Return blocks
    instructions.addAll(generateBooleanReturnBlockWithLabel(returnFalseLabel, false,
        RETURN_VAR, debugInfo, scopeId));
    instructions.addAll(generateBooleanReturnBlockWithLabel(returnTrueLabel, true,
        RETURN_VAR, debugInfo, scopeId));

    return instructions;
  }

  /**
   * Generate field isSet check.
   */
  private List<IRInstr> generateFieldIsSetCheck(final ISymbol field,
                                                 final DebugInfo debugInfo,
                                                 final String scopeId,
                                                 final String returnFalseLabel) {

    final var fieldName = field.getName();
    final var fieldTypeName = getTypeName(field);

    // Load this.field
    final var thisFieldVar = generateTempName();
    final var instructions = new ArrayList<>(generateFieldLoad(thisFieldVar, IRConstants.THIS, fieldName,
        debugInfo, scopeId));

    // Call field._isSet() -> Boolean
    final var isSetResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        isSetResultVar,
        thisFieldVar,
        fieldTypeName,
        "_isSet",
        List.of(),
        List.of(),
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Call _true() to get the primitive boolean value
    final var trueResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        trueResultVar,
        isSetResultVar,
        getBooleanTypeName(),
        "_true",
        List.of(),
        List.of(),
        "boolean", // Primitive type
        debugInfo,
        scopeId
    ));

    // Branch if false (field is not set)
    instructions.add(BranchInstr.branchIfFalse(trueResultVar, returnFalseLabel, debugInfo));

    return instructions;
  }

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
