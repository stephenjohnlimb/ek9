package org.ek9lang.compiler.ir;

/**
 * Enumeration of all IR opcodes used in the EK9 intermediate representation.
 * <p>
 * Key Design Principle: All EK9 operators become method calls (CALL instructions).
 * No primitive arithmetic operations (ADD, SUB, etc.) are included - everything
 * goes through EK9's object method dispatch system.
 * </p>
 * <p>
 * This design ensures target-agnostic IR that works equally well for:
 * - JVM bytecode generation
 * - LLVM-C++ target
 * </p>
 */
public enum IROpcode {

  // Memory operations
  /**
   * Load value from memory location into temporary/register.
   * Format: LOAD dest = source_location
   */
  LOAD,

  /**
   * Store value from temporary/register into memory location.
   * Format: STORE dest_location = source
   */
  STORE,

  /**
   * Load literal/constant value into temporary/register.
   * Format: LOAD_LITERAL dest = literal_value, literal_type
   * Handles: String literals, numeric literals, boolean literals, etc.
   */
  LOAD_LITERAL,

  /**
   * Declare variable reference (no memory allocation).
   * Format: REFERENCE variable_name, type_info
   * Used for parameters and variable-only declarations.
   * Backend handles appropriate reference storage.
   */
  REFERENCE,

  // Method call operations (handles ALL EK9 operators and method calls)
  /**
   * Standard method call with resolved signature.
   * Format: CALL result = object.method(args...)
   * Handles: EK9 operators (a + b → a._add(b)), normal method calls
   */
  CALL,

  /**
   * Virtual method call (polymorphic dispatch).
   * Format: CALL_VIRTUAL result = object.method(args...)
   */
  CALL_VIRTUAL,

  /**
   * Static method/function call.
   * Format: CALL_STATIC result = Type.method(args...)
   */
  CALL_STATIC,

  /**
   * Get singleton instance of a function.
   * Format: FUNCTION_INSTANCE result = FunctionType
   * Returns the singleton instance of the specified function type.
   * No memory management needed - singletons are never released.
   */
  FUNCTION_INSTANCE,

  /**
   * Dispatcher method call (runtime type-based dispatch).
   * Format: CALL_DISPATCHER result = object.dispatcherMethod(args...)
   * Uses runtime type information and dispatch matrix
   */
  CALL_DISPATCHER,

  // Control flow operations
  /**
   * Unconditional branch/jump.
   * Format: BRANCH target_label
   */
  BRANCH,

  /**
   * Branch if condition is true.
   * Format: BRANCH_TRUE condition, target_label
   */
  BRANCH_TRUE,

  /**
   * Branch if condition is false.
   * Format: BRANCH_FALSE condition, target_label
   */
  BRANCH_FALSE,

  /**
   * Return from method/function.
   * Format: RETURN [value]
   */
  RETURN,

  /**
   * Assert that condition is true.
   * Format: ASSERT condition
   * Traps/throws if condition is false.
   */
  ASSERT,

  /**
   * Unconditional assertion failure.
   * Format: ASSERT_FAILURE
   * Always traps/throws - used when failure is known at compile time.
   * More efficient than creating Boolean objects just to fail assertions.
   */
  ASSERT_FAILURE,

  // Exception handling operations
  /**
   * Throw exception.
   * Format: THROW exception_object
   */
  THROW,

  /**
   * Setup exception handler for try block.
   * Format: SETUP_HANDLER handler_label, exception_types...
   */
  SETUP_HANDLER,

  /**
   * Cleanup after exception handling.
   * Format: CLEANUP
   */
  CLEANUP,

  /**
   * Increment object reference count.
   * Format: RETAIN object
   * JVM: no-op, LLVM: increment ref count
   */
  RETAIN,

  /**
   * Decrement object reference count.
   * Format: RELEASE object
   * JVM: no-op, LLVM: decrement ref count, possible deallocation
   */
  RELEASE,

  // Scope management for exception-safe memory handling
  /**
   * Enter new memory management scope.
   * Format: SCOPE_ENTER scope_id
   * Creates cleanup boundary for exception safety
   */
  SCOPE_ENTER,

  /**
   * Exit memory management scope.
   * Format: SCOPE_EXIT scope_id
   * Automatically RELEASE all registered objects in scope
   */
  SCOPE_EXIT,

  /**
   * Register object for automatic cleanup in scope.
   * Format: SCOPE_REGISTER object, scope_id
   * Object will be RELEASE'd when scope exits
   */
  SCOPE_REGISTER,

  // Comparison operations for conditional logic
  /**
   * Check if reference/object is null or uninitialized.
   * Format: IS_NULL result = operand
   */
  IS_NULL,

  // Control flow label operations
  /**
   * Mark instruction position as branch target.
   * Format: LABEL label_name
   * Used by BRANCH, BRANCH_TRUE, BRANCH_FALSE instructions for control flow
   */
  LABEL,

  // SSA operations for LLVM compatibility
  /**
   * PHI node for merging values from different control flow paths.
   * Format: PHI result = [value1, block1], [value2, block2], ...
   * Essential for SSA form and LLVM IR generation.
   */
  PHI,

