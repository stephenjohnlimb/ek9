package org.ek9lang.compiler.phase7;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.IRFrameType;
import org.ek9lang.compiler.phase7.support.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRInstructionBuilder;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;

/**
 * Handles IR generation for enumeration operators (16+ operators).
 *
 * <p>EK9 enumerations automatically receive extensive operator support including
 * ordinal-based comparisons, string-enum hybrid operations, and utility operators.
 * This demonstrates the complexity that requires stack-based context management.</p>
 *
 * <p>Key capabilities:</p>
 * <ul>
 * <li>7 Comparison operators: &lt;=&gt; == &lt;&gt; &lt; &lt;= &gt; &gt;= (ordinal-based)</li>
 * <li>7 String-hybrid operators: CardSuit.Hearts == "Hearts" (string conversion + compare)</li>
 * <li>5+ Utility operators: ? $ #^ $$ #? #&lt; #&gt; (various functions)</li>
 * </ul>
 */
public class EnumerationIRHelper extends AbstractIRHelper {

  // Map-based dispatch for enumeration operators
  private final Map<String, BiFunction<MethodSymbol, AggregateSymbol, List<IRInstr>>> enumerationOperators;

  /**
   * Create the enumeration IR helper.
   */
  public EnumerationIRHelper(IRGenerationContext context, IRInstructionBuilder instructionBuilder) {
    super(context, instructionBuilder);
    this.enumerationOperators = initializeEnumerationOperators();
  }

  /**
   * Generate IR for enumeration operators in the given symbol.
   */
  public void generateFor(ISymbol symbol) {
    if (!(symbol instanceof AggregateSymbol aggregateSymbol)) {
      return;
    }

    // Only process enumeration types
    if (aggregateSymbol.getGenus() != SymbolGenus.TYPE || !isEnumerationType(aggregateSymbol)) {
      return;
    }

    // Generate IR for all enumeration operators
    for (var method : aggregateSymbol.getAllMethods()) {
      if (isEnumerationOperator(method)) {
        generateEnumerationOperatorIR(method, aggregateSymbol);
      }
    }
  }

  /**
   * Generate IR for a specific enumeration operator.
   */
  private void generateEnumerationOperatorIR(MethodSymbol method, AggregateSymbol enumSymbol) {
    // TODO: Get debug info from method symbol once API is understood
    context.enterScope(method.getScopeName(), null, IRFrameType.ENUMERATION);

    try {
      var generator = enumerationOperators.get(getOperatorKey(method));
      if (generator != null) {
        var instructions = generator.apply(method, enumSymbol);
        context.addInstructions(instructions);
      } else {
        // Log warning about unknown enumeration operator
        System.err.println("Unknown enumeration operator: " + method.getName());
      }
    } finally {
      context.exitScope();
    }
  }

  /**
   * Initialize the map of enumeration operators to their IR generators.
   */
  private Map<String, BiFunction<MethodSymbol, AggregateSymbol, List<IRInstr>>> initializeEnumerationOperators() {
    var operators = new HashMap<String, BiFunction<MethodSymbol, AggregateSymbol, List<IRInstr>>>();

    // Ordinal-based comparison operators
    operators.put("<=>", this::generateEnumOrdinalComparison);
    operators.put("==", this::generateEnumEquality);
    operators.put("<>", this::generateEnumInequality);
    operators.put("<", this::generateEnumLessThan);
    operators.put("<=", this::generateEnumLessEqual);
    operators.put(">", this::generateEnumGreaterThan);
    operators.put(">=", this::generateEnumGreaterEqual);

    // Utility operators
    operators.put("?", this::generateEnumIsSetOperator);
    operators.put("$", this::generateEnumStringRepresentation);
    operators.put("#^", this::generateEnumPromoteOperator);
    operators.put("$$", this::generateEnumToJsonOperator);
    operators.put("#?", this::generateEnumHashcodeOperator);
    operators.put("#<", this::generateEnumFirstOperator);
    operators.put("#>", this::generateEnumLastOperator);

    // String-enum hybrid operators (handled specially)
    operators.put("==(String)", this::generateEnumStringEquality);
    operators.put("<=>(String)", this::generateEnumStringComparison);

    return operators;
  }

