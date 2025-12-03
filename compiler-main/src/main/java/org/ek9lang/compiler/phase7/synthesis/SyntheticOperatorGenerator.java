package org.ek9lang.compiler.phase7.synthesis;

import java.util.List;
import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Main coordinator for synthetic operator generation.
 *
 * <p>This class dispatches synthetic operator/method generation to the appropriate
 * specialized generator based on the operator type. All synthetic generation happens
 * at IR level, producing complete instruction sequences that backends can translate
 * directly.</p>
 *
 * <h2>Supported Operators</h2>
 * <p>For records and classes:</p>
 * <ul>
 *   <li>_eq - Field-by-field equality comparison</li>
 *   <li>_neq - Delegates to _eq and negates</li>
 *   <li>_hashcode - Combined hash of all fields</li>
 *   <li>_string - String representation of all fields</li>
 *   <li>_copy - Copy all fields from source</li>
 * </ul>
 *
 * <p>For enumerations:</p>
 * <ul>
 *   <li>_lt, _lte, _gt, _gte - Ordinal comparisons</li>
 *   <li>_cmp - Three-way comparison</li>
 *   <li>_inc, _dec - Navigate enum values</li>
 * </ul>
 *
 * @see AbstractSyntheticGenerator
 */
public final class SyntheticOperatorGenerator {

  private final IRGenerationContext stackContext;
  private final EqualsGenerator equalsGenerator;
  private final NotEqualsGenerator notEqualsGenerator;
  private final CompareGenerator compareGenerator;
  private final DerivedComparisonGenerator derivedComparisonGenerator;
  private final IsSetGenerator isSetGenerator;
  private final FieldSetStatusGenerator fieldSetStatusGenerator;

  /**
   * Create a new synthetic operator generator.
   *
   * @param stackContext The IR generation context for accessing state
   */
  public SyntheticOperatorGenerator(final IRGenerationContext stackContext) {
    AssertValue.checkNotNull("stackContext cannot be null", stackContext);
    this.stackContext = stackContext;
    this.equalsGenerator = new EqualsGenerator(stackContext);
    this.notEqualsGenerator = new NotEqualsGenerator(stackContext);
    this.compareGenerator = new CompareGenerator(stackContext);
    this.derivedComparisonGenerator = new DerivedComparisonGenerator(stackContext);
    this.isSetGenerator = new IsSetGenerator(stackContext);
    this.fieldSetStatusGenerator = new FieldSetStatusGenerator(stackContext);
  }

  /**
   * Generate IR for a synthetic operator.
   *
   * <p>Dispatches to the appropriate specialized generator based on the operator name.
   * Returns a complete {@link OperationInstr} with fully populated IR instructions.</p>
   *
   * @param operatorSymbol   The synthetic operator method symbol
   * @param aggregateSymbol  The aggregate (class/record) containing the operator
   * @return The populated OperationInstr with generated IR body
   */
  public OperationInstr generateOperator(final MethodSymbol operatorSymbol,
                                         final AggregateSymbol aggregateSymbol) {
    AssertValue.checkNotNull("operatorSymbol cannot be null", operatorSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);
    AssertValue.checkTrue("Method must be synthetic", operatorSymbol.isSynthetic());
    AssertValue.checkTrue("Method must be an operator", operatorSymbol.isOperator());

    final var debugInfo = stackContext.createDebugInfo(operatorSymbol.getSourceToken());
    final var operation = new OperationInstr(operatorSymbol, debugInfo);

    // Generate the appropriate operator based on name
    final var operatorName = operatorSymbol.getName();
    final var instructions = dispatchToGenerator(operatorName, operatorSymbol, aggregateSymbol);

    // Package instructions into basic block
    operation.setBody(new BasicBlockInstr(stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL))
        .addInstructions(instructions));

