package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.IRFrameType;
import org.ek9lang.compiler.phase7.support.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRInstructionBuilder;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Handles IR generation for synthetic methods (default operators).
 * 
 * <p>This helper generates IR for all 11 default operators that EK9 supports
 * using a map-based dispatch pattern. It leverages existing markers from
 * earlier phases (isSynthetic() and DEFAULTED squirrel data) to identify
 * which operators need synthesis.</p>
 * 
 * <p>Uses the stack-based IRGenerationContext to eliminate parameter threading
 * and provide consistent scope/debug/memory management.</p>
 */
public class SyntheticMethodIRHelper extends AbstractIRHelper {
  
  // Map-based dispatch for all 11 default operators
  private final Map<String, BiFunction<MethodSymbol, IAggregateSymbol, List<IRInstr>>> defaultOperators;

  /**
   * Create the synthetic method IR helper.
   */
  public SyntheticMethodIRHelper(IRGenerationContext context, IRInstructionBuilder instructionBuilder) {
    super(context, instructionBuilder);
    this.defaultOperators = initializeDefaultOperators();
  }

  /**
   * Generate IR for all synthetic methods in the given symbol.
   */
  public void generateFor(ISymbol symbol) {
    if (!(symbol instanceof IAggregateSymbol aggregateSymbol)) {
      return;
    }

    // Use existing markers from earlier phases
    for (var method : aggregateSymbol.getAllMethods()) {
      if (method.isSynthetic() && isDefaultOperator(method)) {
        generateSyntheticMethodIR(method, aggregateSymbol);
      }
    }
  }

  /**
   * Generate IR for a specific synthetic method.
   */
  private void generateSyntheticMethodIR(MethodSymbol method, IAggregateSymbol aggregate) {
    // TODO: Get debug info from method symbol once API is understood
    context.enterScope(method.getScopeName(), null, IRFrameType.SYNTHETIC_METHOD);
    
    try {
      var generator = defaultOperators.get(method.getName());
      if (generator != null) {
        var instructions = generator.apply(method, aggregate);
        context.addInstructions(instructions);
      } else {
        // Log warning about unknown synthetic operator
        System.err.println("Unknown synthetic operator: " + method.getName());
      }
    } finally {
      context.exitScope();
    }
  }

  /**
   * Initialize the map of default operators to their IR generators.
   */
  private Map<String, BiFunction<MethodSymbol, IAggregateSymbol, List<IRInstr>>> initializeDefaultOperators() {
    var operators = new java.util.HashMap<String, BiFunction<MethodSymbol, IAggregateSymbol, List<IRInstr>>>();
    
    // Foundation comparison operator
    operators.put("<=>", this::generateComparisonOperator);
    
    // Derived comparison operators (use <=> internally)
    operators.put("==", this::generateEqualityOperator);
    operators.put("<>", this::generateInequalityOperator);
    operators.put("<", this::generateLessThanOperator);
    operators.put("<=", this::generateLessEqualOperator);
    operators.put(">", this::generateGreaterThanOperator);
    operators.put(">=", this::generateGreaterEqualOperator);
    
    // Independent unary operators
    operators.put("?", this::generateIsSetOperator);
    operators.put("$", this::generateStringOperator);
    operators.put("$$", this::generateToJsonOperator);
    operators.put("#?", this::generateHashcodeOperator);
    
    return operators;
  }

  /**
   * Check if method is a default operator using existing markers.
   */
  private boolean isDefaultOperator(MethodSymbol method) {
    // Check for DEFAULTED squirrel data set by OperatorFactory
    return "TRUE".equals(method.getSquirrelledData(CommonValues.DEFAULTED));
  }

  // ===== OPERATOR GENERATORS =====

