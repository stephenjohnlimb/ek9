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
 * Generates synthetic IR for the _json ($$) operator.
 *
 * <p>The generated code follows this pattern:</p>
 * <pre>
 * JSON _json():
 *   // Check if any field is set using Bits._empty()
 *   status = this._fieldSetStatus()  // Returns Bits
 *   isEmpty = status._empty()        // Returns Boolean (true if no fields set)
 *   if isEmpty._true(): goto return_unset
 *
 *   // has_data: at least one field is set
 *   jsonTemp = JSON()
 *   result = jsonTemp.object()
 *
 *   // For each field:
 *   fieldValue = LOAD this.field
 *   fieldJson = fieldValue._json()         // Recursive JSON conversion
 *   nameString = "fieldName" (literal)
 *   pair = JSON(nameString, fieldJson)     // Create name/value pair
 *   result._merge(pair)                    // Merge into result
 *
 *   return result
 *
 *   return_unset:
 *     return new JSON()  // Unset JSON
 * </pre>
 *
 * <p>Semantic: If ALL fields are unset, the object has no meaningful data,
 * so we return an unset JSON. If ANY field is set, we proceed with normal
 * JSON construction. This is consistent with _string and _hashcode semantics.</p>
 *
 * <p>Key semantic requirements:</p>
 * <ul>
 *   <li>Returns unset JSON if all fields are unset</li>
 *   <li>Returns set JSON if any field is set</li>
 *   <li>Format: {"field1": value1, "field2": value2}</li>
 *   <li>Each field value is converted via $$ (_json) recursively</li>
 *   <li>Unset field values become JSON null (handled by JSON constructor)</li>
 *   <li>Lists produce JSON arrays, Dicts produce nested objects</li>
 * </ul>
 *
 * <p>Uses Bits._empty() instead of N individual _isSet() calls for efficiency.</p>
 */
final class ToJsonGenerator extends AbstractSyntheticGenerator {

  private static final String RETURN_VAR = "rtn";

  ToJsonGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  /**
   * Generate the _json operator IR for the given aggregate.
   *
   * @param operatorSymbol  The _json operator symbol
   * @param aggregateSymbol The aggregate containing the operator
   * @return List of IR instructions implementing the operator
   */
  List<IRInstr> generate(final MethodSymbol operatorSymbol,
                         final AggregateSymbol aggregateSymbol) {

    AssertValue.checkNotNull("operatorSymbol cannot be null", operatorSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = createDebugInfo(operatorSymbol);
    final var scopeId = stackContext.generateScopeId("_json");
    final var returnUnsetLabel = generateLabelName("return_unset");
    final var aggregateTypeName = aggregateSymbol.getFullyQualifiedName();

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Reference return variable
    instructions.add(MemoryInstr.reference(RETURN_VAR, getJsonTypeName(), debugInfo));

    // Check if any field is set using _fieldSetStatus()._empty()
    // This replaces N _isSet() calls with 2 method calls
    instructions.addAll(generateEmptyFieldCheck(aggregateTypeName, returnUnsetLabel, debugInfo, scopeId));

    // If we get here, at least one field is set - proceed with JSON construction
    final var fields = getSyntheticFields(aggregateSymbol);

    // Create empty JSON object: result = JSON().object()
    instructions.addAll(createEmptyJsonObject(debugInfo, scopeId));

    // Process each field
    for (final var field : fields) {
      instructions.addAll(generateFieldJsonContribution(field, debugInfo, scopeId));
    }

    // Return the result
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(RETURN_VAR, debugInfo));

    // Unset return block - return new unset JSON
    instructions.addAll(generateUnsetReturnBlockWithLabel(
        returnUnsetLabel,
        getJsonTypeName(),
        RETURN_VAR,
        debugInfo,
        scopeId));

    return instructions;
  }

  /**
   * Get the JSON type name from the EK9 type system.
   */
  private String getJsonTypeName() {
    return stackContext.getParsedModule().getEk9Types().ek9Json().getFullyQualifiedName();
  }