    return operation;
  }

  /**
   * Generate IR for a synthetic regular method (non-operator).
   *
   * <p>Handles methods like _isSet that are auto-generated based on aggregate properties.</p>
   *
   * @param methodSymbol     The synthetic method symbol
   * @param aggregateSymbol  The aggregate containing the method
   * @return The populated OperationInstr with generated IR body
   */
  public OperationInstr generateMethod(final MethodSymbol methodSymbol,
                                       final AggregateSymbol aggregateSymbol) {
    AssertValue.checkNotNull("methodSymbol cannot be null", methodSymbol);
    AssertValue.checkNotNull("aggregateSymbol cannot be null", aggregateSymbol);
    AssertValue.checkTrue("Method must be synthetic", methodSymbol.isSynthetic());

    final var debugInfo = stackContext.createDebugInfo(methodSymbol.getSourceToken());
    final var operation = new OperationInstr(methodSymbol, debugInfo);

    // Generate based on method name
    final var methodName = methodSymbol.getName();
    final var instructions = dispatchToMethodGenerator(methodName, methodSymbol, aggregateSymbol);

    // Package instructions into basic block
    operation.setBody(new BasicBlockInstr(stackContext.generateBlockLabel(IRConstants.ENTRY_LABEL))
        .addInstructions(instructions));

    return operation;
  }

  /**
   * Dispatch to the appropriate operator generator based on operator name.
   */
  private List<IRInstr> dispatchToGenerator(final String operatorName,
                                            final MethodSymbol operatorSymbol,
                                            final AggregateSymbol aggregateSymbol) {
    // Use instruction builder for consistent IR generation
    final var instructionBuilder = new IRInstructionBuilder(stackContext);

    // Operator names match source symbols (==, <>, etc.) not internal names (_eq, _neq, etc.)
    return switch (operatorName) {
      case "==" -> generateEqualsOperator(instructionBuilder, operatorSymbol, aggregateSymbol);
      case "<>" -> generateNotEqualsOperator(instructionBuilder, operatorSymbol, aggregateSymbol);
      case "#?" -> generateHashCodeOperator(instructionBuilder, operatorSymbol, aggregateSymbol);
      case "$" -> generateToStringOperator(instructionBuilder, operatorSymbol, aggregateSymbol);
      case ":=:" -> generateCopyOperator(instructionBuilder, operatorSymbol, aggregateSymbol);
      case "<=>" -> generateCompareOperator(instructionBuilder, operatorSymbol, aggregateSymbol);
      case "?" -> generateIsSetOperator(instructionBuilder, operatorSymbol, aggregateSymbol);
      case "<", "<=", ">", ">=" ->
          generateOrdinalComparisonOperator(instructionBuilder, operatorName, operatorSymbol, aggregateSymbol);
      case "++", "--" ->
          generateNavigationOperator(instructionBuilder, operatorName, operatorSymbol, aggregateSymbol);
      default -> generatePlaceholder(instructionBuilder, operatorName);
    };
  }

  /**
   * Dispatch to the appropriate method generator based on method name.
   */
  private List<IRInstr> dispatchToMethodGenerator(final String methodName,
                                                  final MethodSymbol methodSymbol,
                                                  final AggregateSymbol aggregateSymbol) {
    final var instructionBuilder = new IRInstructionBuilder(stackContext);

    return switch (methodName) {
      case "_isSet" -> generateIsSetMethod(instructionBuilder, methodSymbol, aggregateSymbol);
      case "_fieldSetStatus" -> generateFieldSetStatusMethod(instructionBuilder, methodSymbol, aggregateSymbol);
      default -> generatePlaceholder(instructionBuilder, methodName);
    };
  }

  // ========== Individual Operator Generators ==========
  // These will be moved to separate classes as they grow in complexity

  /**
   * Generate _eq operator - field-by-field equality comparison.
   *
   * <p>Delegates to {@link EqualsGenerator} which implements:</p>
   * <ul>
   *   <li>IsSet guards for this and parameter</li>
   *   <li>Super._eq call if super is not Any</li>
   *   <li>Field-by-field comparison with short-circuit evaluation</li>
   *   <li>Proper tri-state return handling (set true, set false, unset)</li>
   * </ul>
   */
  private List<IRInstr> generateEqualsOperator(final IRInstructionBuilder builder,
                                               final MethodSymbol operatorSymbol,
                                               final AggregateSymbol aggregateSymbol) {
    return equalsGenerator.generate(operatorSymbol, aggregateSymbol);
  }

  /**
   * Generate _neq ({@literal <>}) operator - delegates to _eq and negates.
   *
   * <p>Delegates to {@link NotEqualsGenerator} which implements:</p>
   * <ul>
   *   <li>Call this._eq(param) to get equality result</li>
   *   <li>If result is unset, return unset</li>
   *   <li>If result is true, return false (not equal)</li>
   *   <li>If result is false, return true (not equal)</li>
   * </ul>
   */
  private List<IRInstr> generateNotEqualsOperator(final IRInstructionBuilder builder,
                                                  final MethodSymbol operatorSymbol,
                                                  final AggregateSymbol aggregateSymbol) {
    return notEqualsGenerator.generate(operatorSymbol, aggregateSymbol);
  }

  /**
   * Generate _hashcode operator - combine hashes of all fields.
   * TODO: Implement hash combination pattern
   */
  private List<IRInstr> generateHashCodeOperator(final IRInstructionBuilder builder,
                                                 final MethodSymbol operatorSymbol,
                                                 final AggregateSymbol aggregateSymbol) {
    // TODO: Phase 4 implementation
    return generatePlaceholder(builder, "_hashcode");
  }

  /**
   * Generate _string operator - string representation of all fields.
   * TODO: Implement string building pattern
   */
  private List<IRInstr> generateToStringOperator(final IRInstructionBuilder builder,
                                                 final MethodSymbol operatorSymbol,
                                                 final AggregateSymbol aggregateSymbol) {
    // TODO: Phase 5 implementation
    return generatePlaceholder(builder, "_string");
  }

  /**
   * Generate _copy operator - copy all fields from source.
   * TODO: Implement field copy pattern
   */
  private List<IRInstr> generateCopyOperator(final IRInstructionBuilder builder,
                                             final MethodSymbol operatorSymbol,
                                             final AggregateSymbol aggregateSymbol) {
    // TODO: Phase 6 implementation
    return generatePlaceholder(builder, "_copy");
  }

  /**
   * Generate _cmp ({@literal <=>}) operator - three-way comparison.
   *
   * <p>Delegates to {@link CompareGenerator} which implements:</p>
   * <ul>
   *   <li>IsSet guards for this and parameter</li>
   *   <li>Super._cmp call if super is not Any</li>
   *   <li>Field-by-field comparison in declaration order</li>
   *   <li>Short-circuit evaluation on non-zero results</li>
   *   <li>Proper tri-state return handling (set integer, unset)</li>
   * </ul>
   */
  private List<IRInstr> generateCompareOperator(final IRInstructionBuilder builder,
                                                final MethodSymbol operatorSymbol,
                                                final AggregateSymbol aggregateSymbol) {
    return compareGenerator.generate(operatorSymbol, aggregateSymbol);
  }

  /**
   * Generate _isSet (?) operator - check if all fields are set.
   *
   * <p>Delegates to {@link IsSetGenerator} which implements:</p>
   * <ul>
   *   <li>Field-by-field isSet checking</li>
   *   <li>Short-circuit evaluation (return false on first unset field)</li>
   *   <li>Returns true only if ALL fields are set</li>
   * </ul>
   */
  private List<IRInstr> generateIsSetOperator(final IRInstructionBuilder builder,
                                              final MethodSymbol operatorSymbol,
                                              final AggregateSymbol aggregateSymbol) {
    return isSetGenerator.generate(operatorSymbol, aggregateSymbol);
  }

  /**
   * Generate derived comparison operators ({@literal <}, {@literal <=}, {@literal >}, {@literal >=}).
   *
   * <p>Delegates to {@link DerivedComparisonGenerator} which implements:</p>
   * <ul>
   *   <li>Call this._cmp(other) to get Integer comparison result</li>
   *   <li>Check if result is unset, return unset if so</li>
   *   <li>Compare result to 0 using appropriate Integer method</li>
   *   <li>Return the Boolean comparison result</li>
   * </ul>
   *
   * <p>This approach leverages the existing _cmp implementation for all tri-state semantics.</p>
   */
  private List<IRInstr> generateOrdinalComparisonOperator(final IRInstructionBuilder builder,
                                                          final String operatorName,
                                                          final MethodSymbol operatorSymbol,
                                                          final AggregateSymbol aggregateSymbol) {
    return derivedComparisonGenerator.generate(operatorName, operatorSymbol, aggregateSymbol);
  }

  /**
   * Generate navigation operators (_inc, _dec) for enumerations.
   * TODO: Implement array-based navigation
   */
  private List<IRInstr> generateNavigationOperator(final IRInstructionBuilder builder,
                                                   final String operatorName,
                                                   final MethodSymbol operatorSymbol,
                                                   final AggregateSymbol aggregateSymbol) {
    // TODO: Phase 9 implementation
    return generatePlaceholder(builder, operatorName);
  }

  /**
   * Generate _isSet method - check if all required fields are set.
   *
   * <p>Delegates to {@link IsSetGenerator} which implements:</p>
   * <ul>
   *   <li>Field-by-field isSet checking</li>
   *   <li>Short-circuit evaluation (return false on first unset field)</li>
   *   <li>Returns true only if ALL fields are set</li>
   * </ul>
   */
  private List<IRInstr> generateIsSetMethod(final IRInstructionBuilder builder,
                                            final MethodSymbol methodSymbol,
                                            final AggregateSymbol aggregateSymbol) {
    return isSetGenerator.generate(methodSymbol, aggregateSymbol);
  }

  /**
   * Generate _fieldSetStatus method - returns bitmask of field set statuses.
   *
   * <p>Delegates to {@link FieldSetStatusGenerator} which implements:</p>
   * <ul>
   *   <li>For each field, call _isSet() and set corresponding bit</li>
   *   <li>Returns Integer bitmask (bit N = field N status)</li>
   *   <li>Enables efficient field-by-field comparison in operators</li>
   * </ul>
   *
   * <p>This method is called by comparison operators to pre-compute
   * field status before performing comparisons, enabling the IR optimizer
   * to cache and reuse results within expressions.</p>
   */
  private List<IRInstr> generateFieldSetStatusMethod(final IRInstructionBuilder builder,
                                                      final MethodSymbol methodSymbol,
                                                      final AggregateSymbol aggregateSymbol) {
    return fieldSetStatusGenerator.generate(methodSymbol, aggregateSymbol);
  }

  /**
   * Generate placeholder that returns void.
   * Used for operators not yet implemented.
   */
  private List<IRInstr> generatePlaceholder(final IRInstructionBuilder builder,
                                            final String operatorName) {
    // For now, just return void - will be replaced with actual implementation
    builder.returnVoid();
    return builder.extractInstructions();
  }
}
