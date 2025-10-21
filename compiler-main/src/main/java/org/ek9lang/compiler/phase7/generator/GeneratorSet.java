package org.ek9lang.compiler.phase7.generator;

import org.ek9lang.compiler.phase7.calls.CallDetailsBuilder;
import org.ek9lang.compiler.phase7.calls.ParameterPromotionProcessor;
import org.ek9lang.compiler.phase7.generation.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.RecordExprProcessing;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;

/**
 * Struct-style data holder for all Phase 7 IR generators and helpers.
 * <p>
 * <b>Architecture Pattern:</b> Mutable struct with public fields (C-style).
 * Fields are mutable during construction phase, then logically immutable after.
 * This pattern enables natural resolution of circular dependencies without
 * complex forward method references or Supplier indirection.
 * </p>
 * <p>
 * <b>Usage:</b> Created and populated by IRConstructGenerators factory, then passed
 * to all generator constructors. Reduces constructor parameter counts from 1-9
 * parameters down to 2 uniform parameters: (IRGenerationContext, GeneratorSet).
 * </p>
 * <p>
 * <b>Thread Safety:</b> Each construct gets its own GeneratorSet instance via unique
 * IRGenerationContext, ensuring thread isolation during parallel construct generation.
 * </p>
 * <p>
 * <b>Extension Strategy:</b> Adding new generators requires only:
 * 1. Add public field here
 * 2. Populate in IRConstructGenerators.create()
 * 3. Use via generators.fieldName in dependent code
 * </p>
 */
public final class GeneratorSet {

  // ========== Shared Lightweight Helpers (15 fields) ==========
  // Created ONCE per construct, reused by all generators

  /**
   * Handles variable memory management (RETAIN, SCOPE_REGISTER, etc.).
   */
  public VariableMemoryManagement variableMemoryManagement;

  /**
   * Processes parameter promotions for method calls.
   */
  public ParameterPromotionProcessor parameterPromotionProcessor;

  /**
   * Builds CallDetails with cost-based method resolution and promotion.
   */
  public CallDetailsBuilder callDetailsBuilder;

  /**
   * Creates debug info from source tokens when instrumentation enabled.
   */
  public DebugInfoCreator debugInfoCreator;

  /**
   * Processes record expression construction (field extraction for short-circuit operators).
   */
  public RecordExprProcessing recordExprProcessing;

  // ========== Universal IR Generation Helpers (Phase 1) ==========

  /**
   * Executes instructions within a scoped block (SCOPE_ENTER/EXIT).
   */
  public org.ek9lang.compiler.phase7.support.ScopedInstructionExecutor scopedInstructionExecutor;

  /**
   * Invokes unary operators with memory management.
   */
  public org.ek9lang.compiler.phase7.support.UnaryOperatorInvoker unaryOperatorInvoker;

  /**
   * Invokes binary operators with memory management.
   */
  public org.ek9lang.compiler.phase7.support.BinaryOperatorInvoker binaryOperatorInvoker;

  /**
   * Loads literal values with memory management.
   */
  public org.ek9lang.compiler.phase7.support.ManagedLiteralLoader managedLiteralLoader;

  /**
   * Extracts primitive boolean from EK9 Boolean objects.
   */
  public org.ek9lang.compiler.phase7.support.PrimitiveBooleanExtractor primitiveBooleanExtractor;

  // ========== Composite IR Generation Helpers (Phase 2) ==========

  /**
   * Evaluates comparison operations and extracts primitive boolean.
   */
  public org.ek9lang.compiler.phase7.support.ComparisonEvaluator comparisonEvaluator;

  /**
   * Evaluates chained comparisons for polymorphic for-range loops.
   */
  public org.ek9lang.compiler.phase7.support.ChainedComparisonEvaluator chainedComparisonEvaluator;

  /**
   * Evaluates increment/decrement operations and updates loop counter.
   */
  public org.ek9lang.compiler.phase7.support.IncrementEvaluator incrementEvaluator;