  // Higher-level control flow constructs
  /**
   * High-level conditional block construct.
   * Format: CONDITIONAL_BLOCK condition_var, result_var, true_block, false_block
   * Backends lower this to appropriate target-specific control flow.
   */
  CONDITIONAL_BLOCK,

  /**
   * Logical AND block construct with short-circuit capability.
   * Format: LOGICAL_AND_BLOCK result = left_operand, condition, right_evaluation, logical_result
   * Contains complete evaluation paths for both short-circuit and full evaluation.
   */
  LOGICAL_AND_BLOCK,

  /**
   * Logical OR block construct with short-circuit capability.
   * Format: LOGICAL_OR_BLOCK result = left_operand, condition, right_evaluation, logical_result
   * Contains complete evaluation paths for both short-circuit and full evaluation.
   */
  LOGICAL_OR_BLOCK,

  /**
   * Question operator block construct for null-safe _isSet() calls.
   * Format: QUESTION_BLOCK result = operand_evaluation, null_case_evaluation, set_case_evaluation
   * Contains complete evaluation paths for both null and non-null cases.
   * Avoids explicit branching by pre-computing both execution paths.
   */
  QUESTION_BLOCK,

  /**
   * Guarded assignment block construct for conditional assignment (:=? operator).
   * Format: GUARDED_ASSIGNMENT_BLOCK result = condition_evaluation, assignment_evaluation
   * Contains complete evaluation paths for both condition checking and assignment.
   * Assigns only if LHS is null OR LHS._isSet() == false.
   * Uses QUESTION_BLOCK logic internally for consistent null-safety semantics.
   */
  GUARDED_ASSIGNMENT_BLOCK,

  /**
   * Unified control flow block construct for all EK9 conditional evaluation.
   * Format: CONTROL_FLOW_CHAIN result = evaluation_variable, condition_chain, default_evaluation
   * <br>
   * Unifies all EK9 control flow constructs into a single, powerful IR opcode:
   * - Question operator (?): NULL_CHECK condition + _isSet() default
   * - If/else statements: Sequential condition evaluation + else default
   * - Switch statements: Case condition evaluation + default clause
   * - Guarded assignment (:=?): Composition with question operator logic
   * <br>
   * Contains complete evaluation paths for all conditions and their corresponding bodies.
   * Backends can optimize based on chain_type hints:
   * - QUESTION_OPERATOR: Optimize null checks and method calls
   * - IF_ELSE/IF_ELSE_IF: Standard conditional branching
   * - SWITCH/SWITCH_ENUM: Jump tables for dense enums, sequential for complex expressions
   * <br>
   * Key benefits:
   * - Single source of truth for all control flow logic
   * - Consistent memory management across all constructs
   * - Enhanced backend optimization opportunities
   * - Reduced IR complexity and maintenance burden
   */
  CONTROL_FLOW_CHAIN,

  // Stack allocation and memory optimization opcodes
  /**
   * Allocate object on stack instead of heap.
   * Format: STACK_ALLOC result = type_info
   * Used when escape analysis determines object never leaves function scope.
   * Backend: LLVM uses alloca, JVM uses optimized local variables.
   * Eliminates need for RETAIN/RELEASE/SCOPE_REGISTER for this object.
   */
  STACK_ALLOC,

  /**
   * Create literal value on stack instead of heap.
   * Format: STACK_ALLOC_LITERAL result = literal_value, literal_type
   * Optimization for non-escaping literals (strings, numbers, etc.).
   * More efficient than heap allocation - automatic cleanup on function exit.
   * Replaces LOAD_LITERAL during IR optimization phase.
   */
  STACK_ALLOC_LITERAL,

  /**
   * Release heap references held by stack objects before destruction.
   * Format: STACK_CLEANUP_REFS stack_object
   * Handles mixed heap/stack reference scenarios where stack objects
   * hold references to heap-allocated objects. Ensures proper reference
   * counting before stack object is automatically destroyed.
   */
  STACK_CLEANUP_REFS,

  /**
   * Transfer ownership without reference counting overhead.
   * Format: TRANSFER_OWNERSHIP dest = source
   * Optimization for clear ownership transfers - avoids retain/release pair.
   * Used when analysis shows object ownership cleanly passes from source to dest
   * with no intermediate sharing or aliasing.
   */
  TRANSFER_OWNERSHIP,

  // Memory management optimization markers
  /**
   * Annotation marker: skip RETAIN operation for this object.
   * Format: NO_RETAIN object
   * Used during IR optimization to mark objects that don't need reference counting.
   * Does not generate code - just prevents RETAIN instruction emission.
   */
  NO_RETAIN,

  /**
   * Annotation marker: skip RELEASE operation for this object.
   * Format: NO_RELEASE object
   * Used during IR optimization to mark objects that don't need reference counting.
   * Does not generate code - just prevents RELEASE instruction emission.
   */
  NO_RELEASE,

  /**
   * Annotation marker: skip SCOPE_REGISTER operation for this object.
   * Format: NO_SCOPE_REGISTER object, scope
   * Used during IR optimization for objects with automatic cleanup (e.g., stack allocated).
   * Does not generate code - just prevents SCOPE_REGISTER instruction emission.
   */
  NO_SCOPE_REGISTER
}