  /**
   * Generate IR for <=> comparison operator (foundation for all comparisons).
   */
  private List<IRInstr> generateComparisonOperator(MethodSymbol method, IAggregateSymbol aggregate) {
    var instructions = new ArrayList<IRInstr>();
    
    // TODO: Implement comparison logic based on aggregate properties
    // For now, create placeholder basic block
    instructionBuilder.createBasicBlock("cmp_entry");
    
    // Generate comparison logic for each property in order
    for (var property : aggregate.getProperties()) {
      // Call property.<=> and compare results
      // If not equal, return that result
      // If equal, continue to next property
    }
    
    // If all properties equal, return 0
    var resultVar = instructionBuilder.createTempVariable();
    instructionBuilder.createLiteral(resultVar, "0", "Integer");
    
    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for == equality operator (derived from <=>).
   */
  private List<IRInstr> generateEqualityOperator(MethodSymbol method, IAggregateSymbol aggregate) {
    var instructions = new ArrayList<IRInstr>();
    
    instructionBuilder.createBasicBlock("eq_entry");
    
    // Generate: this <=> other == 0
    var cmpResult = instructionBuilder.createTempVariable();
    // TODO: Generate call to <=> operator
    
    var resultVar = instructionBuilder.createTempVariable();
    // TODO: Generate comparison of cmpResult with 0
    
    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for <> inequality operator (derived from <=>).
   */
  private List<IRInstr> generateInequalityOperator(MethodSymbol method, IAggregateSymbol aggregate) {
    var instructions = new ArrayList<IRInstr>();
    
    instructionBuilder.createBasicBlock("neq_entry");
    
    // Generate: this <=> other != 0
    var cmpResult = instructionBuilder.createTempVariable();
    // TODO: Generate call to <=> operator
    
    var resultVar = instructionBuilder.createTempVariable();
    // TODO: Generate comparison of cmpResult with 0 (not equal)
    
    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for < less than operator (derived from <=>).
   */
  private List<IRInstr> generateLessThanOperator(MethodSymbol method, IAggregateSymbol aggregate) {
    return generateComparisonDerived(method, aggregate, "lt", "< 0");
  }

  /**
   * Generate IR for <= less or equal operator (derived from <=>).
   */
  private List<IRInstr> generateLessEqualOperator(MethodSymbol method, IAggregateSymbol aggregate) {
    return generateComparisonDerived(method, aggregate, "lteq", "<= 0");
  }

  /**
   * Generate IR for > greater than operator (derived from <=>).
   */
  private List<IRInstr> generateGreaterThanOperator(MethodSymbol method, IAggregateSymbol aggregate) {
    return generateComparisonDerived(method, aggregate, "gt", "> 0");
  }

  /**
   * Generate IR for >= greater or equal operator (derived from <=>).
   */
  private List<IRInstr> generateGreaterEqualOperator(MethodSymbol method, IAggregateSymbol aggregate) {
    return generateComparisonDerived(method, aggregate, "gteq", ">= 0");
  }

  /**
   * Helper to generate comparison operators derived from <=>.
   */
  private List<IRInstr> generateComparisonDerived(MethodSymbol method, IAggregateSymbol aggregate, 
                                                  String opName, String comparison) {
    var instructions = new ArrayList<IRInstr>();
    
    instructionBuilder.createBasicBlock(opName + "_entry");
    
    // Generate: (this <=> other) comparison 0
    var cmpResult = instructionBuilder.createTempVariable();
    // TODO: Generate call to <=> operator
    
    var resultVar = instructionBuilder.createTempVariable();
    // TODO: Generate comparison with 0 using specified operation
    
    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for ? isSet operator.
   */
  private List<IRInstr> generateIsSetOperator(MethodSymbol method, IAggregateSymbol aggregate) {
    var instructions = new ArrayList<IRInstr>();
    
    instructionBuilder.createBasicBlock("isSet_entry");
    
    // Check all properties are set
    for (var property : aggregate.getProperties()) {
      // Call property.? and check result
      // If any property is not set, return false
    }
    
    // If all properties set, return true
    var resultVar = instructionBuilder.createTempVariable();
    instructionBuilder.createLiteral(resultVar, "true", "Boolean");
    
    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for $ string representation operator.
   */
  private List<IRInstr> generateStringOperator(MethodSymbol method, IAggregateSymbol aggregate) {
    var instructions = new ArrayList<IRInstr>();
    
    instructionBuilder.createBasicBlock("string_entry");
    
    // Build string representation from all properties
    var resultVar = instructionBuilder.createTempVariable();
    
    // TODO: Generate string concatenation of all property._string() calls
    // Format: ClassName(prop1=value1, prop2=value2, ...)
    
    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for $$ JSON representation operator.
   */
  private List<IRInstr> generateToJsonOperator(MethodSymbol method, IAggregateSymbol aggregate) {
    var instructions = new ArrayList<IRInstr>();
    
    instructionBuilder.createBasicBlock("toJson_entry");
    
    // Build JSON representation from all properties
    var resultVar = instructionBuilder.createTempVariable();
    
    // TODO: Generate JSON object with all property values
    // Format: {"prop1": value1, "prop2": value2, ...}
    
    return instructionBuilder.extractInstructions();
  }

  /**
   * Generate IR for #? hashcode operator.
   */
  private List<IRInstr> generateHashcodeOperator(MethodSymbol method, IAggregateSymbol aggregate) {
    var instructions = new ArrayList<IRInstr>();
    
    instructionBuilder.createBasicBlock("hashcode_entry");
    
    // Generate hash based on all properties
    var resultVar = instructionBuilder.createTempVariable();
    
    // TODO: Generate hash combination of all property.#? calls
    // Use standard hash combining algorithm
    
    return instructionBuilder.extractInstructions();
  }
}