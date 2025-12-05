package org.ek9lang.compiler.phase7.synthesis;

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
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Base class for synthetic operator generators.
 *
 * <p>Provides common patterns and utilities used by all synthetic generators:</p>
 * <ul>
 *   <li>IsSet guard generation for this and parameters</li>
 *   <li>Unset return block generation</li>
 *   <li>Field iteration utilities</li>
 *   <li>Memory management (RETAIN/SCOPE_REGISTER) patterns</li>
 * </ul>
 *
 * <p>All generated IR follows the EK9 tri-state semantics where operations
 * return unset if any operand is unset.</p>
 */
public abstract class AbstractSyntheticGenerator {

  protected final IRGenerationContext stackContext;

  /**
   * Create a new synthetic generator with the given context.
   *
   * @param stackContext The IR generation context
   */
  protected AbstractSyntheticGenerator(final IRGenerationContext stackContext) {
    AssertValue.checkNotNull("stackContext cannot be null", stackContext);
    this.stackContext = stackContext;
  }

  /**
   * Get the fully qualified name for a type.
   *
   * @param symbol The symbol to get the type name for
   * @return The fully qualified type name
   */
  protected String getTypeName(final ISymbol symbol) {
    return symbol.getType()
        .map(ISymbol::getFullyQualifiedName)
        .orElse(symbol.getFullyQualifiedName());
  }

  /**
   * Get the Boolean type name from the EK9 type system.
   */
  protected String getBooleanTypeName() {
    return stackContext.getParsedModule().getEk9Types().ek9Boolean().getFullyQualifiedName();
  }

  /**
   * Get the Integer type name from the EK9 type system.
   */
  protected String getIntegerTypeName() {
    return stackContext.getParsedModule().getEk9Types().ek9Integer().getFullyQualifiedName();
  }

  /**
   * Get the String type name from the EK9 type system.
   */
  protected String getStringTypeName() {
    return stackContext.getParsedModule().getEk9Types().ek9String().getFullyQualifiedName();
  }

  /**
   * Get the Bits type name from the EK9 type system.
   */
  protected String getBitsTypeName() {
    return stackContext.getParsedModule().getEk9Types().ek9Bits().getFullyQualifiedName();
  }

  /**
   * Get the Void type name from the EK9 type system.
   */
  protected String getVoidTypeName() {
    return stackContext.getParsedModule().getEk9Types().ek9Void().getFullyQualifiedName();
  }

  /**
   * Generate a temporary variable name.
   */
  protected String generateTempName() {
    return stackContext.generateTempName();
  }

  /**
   * Generate a label name with the given prefix.
   */
  protected String generateLabelName(final String prefix) {
    return stackContext.generateLabelName(prefix);
  }

  /**
   * Create debug info from a symbol's source token.
   */
  protected DebugInfo createDebugInfo(final ISymbol symbol) {
    return stackContext.createDebugInfo(symbol.getSourceToken());
  }

  // ========== Common IR Generation Patterns ==========

  /**
   * Generate isSet guard check for 'this' with branch to unset return.
   *
   * <p>Pattern:</p>
   * <pre>
   *   _temp = CALL this._isSet() -> Boolean
   *   RETAIN _temp
   *   SCOPE_REGISTER _temp, scope_id
   *   _temp_val = UNBOX _temp -> boolean
   *   BRANCH_IF_FALSE _temp_val -> unset_label
   * </pre>
   *
   * @param aggregateTypeName The fully qualified type name of the aggregate
   * @param debugInfo         Debug information for the instructions
   * @param unsetLabel        Label to branch to if unset
   * @param scopeId           Current scope ID for memory management
   * @return List of IR instructions for the guard
   */
  protected List<IRInstr> generateThisIsSetGuard(final String aggregateTypeName,
                                                 final DebugInfo debugInfo,
                                                 final String unsetLabel,
                                                 final String scopeId) {
    return generateIsSetGuard(IRConstants.THIS, aggregateTypeName, debugInfo, unsetLabel, scopeId);
  }