  /**
   * Generate check if all fields are unset using _fieldSetStatus()._empty().
   *
   * <p>Pattern:</p>
   * <pre>
   *   status = this._fieldSetStatus()  // Returns Bits
   *   isEmpty = status._empty()        // Returns Boolean (true if no bits set)
   *   isEmptyBool = isEmpty._true()    // Extract boolean
   *   BRANCH_TRUE isEmptyBool, returnUnsetLabel
   * </pre>
   *
   * <p>This is more efficient than checking each field individually - just 2 method calls
   * instead of N _isSet() calls.</p>
   */
  private List<IRInstr> generateEmptyFieldCheck(final String aggregateTypeName,
                                                  final String returnUnsetLabel,
                                                  final DebugInfo debugInfo,
                                                  final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Call this._fieldSetStatus() -> Bits
    final var statusVar = generateTempName();
    instructions.addAll(generateMethodCall(
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

    // Call status._empty() -> Boolean
    final var isEmptyVar = generateTempName();
    instructions.addAll(generateMethodCall(
        isEmptyVar,
        statusVar,
        getBitsTypeName(),
        "_empty",
        List.of(),
        List.of(),
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Extract boolean via _true()
    final var isEmptyBoolVar = generateTempName();
    instructions.addAll(generateMethodCall(
        isEmptyBoolVar,
        isEmptyVar,
        getBooleanTypeName(),
        "_true",
        List.of(),
        List.of(),
        "boolean",
        debugInfo,
        scopeId
    ));

    // If empty (no fields set), branch to unset return
    instructions.add(BranchInstr.branchIfTrue(isEmptyBoolVar, returnUnsetLabel, debugInfo));

    return instructions;
  }

  /**
   * Create an empty JSON object and store in return variable.
   *
   * <p>Pattern:</p>
   * <pre>
   *   jsonTemp = CALL JSON.<init>() -> JSON    // Create JSON instance
   *   objectResult = CALL jsonTemp.object() -> JSON  // Make it an empty object {}
   *   STORE rtn = objectResult
   * </pre>
   */
  private List<IRInstr> createEmptyJsonObject(final DebugInfo debugInfo,
                                               final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Create JSON instance via default constructor
    final var jsonTemp = generateTempName();
    instructions.addAll(generateConstructorCall(jsonTemp, getJsonTypeName(), debugInfo, scopeId));

    // Call object() method to create empty JSON object {}
    final var objectResult = generateTempName();
    instructions.addAll(generateMethodCall(
        objectResult,
        jsonTemp,
        getJsonTypeName(),
        "object",
        List.of(),
        List.of(),
        getJsonTypeName(),
        debugInfo,
        scopeId
    ));

    // Store as result
    instructions.add(MemoryInstr.store(RETURN_VAR, objectResult, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));

    return instructions;
  }

  /**
   * Generate JSON contribution for a single field.
   *
   * <p>Pattern:</p>
   * <pre>
   *   fieldValue = LOAD this.field
   *   fieldJson = CALL fieldValue._json() -> JSON    // Recursive conversion
   *   nameString = LITERAL "fieldName"
   *   pair = CALL JSON.<init>(nameString, fieldJson) -> JSON  // Name/value pair
   *   CALL result._merge(pair)                       // Merge into result
   * </pre>
   */
  private List<IRInstr> generateFieldJsonContribution(final ISymbol field,
                                                       final DebugInfo debugInfo,
                                                       final String scopeId) {

    final var fieldName = field.getName();
    final var fieldTypeName = getTypeName(field);
    final var instructions = new ArrayList<IRInstr>();

    // Load field value
    final var fieldVar = generateTempName();
    instructions.addAll(generateFieldLoad(fieldVar, IRConstants.THIS, fieldName, debugInfo, scopeId));

    // Call field._json() to get JSON representation
    final var fieldJsonVar = generateTempName();
    instructions.addAll(generateMethodCall(
        fieldJsonVar,
        fieldVar,
        fieldTypeName,
        "_json",
        List.of(),
        List.of(),
        getJsonTypeName(),
        debugInfo,
        scopeId
    ));

    // Create string literal for field name
    final var nameStringVar = generateTempName();
    instructions.add(LiteralInstr.literal(nameStringVar, fieldName, getStringTypeName(), debugInfo));
    instructions.add(MemoryInstr.retain(nameStringVar, debugInfo));
    instructions.add(ScopeInstr.register(nameStringVar, scopeId, debugInfo));

    // Create JSON name/value pair via JSON(String, JSON) constructor
    final var pairVar = generateTempName();
    instructions.addAll(generateTwoArgConstructorCall(
        pairVar,
        getJsonTypeName(),
        nameStringVar,
        getStringTypeName(),
        fieldJsonVar,
        getJsonTypeName(),
        debugInfo,
        scopeId
    ));

    // Merge pair into result via _merge method
    instructions.addAll(generateVoidMethodCall(
        RETURN_VAR,
        getJsonTypeName(),
        "_merge",
        List.of(pairVar),
        List.of(getJsonTypeName()),
        debugInfo,
        scopeId
    ));

    return instructions;
  }

  /**
   * Generate a constructor call with two arguments.
   *
   * @param resultVar    Variable to store result
   * @param typeName     Fully qualified type name to construct
   * @param arg1Var      First argument variable name
   * @param arg1Type     First argument type
   * @param arg2Var      Second argument variable name
   * @param arg2Type     Second argument type
   * @param debugInfo    Debug information
   * @param scopeId      Current scope ID
   * @return List of IR instructions
   */
  private List<IRInstr> generateTwoArgConstructorCall(final String resultVar,
                                                       final String typeName,
                                                       final String arg1Var,
                                                       final String arg1Type,
                                                       final String arg2Var,
                                                       final String arg2Type,
                                                       final DebugInfo debugInfo,
                                                       final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Generate constructor CALL with two arguments
    instructions.addAll(generateMethodCall(
        resultVar,
        typeName, // Target is class name for constructor
        typeName,
        "<init>",
        List.of(arg1Var, arg2Var),
        List.of(arg1Type, arg2Type),
        typeName,
        debugInfo,
        scopeId
    ));

    return instructions;
  }

  /**
   * Generate a void method call (no return value).
   *
   * @param targetVar      Variable to call method on
   * @param targetType     Fully qualified type name of the target
   * @param methodName     Method to call
   * @param arguments      Method argument variable names
   * @param parameterTypes Parameter types
   * @param debugInfo      Debug information
   * @param scopeId        Current scope ID
   * @return List of IR instructions
   */
  private List<IRInstr> generateVoidMethodCall(final String targetVar,
                                                final String targetType,
                                                final String methodName,
                                                final List<String> arguments,
                                                final List<String> parameterTypes,
                                                final DebugInfo debugInfo,
                                                final String scopeId) {
    // Use null for result var since it's void
    return generateMethodCall(
        null,
        targetVar,
        targetType,
        methodName,
        arguments,
        parameterTypes,
        getVoidTypeName(),
        debugInfo,
        scopeId
    );
  }
}
