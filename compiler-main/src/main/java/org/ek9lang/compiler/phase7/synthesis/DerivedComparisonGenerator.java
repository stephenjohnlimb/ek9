package org.ek9lang.compiler.phase7.synthesis;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Generates synthetic IR for derived comparison operators: {@literal <}, {@literal <=}, {@literal >}, {@literal >=}.
 *
 * <p>These operators delegate to {@literal <=>} (_cmp) and interpret the Integer result:</p>
 * <ul>
 *   <li>{@literal <} - returns true if _cmp result {@literal <} 0</li>
 *   <li>{@literal <=} - returns true if _cmp result {@literal <=} 0</li>
 *   <li>{@literal >} - returns true if _cmp result {@literal >} 0</li>
 *   <li>{@literal >=} - returns true if _cmp result {@literal >=} 0</li>
 * </ul>
 *
 * <p>Generated code pattern:</p>
 * <pre>
 * Boolean operator(T other):
 *   cmpResult = this._cmp(other)
 *   if !cmpResult._isSet() -> return unset
 *   return cmpResult.{_lt|_lte|_gt|_gte}(0)
 * </pre>
 *
 * <p>This approach leverages the existing _cmp implementation which already handles:</p>
 * <ul>
 *   <li>This/other isSet guards</li>
 *   <li>Super class comparison</li>
 *   <li>Field-by-field tri-state semantics</li>
 * </ul>
 */
final class DerivedComparisonGenerator extends AbstractSyntheticGenerator {

  private static final String RETURN_VAR = "rtn";
  private static final String OTHER_PARAM = "param";
  private final OperatorMap operatorMap = new OperatorMap();

  DerivedComparisonGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
  }

  /**
   * Generate IR for a derived comparison operator.
   *
   * @param operatorName    The operator symbol ({@literal <}, {@literal <=}, {@literal >}, {@literal >=})
   * @param operatorSymbol  The operator method symbol
   * @param aggregateSymbol The aggregate containing the operator
   * @return List of IR instructions implementing the operator
   */
  List<IRInstr> generate(final String operatorName,
                         final MethodSymbol operatorSymbol,
                         final AggregateSymbol aggregateSymbol) {

    AssertValue.checkNotNull("operatorName cannot be null", operatorName);
    AssertValue.checkNotNull("operatorSymbol cannot be null", operatorSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = createDebugInfo(operatorSymbol);
    final var scopeId = stackContext.generateScopeId(getInternalName(operatorName));
    final var aggregateTypeName = aggregateSymbol.getFullyQualifiedName();

    // Labels for control flow
    final var returnUnsetLabel = generateLabelName("return_unset");
    final var returnResultLabel = generateLabelName("return_result");

    // Enter scope
    instructions.add(ScopeInstr.enter(scopeId, debugInfo));

    // Reference return variable
    instructions.add(MemoryInstr.reference(RETURN_VAR, getBooleanTypeName(), debugInfo));

    // Call this._cmp(other) to get comparison result
    final var cmpResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        cmpResultVar,
        IRConstants.THIS,
        aggregateTypeName,
        IRConstants.CMP_METHOD,
        OTHER_PARAM,
        aggregateTypeName,
        getIntegerTypeName(),
        debugInfo,
        scopeId
    ));

    // Check if _cmp result is set
    instructions.addAll(generateIsSetGuard(cmpResultVar, getIntegerTypeName(), debugInfo, returnUnsetLabel, scopeId));

    // Create zero literal for comparison
    final var zeroVar = generateTempName();
    instructions.add(LiteralInstr.literal(zeroVar, "0", getIntegerTypeName(), debugInfo));
    instructions.add(MemoryInstr.retain(zeroVar, debugInfo));
    instructions.add(ScopeInstr.register(zeroVar, scopeId, debugInfo));

    // Call appropriate comparison method on the Integer result
    // _lt for <, _lte for <=, _gt for >, _gte for >=
    final var comparisonMethod = getComparisonMethod(operatorName);
    final var boolResultVar = generateTempName();
    instructions.addAll(generateMethodCall(
        boolResultVar,
        cmpResultVar,
        getIntegerTypeName(),
        comparisonMethod,
        zeroVar,
        getIntegerTypeName(),
        getBooleanTypeName(),
        debugInfo,
        scopeId
    ));

    // Store result and branch to return
    instructions.add(MemoryInstr.store(RETURN_VAR, boolResultVar, debugInfo));
    instructions.add(MemoryInstr.retain(RETURN_VAR, debugInfo));
    instructions.add(BranchInstr.branch(returnResultLabel, debugInfo));

    // Return blocks - use inherited methods from AbstractSyntheticGenerator
    instructions.addAll(generateUnsetReturnBlockWithLabel(returnUnsetLabel, getBooleanTypeName(),
        RETURN_VAR, debugInfo, scopeId));
    instructions.addAll(generateResultReturnBlock(returnResultLabel, RETURN_VAR, debugInfo, scopeId));

    return instructions;
  }

  /**
   * Get the internal method name for the operator.
   */
  private String getInternalName(final String operatorName) {
    return switch (operatorName) {
      case "<" -> "_lt";
      case "<=" -> "_lte";
      case ">" -> "_gt";
      case ">=" -> "_gte";
      default -> throw new IllegalArgumentException("Unknown comparison operator: " + operatorName);
    };
  }

  /**
   * Get the Integer comparison method to call based on operator.
   * Uses OperatorMap to ensure correct method names (e.g., {@literal <=} -> _lteq, not _lte).
   */
  private String getComparisonMethod(final String operatorName) {
    return operatorMap.getForward(operatorName);
  }

  // NOTE: generateUnsetReturnBlockWithLabel and generateResultReturnBlock
  // are inherited from AbstractSyntheticGenerator
}
