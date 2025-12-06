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
 *   // Check super first (if not Any and has ? operator)
 *   if super._isSet() -> return true  // Valid via parent
 *
 *   // For each field in own properties:
 *   //   result = this.field._isSet()
 *   //   if result._true() -> return true  // Found set field
 *
 *   return false  // No fields set = truly empty object
 * </pre>
 *
 * <p>Key semantic requirements:</p>
 * <ul>
 *   <li>Returns true if ANY field is set (short-circuit on first set)</li>
 *   <li>Returns true if super._isSet() returns true</li>
 *   <li>Returns false only when ALL fields are unset (truly empty)</li>
 *   <li>No parameter checking needed (no "other" parameter)</li>
 * </ul>
 *
 * <p>This "ANY field set" semantic enables:</p>
 * <ul>
 *   <li>Partial objects with optional fields to be valid</li>
 *   <li>Builder patterns where objects become valid incrementally</li>
 *   <li>Reduced boilerplate (no need to override ? for optional fields)</li>
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

    // Labels for control flow - note the semantics flip from ALL to ANY
    final var returnFalseLabel = generateLabelName("return_false");
    final var returnTrueLabel = generateLabelName("return_true");

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Reference return variable
    instructions.add(MemoryInstr.reference(RETURN_VAR, getBooleanTypeName(), debugInfo));

    // Check super first - if super._isSet() returns true, we're valid via inheritance
    final var superHasIsSet = superHasOperator(aggregateSymbol, "?");
    if (superHasIsSet) {
      instructions.addAll(generateSuperIsSetCheck(aggregateSymbol, debugInfo, scopeId, returnTrueLabel));
    }

    // Generate field-by-field isSet check for this class's own fields
    // Key semantic change: return TRUE on first SET field (not false on first unset)
    final var fields = getSyntheticFields(aggregateSymbol);
    for (final var field : fields) {
      instructions.addAll(generateFieldIsSetCheck(field, debugInfo, scopeId, returnTrueLabel));
    }

    // No fields set (and super wasn't set) - branch to return false
    instructions.add(BranchInstr.branch(returnFalseLabel, debugInfo));

    // Return blocks - note: false first since that's now the "fall-through" case
    instructions.addAll(generateBooleanReturnBlockWithLabel(returnFalseLabel, false,
        RETURN_VAR, debugInfo, scopeId));
    instructions.addAll(generateBooleanReturnBlockWithLabel(returnTrueLabel, true,
        RETURN_VAR, debugInfo, scopeId));

    return instructions;
  }

  /**
   * Generate super._isSet() check.
   *
   * <p>If super._isSet() returns true, the object is valid via inheritance,
   * so we short-circuit to return true immediately.</p>
   */
  private List<IRInstr> generateSuperIsSetCheck(final AggregateSymbol aggregateSymbol,
                                                 final DebugInfo debugInfo,
                                                 final String scopeId,
                                                 final String returnTrueLabel) {

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

    // Call super._isSet() -> Boolean
    final var superResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        superResultVar,
        IRConstants.SUPER,
        superAggregate.getFullyQualifiedName(),
        IRConstants.IS_SET_METHOD,
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Extract boolean value via _true()
    final var superBoolVar = generateTempName();
    instructions.addAll(generateMethodCall(
        superBoolVar,
        superResultVar,
        getBooleanTypeName(),
        IRConstants.TRUE_METHOD,
        IRConstants.BOOLEAN,
        debugInfo,
        scopeId
    ));

    // If super is set (true), branch to return true - valid via inheritance
    instructions.add(BranchInstr.branchIfTrue(superBoolVar, returnTrueLabel, debugInfo));

    return instructions;
  }

  /**
   * Generate field isSet check.
   *
   * <p>Key semantic change for "ANY field set": branches to return TRUE
   * when field IS set (opposite of previous "ALL fields set" behavior).</p>
   */
  private List<IRInstr> generateFieldIsSetCheck(final ISymbol field,
                                                 final DebugInfo debugInfo,
                                                 final String scopeId,
                                                 final String returnTrueLabel) {

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
        IRConstants.IS_SET_METHOD,
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
        IRConstants.TRUE_METHOD,
        IRConstants.BOOLEAN,
        debugInfo,
        scopeId
    ));

    // Branch if TRUE (field IS set) - ANY set field makes object valid
    instructions.add(BranchInstr.branchIfTrue(trueResultVar, returnTrueLabel, debugInfo));

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
        value ? IRConstants.OF_TRUE_METHOD : IRConstants.OF_FALSE_METHOD,
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