  /**
   * Generate isSet guard check for a variable with branch to unset return.
   *
   * <p>The check generates:</p>
   * <pre>
   *   1. _temp = CALL variable._isSet() -> Boolean
   *   2. RETAIN/SCOPE_REGISTER _temp
   *   3. _tempBool = CALL _temp._true() -> boolean (primitive)
   *   4. RETAIN/SCOPE_REGISTER _tempBool
   *   5. BRANCH_FALSE _tempBool, unsetLabel
   * </pre>
   *
   * @param variableName The variable to check
   * @param typeName     The fully qualified type name of the variable
   * @param debugInfo    Debug information for the instructions
   * @param unsetLabel   Label to branch to if unset
   * @param scopeId      Current scope ID for memory management
   * @return List of IR instructions for the guard
   */
  protected List<IRInstr> generateIsSetGuard(final String variableName,
                                             final String typeName,
                                             final DebugInfo debugInfo,
                                             final String unsetLabel,
                                             final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Generate temp name for isSet result
    final var isSetResult = generateTempName();

    // Call _isSet() on the variable - returns Boolean object
    // isTraitCall=false because we're calling on a class instance, not through a trait reference
    final var callDetails = new CallDetails(
        variableName,
        typeName,
        "_isSet",
        List.of(),
        getBooleanTypeName(),
        List.of(),
        CallMetaDataDetails.defaultMetaData(),
        false // isTraitCall - class method call uses invokevirtual
    );
    instructions.add(CallInstr.call(isSetResult, debugInfo, callDetails));

    // Memory management for the Boolean result
    instructions.add(MemoryInstr.retain(isSetResult, debugInfo));
    instructions.add(ScopeInstr.register(isSetResult, scopeId, debugInfo));

    // Convert Boolean to primitive boolean via _true()
    // Note: Primitive results don't need RETAIN/SCOPE_REGISTER - they're stack values
    final var primitiveBoolResult = generateTempName();
    final var trueCallDetails = new CallDetails(
        isSetResult,
        getBooleanTypeName(),
        "_true",
        List.of(),
        "boolean",
        List.of(),
        CallMetaDataDetails.defaultMetaData(),
        false // isTraitCall - class method call uses invokevirtual
    );
    instructions.add(CallInstr.call(primitiveBoolResult, debugInfo, trueCallDetails));

    // Branch if not set (primitive boolean is false/0)
    // No memory management needed for primitive - it's stored directly with ISTORE
    instructions.add(BranchInstr.branchIfFalse(primitiveBoolResult, unsetLabel, debugInfo));

    return instructions;
  }