  /**
   * Get operator key for map lookup, handling string variants.
   */
  private String getOperatorKey(MethodSymbol method) {
    var operatorName = method.getName();

    // Check if this is a string variant operator
    if (method.getCallParameters().size() == 1) {
      var paramType = method.getCallParameters().get(0).getType();
      if (paramType.isPresent() && isStringType(paramType.get())) {
        return operatorName + "(String)";
      }
    }

    return operatorName;
  }

  /**
   * Check if symbol is an enumeration type.
   */
  private boolean isEnumerationType(AggregateSymbol symbol) {
    // Check if symbol was created as an enumeration
    // This would need to be determined from the symbol's creation context
    return symbol.getName().contains("Enum") || hasEnumerationValues(symbol);
  }

  /**
   * Check if symbol has enumeration values.
   */
  private boolean hasEnumerationValues(AggregateSymbol symbol) {
    // Check for enumeration-specific markers or structure
    return symbol.getProperties().stream()
        .anyMatch(prop -> prop.getName().matches("[A-Z][a-zA-Z]*")); // Enum value naming pattern
  }

  /**
   * Check if method is an enumeration operator.
   */
  private boolean isEnumerationOperator(MethodSymbol method) {
    // Check if method was added by OperatorFactory.addEnumerationMethods
    return method.isSynthetic() && enumerationOperators.containsKey(getOperatorKey(method));
  }

  /**
   * Check if type is String type.
   */
  private boolean isStringType(ISymbol type) {
    return type != null && "String".equals(type.getName());
  }

  // ===== ENUMERATION OPERATOR GENERATORS =====

