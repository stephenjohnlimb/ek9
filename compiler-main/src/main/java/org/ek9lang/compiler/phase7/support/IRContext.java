package org.ek9lang.compiler.phase7.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.core.AssertValue;

/**
 * Centralized context for IR generation within a single executable scope.
 * <p>
 * Each method, operator, function, or program gets its own IRGenerationContext
 * to ensure unique temporary variable names and scope identifiers within that scope.
 * </p>
 * <p>
 * This prevents conflicts like multiple "_temp1" variables within the same method
 * while allowing parallel processing of different methods/functions.
 * </p>
 * <p>
 * In general when decomposing EK9 statements/expressions within a processing block
 * we have to use many uniquely named elements. This is so that we can start to use SSA
 * in the IR we generate. So we're taking a big leap from one or a handful of EK9 statements
 * or expressions and expanding them into a larger number of IR statements. This brings
 * opportunities to optimise the IR in a much more significant way - before we get to code generation.
 * </p>
 */
public final class IRContext {

  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;

  // Per-prefix counters for logical numbering: _temp1, _param1, _return1, _scope1, etc.
  private final ConcurrentHashMap<String, AtomicInteger> prefixCounters = new ConcurrentHashMap<>();

  public IRContext(final IRContext fromContext) {
    this(fromContext.parsedModule, fromContext.compilerFlags);
  }

  /**
   * Create new IR generation context for an executable scope.
   *
   * @param parsedModule  The parsed module containing resolved symbols
   * @param compilerFlags The compiler flags for controlling debug instrumentation
   */
  public IRContext(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);
    AssertValue.checkNotNull("CompilerFlags cannot be null", compilerFlags);
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
  }

  /**
   * Get the parsed module for symbol resolution.
   */
  public ParsedModule getParsedModule() {
    return parsedModule;
  }

  /**
   * Get the compiler flags for debug instrumentation control.
   */
  public CompilerFlags getCompilerFlags() {
    return compilerFlags;
  }

  /**
   * Generate unique temporary variable name within this context.
   * Uses underscore prefix to avoid conflicts with EK9 user variables.
   *
   * @return Unique temp variable name like "_temp1", "_temp2", etc.
   */
  public String generateTempName() {
    return "_temp" + getNextCounterFor("temp");
  }

  /**
   * Generate unique scope identifier within this context.
   * Uses underscore prefix to avoid conflicts with EK9 user identifiers.
   *
   * @param prefix Descriptive prefix for the scope
   * @return Unique scope ID like "_param_1", "_return_1", "_scope_1", etc.
   */
  public String generateScopeId(final String prefix) {
    return "_" + prefix + "_" + getNextCounterFor(prefix);
  }

  /**
   * Generate unique basic block label within this context.
   * Uses underscore prefix to avoid conflicts with EK9 user identifiers.
   *
   * @param prefix Descriptive prefix for the block
   * @return Unique block label like "_entry_1", "_if_then_1", "_loop_1", etc.
   */
  public String generateBlockLabel(final String prefix) {
    return "_" + prefix + "_" + getNextCounterFor(prefix);
  }

  /**
   * Generate unique label name within this context for control flow.
   * Uses underscore prefix to avoid conflicts with EK9 user identifiers.
   *
   * @param prefix Descriptive prefix for the label.
   * @return Unique label like "_end_1", "_loop_exit_1", "_var_unset_1", etc.
   */
  public String generateLabelName(final String prefix) {
    return "_" + prefix + "_" + getNextCounterFor(prefix);
  }

  /**
   * Get or create counter for the specified prefix and increment it.
   * Thread-safe using ConcurrentHashMap and AtomicInteger.
   *
   * @param prefix The prefix to get counter for
   * @return The incremented counter value for this prefix
   */
  @SuppressWarnings("checkstyle:LambdaParameterName")
  private int getNextCounterFor(final String prefix) {
    return prefixCounters.computeIfAbsent(prefix, _ -> new AtomicInteger(0)).incrementAndGet();
  }

  /**
   * Get current counter value for a specific prefix (for debugging/testing).
   *
   * @param prefix The prefix to check
   * @return Current counter value, or 0 if prefix hasn't been used
   */
  public int getCounterFor(final String prefix) {
    final var counter = prefixCounters.get(prefix);
    return counter != null ? counter.get() : 0;
  }

  /**
   * Get current temp counter value (for debugging/testing).
   */
  public int getTempCounter() {
    return getCounterFor("temp");
  }

  /**
   * Get total number of unique prefixes used (for debugging/testing).
   */
  public int getUniquePrefixCount() {
    return prefixCounters.size();
  }
}