  /**
   * Generate the return_unset basic block that returns an unset value.
   *
   * <p>Pattern for Boolean return:</p>
   * <pre>
   *   return_unset:
   *     _result = CALL Boolean._new() -> Boolean
   *     RETAIN _result
   *     STORE rtn = _result
   *     SCOPE_EXIT scope_id
   *     RETURN rtn
   * </pre>
   *
   * @param returnTypeName The type of value to return (e.g., Boolean, Integer)
   * @param returnVarName  The name of the return variable (typically "rtn")
   * @param debugInfo      Debug information for the instructions
   * @param scopeId        Current scope ID for cleanup
   * @return List of IR instructions for the unset return block
   */
  protected List<IRInstr> generateUnsetReturnBlock(final String returnTypeName,
                                                   final String returnVarName,
                                                   final DebugInfo debugInfo,
                                                   final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label for the unset return block
    instructions.add(LabelInstr.label(generateLabelName("return_unset")));

    // Create unset value via constructor call
    // Pattern: CALL (Type)Type.<init>() - creates new instance with NEW + DUP + INVOKESPECIAL
    final var resultTemp = generateTempName();
    final var callDetails = new CallDetails(
        returnTypeName, // Target is the class name for constructor calls
        returnTypeName,
        "<init>",
        List.of(),
        returnTypeName,
        List.of(),
        CallMetaDataDetails.defaultMetaData(),
        false // isTraitCall - constructor uses invokespecial
    );
    instructions.add(CallInstr.call(resultTemp, debugInfo, callDetails));

    // Memory management for temp
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

  /**
   * Generate a return block that returns a specific boolean value (true or false).
   *
   * @param value         The boolean value to return (true or false)
   * @param returnVarName The name of the return variable
   * @param debugInfo     Debug information
   * @param scopeId       Current scope ID
   * @return List of IR instructions
   */
  protected List<IRInstr> generateBooleanReturnBlock(final boolean value,
                                                     final String returnVarName,
                                                     final DebugInfo debugInfo,
                                                     final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Label
    final var labelName = value ? "return_true" : "return_false";
    instructions.add(LabelInstr.label(generateLabelName(labelName)));

    // Create Boolean via Boolean._of(value)
    final var resultTemp = generateTempName();
    final var callDetails = new CallDetails(
        null, // Static call
        getBooleanTypeName(),
        "_of",
        List.of("boolean"),
        getBooleanTypeName(),
        List.of(String.valueOf(value)),
        CallMetaDataDetails.defaultMetaData(),
        false // isTraitCall - static call doesn't use invokeinterface
    );
    instructions.add(CallInstr.callStatic(resultTemp, debugInfo, callDetails));

    // Memory management for temp
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

  /**
   * Get all fields from an aggregate that should be included in synthetic operations.
   *
   * <p>This returns only the direct properties of the aggregate, not inherited ones.
   * Inherited fields are handled by calling super's synthetic operators.</p>
   *
   * @param aggregateSymbol The aggregate to get fields from
   * @return List of field symbols
   */
  protected List<ISymbol> getSyntheticFields(final AggregateSymbol aggregateSymbol) {
    return new ArrayList<>(aggregateSymbol.getProperties());
  }

  /**
   * Check if the super aggregate has a specific operator defined.
   *
   * @param aggregateSymbol The aggregate whose super we're checking
   * @param operatorName    The operator name (e.g., "==", "&lt;=&gt;")
   * @return true if super has the operator, false otherwise
   */
  protected boolean superHasOperator(final AggregateSymbol aggregateSymbol,
                                     final String operatorName) {
    final var superOpt = aggregateSymbol.getSuperAggregate();
    if (superOpt.isEmpty()) {
      return false;
    }

    final var superAggregate = superOpt.get();
    if (isAnyType(superAggregate)) {
      return false;
    }

    // Check if super has this operator in its own scope (not inherited)
    return superAggregate.getAllMethodInThisScopeOnly().stream()
        .filter(MethodSymbol::isOperator)
        .anyMatch(method -> method.getName().equals(operatorName));
  }

  /**
   * Check if the given type is the Any type.
   */
  protected boolean isAnyType(final IAggregateSymbol type) {
    final var anyType = stackContext.getParsedModule().getEk9Types().ek9Any();
    return anyType.isExactSameType(type);
  }

  /**
   * Get the fully qualified name of the super aggregate.
   *
   * @param aggregateSymbol The aggregate to get super from
   * @return The super type name, or empty string if no super (other than Any)
   */
  protected String getSuperTypeName(final AggregateSymbol aggregateSymbol) {
    return aggregateSymbol.getSuperAggregate()
        .filter(superAgg -> !isAnyType(superAgg))
        .map(ISymbol::getFullyQualifiedName)
        .orElse("");
  }

  /**
   * Generate field load instruction with memory management.
   *
   * <p>Pattern:</p>
   * <pre>
   *   _temp = LOAD this.fieldName -> FieldType
   *   RETAIN _temp
   *   SCOPE_REGISTER _temp, scope_id
   * </pre>
   *
   * @param targetVar The variable to store the loaded value
   * @param objectVar The object to load from (typically "this" or "other")
   * @param fieldName The field name to load
   * @param debugInfo Debug information
   * @param scopeId   Current scope ID
   * @return List of IR instructions
   */
  protected List<IRInstr> generateFieldLoad(final String targetVar,
                                            final String objectVar,
                                            final String fieldName,
                                            final DebugInfo debugInfo,
                                            final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Generate LOAD instruction - source location is "object.field"
    final var sourceLocation = objectVar + "." + fieldName;
    instructions.add(MemoryInstr.load(targetVar, sourceLocation, debugInfo));

    // Memory management
    instructions.add(MemoryInstr.retain(targetVar, debugInfo));
    instructions.add(ScopeInstr.register(targetVar, scopeId, debugInfo));

    return instructions;
  }

  /**
   * Generate a constructor call to create a new instance.
   *
   * <p>Pattern:</p>
   * <pre>
   *   _result = CALL (Type)Type.&lt;init&gt;() [pure=true, complexity=1, effects=RETURN_MUTATION]
   *   RETAIN _result
   *   SCOPE_REGISTER _result, scope_id
   * </pre>
   *
   * @param resultVar Variable to store result
   * @param typeName  Fully qualified type name to construct
   * @param debugInfo Debug information
   * @param scopeId   Current scope ID
   * @return List of IR instructions
   */
  protected List<IRInstr> generateConstructorCall(final String resultVar,
                                                   final String typeName,
                                                   final DebugInfo debugInfo,
                                                   final String scopeId) {
    return generateConstructorCallWithArgs(resultVar, typeName, List.of(), List.of(), debugInfo, scopeId);
  }

  /**
   * Generate a constructor call with arguments to create a new instance.
   *
   * <p>Pattern:</p>
   * <pre>
   *   _result = CALL (Type)Type.&lt;init&gt;(args) [pure=true, complexity=1, effects=RETURN_MUTATION]
   *   RETAIN _result
   *   SCOPE_REGISTER _result, scope_id
   * </pre>
   *
   * @param resultVar      Variable to store result
   * @param typeName       Fully qualified type name to construct
   * @param arguments      Argument variable names to pass to constructor
   * @param parameterTypes Parameter types (must match arguments)
   * @param debugInfo      Debug information
   * @param scopeId        Current scope ID
   * @return List of IR instructions
   */
  protected List<IRInstr> generateConstructorCallWithArgs(final String resultVar,
                                                           final String typeName,
                                                           final List<String> arguments,
                                                           final List<String> parameterTypes,
                                                           final DebugInfo debugInfo,
                                                           final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Generate constructor CALL instruction
    // Target is the class name for constructor calls (triggers NEW + DUP + INVOKESPECIAL pattern)
    final var callDetails = new CallDetails(
        typeName, // Target is the class name
        typeName,
        "<init>",
        parameterTypes,
        typeName,
        arguments,
        CallMetaDataDetails.defaultMetaData(),
        false // isTraitCall - constructor uses invokespecial
    );
    instructions.add(CallInstr.call(resultVar, debugInfo, callDetails));

    // Memory management
    instructions.add(MemoryInstr.retain(resultVar, debugInfo));
    instructions.add(ScopeInstr.register(resultVar, scopeId, debugInfo));

    return instructions;
  }

  /**
   * Generate a string literal load with memory management.
   *
   * <p>Pattern:</p>
   * <pre>
   *   _temp = LOAD_LITERAL "value", org.ek9.lang::String
   *   RETAIN _temp
   *   SCOPE_REGISTER _temp, scope_id
   * </pre>
   *
   * @param resultVar    Variable to store the loaded literal
   * @param literalValue The string literal value
   * @param debugInfo    Debug information
   * @param scopeId      Current scope ID
   * @return List of IR instructions
   */
  protected List<IRInstr> generateStringLiteralLoad(final String resultVar,
                                                     final String literalValue,
                                                     final DebugInfo debugInfo,
                                                     final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Generate LOAD_LITERAL instruction for the string
    instructions.add(LiteralInstr.literal(resultVar, literalValue, getStringTypeName(), debugInfo));

    // Memory management
    instructions.add(MemoryInstr.retain(resultVar, debugInfo));
    instructions.add(ScopeInstr.register(resultVar, scopeId, debugInfo));

    return instructions;
  }

  /**
   * Generate unset return block at a specific label.
   *
   * <p>Unlike {@link #generateUnsetReturnBlock}, this version uses the
   * provided label name instead of generating a new one. Use this when
   * you need to branch to the unset return from multiple locations.</p>
   *
   * @param labelName      The label name for this block
   * @param returnTypeName The type of value to return
   * @param returnVarName  The name of the return variable
   * @param debugInfo      Debug information
   * @param scopeId        Current scope ID
   * @return List of IR instructions for the unset return block
   */
  protected List<IRInstr> generateUnsetReturnBlockWithLabel(final String labelName,
                                                             final String returnTypeName,
                                                             final String returnVarName,
                                                             final DebugInfo debugInfo,
                                                             final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Use the provided label name
    instructions.add(LabelInstr.label(labelName));

    // Create unset value via constructor call
    final var resultTemp = generateTempName();
    instructions.addAll(generateConstructorCall(resultTemp, returnTypeName, debugInfo, scopeId));

    // Store to return variable and retain for ownership transfer
    instructions.add(MemoryInstr.store(returnVarName, resultTemp, debugInfo));
    instructions.add(MemoryInstr.retain(returnVarName, debugInfo));

    // Scope cleanup and return
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(returnVarName, debugInfo));

    return instructions;
  }

  /**
   * Generate a method call on a variable with result storage and memory management.
   *
   * @param resultVar      Variable to store result
   * @param targetVar      Variable to call method on (null for static calls)
   * @param targetType     Fully qualified type name of the target
   * @param methodName     Method to call
   * @param arguments      Method argument variable names
   * @param parameterTypes Parameter types (must match arguments)
   * @param returnType     Return type of method
   * @param debugInfo      Debug information
   * @param scopeId        Current scope ID
   * @return List of IR instructions
   */
  protected List<IRInstr> generateMethodCall(final String resultVar,
                                             final String targetVar,
                                             final String targetType,
                                             final String methodName,
                                             final List<String> arguments,
                                             final List<String> parameterTypes,
                                             final String returnType,
                                             final DebugInfo debugInfo,
                                             final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Generate CALL instruction
    // isTraitCall=false because we're calling on class instances, not through trait references
    final var callDetails = new CallDetails(
        targetVar,
        targetType,
        methodName,
        parameterTypes,
        returnType,
        arguments,
        CallMetaDataDetails.defaultMetaData(),
        false // isTraitCall - class method call uses invokevirtual
    );

    // Use static call if no target variable
    if (targetVar == null) {
      instructions.add(CallInstr.callStatic(resultVar, debugInfo, callDetails));
    } else {
      instructions.add(CallInstr.call(resultVar, debugInfo, callDetails));
    }

    // Memory management for non-void results
    if (resultVar != null) {
      instructions.add(MemoryInstr.retain(resultVar, debugInfo));
      instructions.add(ScopeInstr.register(resultVar, scopeId, debugInfo));
    }

    return instructions;
  }
}