  /**
   * Generate IR for enumeration <=> comparison (ordinal-based).
   */
  private List<IRInstr> generateEnumOrdinalComparison(MethodSymbol method, AggregateSymbol enumSymbol) {
    instructionBuilder.createBasicBlock("enum_cmp_entry");

    // Generate: this.ordinal() <=> other.ordinal()
    var thisOrdinal = instructionBuilder.createTempVariable();
    var otherOrdinal = instructionBuilder.createTempVariable();

    // TODO: Generate calls to get ordinal values
    // TODO: Generate integer comparison of ordinals

    var resultVar = instructionBuilder.createTempVariable();
    // Result is -1, 0, or 1 based on ordinal comparison

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration == equality.
   */
  private List<IRInstr> generateEnumEquality(MethodSymbol method, AggregateSymbol enumSymbol) {
    instructionBuilder.createBasicBlock("enum_eq_entry");

    // Generate: this.ordinal() == other.ordinal()
    var cmpResult = instructionBuilder.createTempVariable();
    // TODO: Generate call to <=> operator

    var resultVar = instructionBuilder.createTempVariable();
    instructionBuilder.createLiteral(resultVar, "false", "Boolean"); // Placeholder

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration <> inequality.
   */
  private List<IRInstr> generateEnumInequality(MethodSymbol method, AggregateSymbol enumSymbol) {
    return generateEnumComparisonDerived(method, enumSymbol, "enum_neq", "!= 0");
  }

  /**
   * Generate IR for enumeration < less than.
   */
  private List<IRInstr> generateEnumLessThan(MethodSymbol method, AggregateSymbol enumSymbol) {
    return generateEnumComparisonDerived(method, enumSymbol, "enum_lt", "< 0");
  }

  /**
   * Generate IR for enumeration <= less or equal.
   */
  private List<IRInstr> generateEnumLessEqual(MethodSymbol method, AggregateSymbol enumSymbol) {
    return generateEnumComparisonDerived(method, enumSymbol, "enum_lteq", "<= 0");
  }

  /**
   * Generate IR for enumeration > greater than.
   */
  private List<IRInstr> generateEnumGreaterThan(MethodSymbol method, AggregateSymbol enumSymbol) {
    return generateEnumComparisonDerived(method, enumSymbol, "enum_gt", "> 0");
  }

  /**
   * Generate IR for enumeration >= greater or equal.
   */
  private List<IRInstr> generateEnumGreaterEqual(MethodSymbol method, AggregateSymbol enumSymbol) {
    return generateEnumComparisonDerived(method, enumSymbol, "enum_gteq", ">= 0");
  }

  /**
   * Helper for enumeration comparison operators derived from <=>.
   */
  private List<IRInstr> generateEnumComparisonDerived(MethodSymbol method, AggregateSymbol enumSymbol,
                                                      String opName, String comparison) {
    instructionBuilder.createBasicBlock(opName + "_entry");

    // Generate: (this <=> other) comparison 0
    var cmpResult = instructionBuilder.createTempVariable();
    // TODO: Generate call to enumeration <=> operator

    var resultVar = instructionBuilder.createTempVariable();
    // TODO: Generate comparison with 0 using specified operation

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration ? isSet operator.
   */
  private List<IRInstr> generateEnumIsSetOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    instructionBuilder.createBasicBlock("enum_isSet_entry");

    // Check if enum value is set (not uninitialized)
    var resultVar = instructionBuilder.createTempVariable();

    // TODO: Generate check for valid enum ordinal value
    instructionBuilder.createLiteral(resultVar, "true", "Boolean"); // Placeholder

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration $ string representation.
   */
  private List<IRInstr> generateEnumStringRepresentation(MethodSymbol method, AggregateSymbol enumSymbol) {
    instructionBuilder.createBasicBlock("enum_string_entry");

    // Return the string name of the enum value
    var resultVar = instructionBuilder.createTempVariable();

    // TODO: Generate switch/lookup based on ordinal to return enum name
    // e.g., ordinal 0 -> "Hearts", ordinal 1 -> "Diamonds", etc.

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration #^ promote operator.
   */
  private List<IRInstr> generateEnumPromoteOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    instructionBuilder.createBasicBlock("enum_promote_entry");

    // Same as $ operator for enumerations
    return generateEnumStringRepresentation(method, enumSymbol);
  }

  /**
   * Generate IR for enumeration $$ JSON representation.
   */
  private List<IRInstr> generateEnumToJsonOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    instructionBuilder.createBasicBlock("enum_toJson_entry");

    // Return JSON string representation of enum name
    var resultVar = instructionBuilder.createTempVariable();

    // TODO: Generate JSON string with enum name
    // e.g., "\"Hearts\"", "\"Diamonds\"", etc.

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration #? hashcode operator.
   */
  private List<IRInstr> generateEnumHashcodeOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    instructionBuilder.createBasicBlock("enum_hashcode_entry");

    // Use ordinal value as hash code
    var resultVar = instructionBuilder.createTempVariable();

    // TODO: Generate return of ordinal value as hashcode

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration #< first operator.
   */
  private List<IRInstr> generateEnumFirstOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    instructionBuilder.createBasicBlock("enum_first_entry");

    // Return the first enum value (ordinal 0)
    var resultVar = instructionBuilder.createTempVariable();

    // TODO: Generate creation of enum with ordinal 0

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration #> last operator.
   */
  private List<IRInstr> generateEnumLastOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    instructionBuilder.createBasicBlock("enum_last_entry");

    // Return the last enum value (highest ordinal)
    var resultVar = instructionBuilder.createTempVariable();

    // TODO: Generate creation of enum with highest ordinal
    // Need to determine the count of enum values

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for string-enum equality (CardSuit.Hearts == "Hearts").
   */
  private List<IRInstr> generateEnumStringEquality(MethodSymbol method, AggregateSymbol enumSymbol) {
    instructionBuilder.createBasicBlock("enum_string_eq_entry");

    // Complex logic: attempt string-to-enum conversion, then compare
    var stringToEnum = instructionBuilder.createTempVariable();
    var resultVar = instructionBuilder.createTempVariable();

    // TODO: Generate string-to-enum conversion logic
    // TODO: Generate comparison with this enum value
    // TODO: Handle conversion failure (return false)

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for string-enum comparison (CardSuit.Hearts <=> "Hearts").
   */
  private List<IRInstr> generateEnumStringComparison(MethodSymbol method, AggregateSymbol enumSymbol) {
    instructionBuilder.createBasicBlock("enum_string_cmp_entry");

    // Convert string to enum, then do ordinal comparison
    var stringToEnum = instructionBuilder.createTempVariable();
    var resultVar = instructionBuilder.createTempVariable();

    // TODO: Generate string-to-enum conversion logic
    // TODO: Generate ordinal comparison if conversion succeeds
    // TODO: Handle conversion failure (return appropriate comparison result)

    return instructionBuilder.extractInstructions();
  }
}