  // ========== Domain-Specific IR Generation Helpers (Phase 3) ==========

  /**
   * Builds direction check IR for polymorphic for-range loops.
   */
  public org.ek9lang.compiler.phase7.support.DirectionCheckBuilder directionCheckBuilder;

  // ========== Expression Generators (9 fields) ==========
  // Created ONCE, reused for all expressions in construct

  /**
   * Main expression processor - handles all expression types recursively.
   */
  public ExprInstrGenerator exprGenerator;

  /**
   * Processes object access expressions (method calls, property access).
   */
  public ObjectAccessInstrGenerator objectAccessGenerator;

  /**
   * Handles primary references (THIS and SUPER keywords).
   */
  public PrimaryReferenceGenerator primaryReferenceGenerator;

  /**
   * Generates IR for unary operations (negation, increment, etc.).
   */
  public UnaryOperationGenerator unaryOperationGenerator;

  /**
   * Generates IR for binary operations (addition, comparison, etc.).
   */
  public BinaryOperationGenerator binaryOperationGenerator;

  /**
   * Generates short-circuit AND expressions.
   */
  public ShortCircuitAndGenerator shortCircuitAndGenerator;

  /**
   * Generates short-circuit OR expressions.
   */
  public ShortCircuitOrGenerator shortCircuitOrGenerator;

  /**
   * Generates question block (? operator) expressions.
   */
  public QuestionBlockGenerator questionBlockGenerator;

  /**
   * Generates control flow chains (ternary, guards, etc.).
   */
  public ControlFlowChainGenerator controlFlowChainGenerator;

  // ========== Statement Generators (4 fields) ==========
  // Created ONCE, reused for all statements in construct

  /**
   * Main statement processor - delegates to specialized statement generators.
   */
  public StmtInstrGenerator stmtGenerator;

  /**
   * Generates IR for assignment statements with guard support.
   */
  public AssignmentStmtGenerator assignmentStmtGenerator;

  /**
   * Generates IR for assert statements.
   */
  public AssertStmtGenerator assertStmtGenerator;

  /**
   * Generates IR for call statements (function/method calls).
   */
  public CallInstrGenerator callGenerator;

  // ========== Control Flow Generators (3 fields - more to come) ==========
  // Created ONCE, reused for all control flow statements in construct

  /**
   * Generates IR for if/else statements using CONTROL_FLOW_CHAIN.
   */
  public IfStatementGenerator ifStatementGenerator;

  /**
   * Generates IR for while/do-while loops using CONTROL_FLOW_CHAIN.
   */
  public WhileStatementGenerator whileStatementGenerator;

  /**
   * Generates IR for for-range loops using CONTROL_FLOW_CHAIN.
   */
  public ForStatementGenerator forStatementGenerator;

  // ========== Block Generators (3 fields) ==========
  // Created ONCE, reused for all blocks in construct

  /**
   * Main block statement processor - handles statement blocks.
   */
  public BlockStmtInstrGenerator blockStmtGenerator;

  /**
   * Generates IR for variable declarations with initialization.
   */
  public VariableDeclInstrGenerator variableDeclGenerator;

  /**
   * Generates IR for variable declarations without initialization.
   */
  public VariableOnlyDeclInstrGenerator variableOnlyDeclGenerator;

  // ========== Constructor/Function Processors (2 fields) ==========
  // Created ONCE, handle specialized call patterns

  /**
   * Processes constructor calls with memory management.
   */
  public ConstructorCallProcessor constructorCallProcessor;

  /**
   * Processes function calls with promotion support.
   */
  public FunctionCallProcessor functionCallProcessor;

  /**
   * Package-private constructor - only IRConstructGenerators factory can create instances.
   * Fields are populated externally during construction phase.
   */
  GeneratorSet() {
    // Intentionally empty - fields populated by factory
  }
}
