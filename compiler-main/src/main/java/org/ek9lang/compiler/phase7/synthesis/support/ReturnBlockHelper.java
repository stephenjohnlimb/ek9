package org.ek9lang.compiler.phase7.synthesis.support;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.LabelInstr;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.core.AssertValue;

/**
 * Helper class for generating return block IR patterns.
 *
 * <p>Consolidates the various return block patterns used across synthetic generators:</p>
 * <ul>
 *   <li>Boolean returns (true/false via _ofTrue/_ofFalse)</li>
 *   <li>Integer returns (literal values like zero for compare)</li>
 *   <li>Unset returns (creates new unset instance via constructor)</li>
 *   <li>Value returns (returns existing variable, just scope cleanup)</li>
 * </ul>
 *
 * <p>All return blocks follow the same structure:</p>
 * <pre>
 *   label_name:
 *   [create/prepare return value]
 *   STORE returnVar = value
 *   RETAIN returnVar
 *   SCOPE_EXIT scopeId
 *   RETURN returnVar
 * </pre>
 */
public final class ReturnBlockHelper {

  private final IRGenerationContext context;

  /**
   * Create a new return block helper with the given context.
   *
   * @param context The IR generation context
   */
  public ReturnBlockHelper(final IRGenerationContext context) {
    AssertValue.checkNotNull("context cannot be null", context);
    this.context = context;
  }

  /**
   * Generate a return block that returns a boolean value (true or false).
   *
   * <p>Uses Boolean._ofTrue() or Boolean._ofFalse() static factory methods.</p>
   *
   * @param labelName     The label name for this return block
   * @param value         The boolean value to return
   * @param returnVarName The return variable name
   * @param debugInfo     Debug information
   * @param scopeId       Current scope ID
   * @return List of IR instructions for the return block
   */
  public List<IRInstr> generateBooleanReturn(final String labelName,
                                             final boolean value,
                                             final String returnVarName,
                                             final DebugInfo debugInfo,
                                             final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label
    instructions.add(LabelInstr.label(labelName));

    // Create Boolean via Boolean._ofTrue() or _ofFalse()
    final var resultTemp = context.generateTempName();
    final var methodName = value ? IRConstants.OF_TRUE_METHOD : IRConstants.OF_FALSE_METHOD;
    final var callDetails = new CallDetails(
        null, // Static call
        getBooleanTypeName(),
        methodName,
        List.of(),
        getBooleanTypeName(),
        List.of(),
        CallMetaDataDetails.defaultMetaData(),
        false // isTraitCall
    );
    instructions.add(CallInstr.callStatic(resultTemp, debugInfo, callDetails));

    // Memory management for temp
    instructions.add(MemoryInstr.retain(resultTemp, debugInfo));
    instructions.add(ScopeInstr.register(resultTemp, scopeId, debugInfo));

    // Store to return variable and retain for ownership transfer
    instructions.add(MemoryInstr.store(returnVarName, resultTemp, debugInfo));
    instructions.add(MemoryInstr.retain(returnVarName, debugInfo));

    // Scope cleanup and return
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(returnVarName, debugInfo));

    return instructions;
  }

  /**
   * Generate a return block that returns an integer literal value.
   *
   * <p>Commonly used for compare operators returning zero (equal).</p>
   *
   * @param labelName     The label name for this return block
   * @param value         The integer value to return
   * @param returnVarName The return variable name
   * @param debugInfo     Debug information
   * @param scopeId       Current scope ID
   * @return List of IR instructions for the return block
   */
  public List<IRInstr> generateIntegerReturn(final String labelName,
                                             final int value,
                                             final String returnVarName,
                                             final DebugInfo debugInfo,
                                             final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label
    instructions.add(LabelInstr.label(labelName));

    // Create Integer via literal
    final var resultTemp = context.generateTempName();
    instructions.add(LiteralInstr.literal(resultTemp, String.valueOf(value),
        getIntegerTypeName(), debugInfo));
    instructions.add(MemoryInstr.retain(resultTemp, debugInfo));
    instructions.add(ScopeInstr.register(resultTemp, scopeId, debugInfo));

    // Store to return variable and retain for ownership transfer
    instructions.add(MemoryInstr.store(returnVarName, resultTemp, debugInfo));
    instructions.add(MemoryInstr.retain(returnVarName, debugInfo));

    // Scope cleanup and return
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(returnVarName, debugInfo));

    return instructions;
  }

  /**
   * Generate a return block that returns an unset value of the given type.
   *
   * <p>Creates a new instance via the default constructor, which produces an unset value.</p>
   *
   * @param labelName      The label name for this return block
   * @param returnTypeName The fully qualified type name to create
   * @param returnVarName  The return variable name
   * @param debugInfo      Debug information
   * @param scopeId        Current scope ID
   * @return List of IR instructions for the return block
   */
  public List<IRInstr> generateUnsetReturn(final String labelName,
                                           final String returnTypeName,
                                           final String returnVarName,
                                           final DebugInfo debugInfo,
                                           final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label
    instructions.add(LabelInstr.label(labelName));

    // Create unset value via constructor call
    final var resultTemp = context.generateTempName();
    final var callDetails = new CallDetails(
        returnTypeName, // Target is the class name for constructor calls
        returnTypeName,
        IRConstants.INIT_METHOD,
        List.of(),
        returnTypeName,
        List.of(),
        CallMetaDataDetails.defaultMetaData(),
        false // isTraitCall
    );
    instructions.add(CallInstr.call(resultTemp, debugInfo, callDetails));

    // Memory management for temp
    instructions.add(MemoryInstr.retain(resultTemp, debugInfo));
    instructions.add(ScopeInstr.register(resultTemp, scopeId, debugInfo));

    // Store to return variable and retain for ownership transfer
    instructions.add(MemoryInstr.store(returnVarName, resultTemp, debugInfo));
    instructions.add(MemoryInstr.retain(returnVarName, debugInfo));

    // Scope cleanup and return
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(returnVarName, debugInfo));

    return instructions;
  }

  /**
   * Generate a return block that returns the value already in the return variable.
   *
   * <p>This is used when the return value has been computed and stored elsewhere.
   * Only performs scope cleanup and return.</p>
   *
   * @param labelName     The label name for this return block
   * @param returnVarName The return variable name (already populated)
   * @param debugInfo     Debug information
   * @param scopeId       Current scope ID
   * @return List of IR instructions for the return block
   */
  public List<IRInstr> generateValueReturn(final String labelName,
                                           final String returnVarName,
                                           final DebugInfo debugInfo,
                                           final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label
    instructions.add(LabelInstr.label(labelName));

    // Scope cleanup and return - return variable was already set
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(returnVarName, debugInfo));

    return instructions;
  }

  // ========== Type Name Helpers ==========

  private String getBooleanTypeName() {
    return context.getParsedModule().getEk9Types().ek9Boolean().getFullyQualifiedName();
  }

  private String getIntegerTypeName() {
    return context.getParsedModule().getEk9Types().ek9Integer().getFullyQualifiedName();
  }
}
