package org.ek9lang.compiler.phase7.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
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
  public EnumerationIRHelper(IRInstructionBuilder instructionBuilder) {
    super(instructionBuilder);
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
    if (aggregateSymbol.getGenus() != SymbolGenus.CLASS_ENUMERATION) {
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
    // will generate
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

    return method.getName();
  }


  /**
   * Check if method is an enumeration operator.
   */
  private boolean isEnumerationOperator(MethodSymbol method) {
    // Check if method was added by OperatorFactory.addEnumerationMethods
    return method.isSynthetic() && enumerationOperators.containsKey(getOperatorKey(method));
  }


  // ===== ENUMERATION OPERATOR GENERATORS =====

  /**
   * Generate IR for enumeration <=> comparison (ordinal-based).
   */
  private List<IRInstr> generateEnumOrdinalComparison(MethodSymbol method, AggregateSymbol enumSymbol) {
    return List.of();
  }

  /**
   * Generate IR for enumeration == equality.
   */
  private List<IRInstr> generateEnumEquality(MethodSymbol method, AggregateSymbol enumSymbol) {
    //will generate
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
    //will generate

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration ? isSet operator.
   */
  private List<IRInstr> generateEnumIsSetOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    //will generate
    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration $ string representation.
   */
  private List<IRInstr> generateEnumStringRepresentation(MethodSymbol method, AggregateSymbol enumSymbol) {
    //will generate

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration #^ promote operator.
   */
  private List<IRInstr> generateEnumPromoteOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    //will generate
    return generateEnumStringRepresentation(method, enumSymbol);
  }

  /**
   * Generate IR for enumeration $$ JSON representation.
   */
  private List<IRInstr> generateEnumToJsonOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    //will generate

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration #? hashcode operator.
   */
  private List<IRInstr> generateEnumHashcodeOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    //will generate

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration #< first operator.
   */
  private List<IRInstr> generateEnumFirstOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    //will generate
    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for enumeration #> last operator.
   */
  private List<IRInstr> generateEnumLastOperator(MethodSymbol method, AggregateSymbol enumSymbol) {
    //will generate

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for string-enum equality (CardSuit.Hearts == "Hearts").
   */
  private List<IRInstr> generateEnumStringEquality(MethodSymbol method, AggregateSymbol enumSymbol) {
    //will generate

    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for string-enum comparison (CardSuit.Hearts <=> "Hearts").
   */
  private List<IRInstr> generateEnumStringComparison(MethodSymbol method, AggregateSymbol enumSymbol) {
    //will generate

    return instructionBuilder.extractInstructions();
  }
}