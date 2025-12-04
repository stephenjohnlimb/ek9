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
 * Generates synthetic IR for the _string ($) operator.
 *
 * <p>The generated code follows this pattern:</p>
 * <pre>
 * String _string():
 *   result = "ClassName("
 *   needsSeparator = false
 *
 *   // For each field:
 *   if field._isSet():
 *     if needsSeparator:
 *       result = result + ", "
 *     result = result + "fieldName="
 *     result = result + field.$()
 *     needsSeparator = true
 *   else:
 *     if needsSeparator:
 *       result = result + ", "
 *     result = result + "fieldName=?"
 *     needsSeparator = true
 *
 *   result = result + ")"
 *   return result
 * </pre>
 *
 * <p>Key semantic requirements:</p>
 * <ul>
 *   <li>Returns String (always set)</li>
 *   <li>Format: "ClassName(field1=value1, field2=value2)"</li>
 *   <li>SET fields show their string value via $</li>
 *   <li>UNSET fields show "?" as placeholder</li>
 *   <li>Uses simple class name (not fully qualified)</li>
 * </ul>
 */
final class ToStringGenerator extends AbstractSyntheticGenerator {

  private static final String RETURN_VAR = "rtn";
  private static final String SEPARATOR = ", ";
  private static final String UNSET_MARKER = "?";

  ToStringGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  /**
   * Generate the _string operator IR for the given aggregate.
   *
   * @param operatorSymbol  The _string operator symbol
   * @param aggregateSymbol The aggregate containing the operator
   * @return List of IR instructions implementing the operator
   */
  List<IRInstr> generate(final MethodSymbol operatorSymbol,
                         final AggregateSymbol aggregateSymbol) {

    AssertValue.checkNotNull("operatorSymbol cannot be null", operatorSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = createDebugInfo(operatorSymbol);
    final var scopeId = stackContext.generateScopeId("_string");

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Reference return variable
    instructions.add(MemoryInstr.reference(RETURN_VAR, getStringTypeName(), debugInfo));

    // Initialize result with class name and opening parenthesis
    final var className = aggregateSymbol.getName();
    instructions.addAll(initializeResultString(className + "(", debugInfo, scopeId));

    // Process each field
    final var fields = getSyntheticFields(aggregateSymbol);
    boolean isFirstField = true;
    for (final var field : fields) {
      instructions.addAll(generateFieldStringContribution(field, isFirstField, debugInfo, scopeId));
      isFirstField = false;
    }

    // Append closing parenthesis
    instructions.addAll(appendStringLiteral(")", debugInfo, scopeId));

    // Return the result
    instructions.add(ScopeInstr.exit(scopeId, debugInfo));
    instructions.add(BranchInstr.returnValue(RETURN_VAR, debugInfo));

    return instructions;
  }

  /**
   * Initialize result with the given string literal.
   */
  private List<IRInstr> initializeResultString(final String value,
                                                final DebugInfo debugInfo,
                                                final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Create string literal
    final var literalVar = generateTempName();
    instructions.add(LiteralInstr.literal(literalVar, value, getStringTypeName(), debugInfo));
    instructions.add(MemoryInstr.retain(literalVar, debugInfo));
    instructions.add(ScopeInstr.register(literalVar, scopeId, debugInfo));

    // Store as initial result
    instructions.add(MemoryInstr.store(RETURN_VAR, literalVar, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));

    return instructions;
  }

  /**
   * Append a string literal to the result.
   */
  private List<IRInstr> appendStringLiteral(final String value,
                                             final DebugInfo debugInfo,
                                             final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Create string literal
    final var literalVar = generateTempName();
    instructions.add(LiteralInstr.literal(literalVar, value, getStringTypeName(), debugInfo));
    instructions.add(MemoryInstr.retain(literalVar, debugInfo));
    instructions.add(ScopeInstr.register(literalVar, scopeId, debugInfo));

    // Concatenate: result = result + literal via _add method (+ operator)
    final var concatenatedVar = generateTempName();
    instructions.addAll(generateMethodCall(
        concatenatedVar,
        RETURN_VAR,
        getStringTypeName(),
        "_add",
        List.of(literalVar),
        List.of(getStringTypeName()),
        getStringTypeName(),
        debugInfo,
        scopeId
    ));

    // Store result - must RELEASE old value first for ARC
    instructions.add(MemoryInstr.release(RETURN_VAR, debugInfo));
    instructions.add(MemoryInstr.store(RETURN_VAR, concatenatedVar, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));

    return instructions;
  }

  /**
   * Append a string variable to the result.
   */
  private List<IRInstr> appendStringVariable(final String varName,
                                              final DebugInfo debugInfo,
                                              final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Concatenate: result = result + var via _add method (+ operator)
    final var concatenatedVar = generateTempName();
    instructions.addAll(generateMethodCall(
        concatenatedVar,
        RETURN_VAR,
        getStringTypeName(),
        "_add",
        List.of(varName),
        List.of(getStringTypeName()),
        getStringTypeName(),
        debugInfo,
        scopeId
    ));

    // Store result - must RELEASE old value first for ARC
    instructions.add(MemoryInstr.release(RETURN_VAR, debugInfo));
    instructions.add(MemoryInstr.store(RETURN_VAR, concatenatedVar, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));

    return instructions;
  }

  /**
   * Generate string contribution for a single field.
   *
   * <p>Pattern:</p>
   * <pre>
   *   fieldValue = LOAD this.field
   *   isSetResult = CALL fieldValue._isSet()
   *   isSetBool = CALL isSetResult._true()
   *   BRANCH_FALSE isSetBool, unset_label
   *
   *   // Field is set - append "fieldName=" + field.$()
   *   [append separator if not first]
   *   result = result + "fieldName="
   *   fieldString = CALL fieldValue.$()
   *   result = result + fieldString
   *   BRANCH done_label
   *
   *   unset_label:
   *   // Field is unset - append "fieldName=?"
   *   [append separator if not first]
   *   result = result + "fieldName=?"
   *
   *   done_label:
   * </pre>
   */
  private List<IRInstr> generateFieldStringContribution(final ISymbol field,
                                                         final boolean isFirstField,
                                                         final DebugInfo debugInfo,
                                                         final String scopeId) {

    final var fieldName = field.getName();
    final var fieldTypeName = getTypeName(field);
    final var instructions = new ArrayList<IRInstr>();

    // Labels for control flow
    final var unsetLabel = generateLabelName("field_unset");
    final var doneLabel = generateLabelName("field_done");

    // Load field value
    final var fieldVar = generateTempName();
    instructions.addAll(generateFieldLoad(fieldVar, IRConstants.THIS, fieldName, debugInfo, scopeId));

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

    // Branch to unset handling if not set
    instructions.add(BranchInstr.branchIfFalse(isSetBoolVar, unsetLabel, debugInfo));

    // === Field is SET path ===
    // Add separator if not first field
    if (!isFirstField) {
      instructions.addAll(appendStringLiteral(SEPARATOR, debugInfo, scopeId));
    }

    // Append "fieldName="
    instructions.addAll(appendStringLiteral(fieldName + "=", debugInfo, scopeId));

    // Get field's string representation via _string method ($ operator internal name)
    final var fieldStringVar = generateTempName();
    instructions.addAll(generateMethodCall(
        fieldStringVar,
        fieldVar,
        fieldTypeName,
        "_string",
        List.of(),
        List.of(),
        getStringTypeName(),
        debugInfo,
        scopeId
    ));

    // Append field's string value
    instructions.addAll(appendStringVariable(fieldStringVar, debugInfo, scopeId));

    // Jump to done
    instructions.add(BranchInstr.branch(doneLabel, debugInfo));

    // === Field is UNSET path ===
    instructions.add(LabelInstr.label(unsetLabel));

    // Add separator if not first field
    if (!isFirstField) {
      instructions.addAll(appendStringLiteral(SEPARATOR, debugInfo, scopeId));
    }

    // Append "fieldName=?"
    instructions.addAll(appendStringLiteral(fieldName + "=" + UNSET_MARKER, debugInfo, scopeId));

    // Done label
    instructions.add(LabelInstr.label(doneLabel));

    return instructions;
  }
}
