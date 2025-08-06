package org.ek9lang.compiler.phase7;

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
public final class IRGenerationContext {

  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;
  private final AtomicInteger tempCounter = new AtomicInteger(0);
  private final AtomicInteger scopeCounter = new AtomicInteger(0);
  private final AtomicInteger blockCounter = new AtomicInteger(0);
  private final AtomicInteger labelCounter = new AtomicInteger(0);

  /**
   * Create new IR generation context for an executable scope.
   *
   * @param parsedModule The parsed module containing resolved symbols
   * @param compilerFlags The compiler flags for controlling debug instrumentation
   */
  public IRGenerationContext(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
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
    return "_temp" + tempCounter.incrementAndGet();
  }

  /**
   * Generate unique scope identifier within this context.
   * Uses underscore prefix to avoid conflicts with EK9 user identifiers.
   *
   * @param prefix Descriptive prefix for the scope
   * @return Unique scope ID like "_main_scope_1", "_if_scope_2", etc.
   */
  public String generateScopeId(final String prefix) {
    return "_" + prefix + "_" + scopeCounter.incrementAndGet();
  }

  /**
   * Generate unique basic block label within this context.
   * Uses underscore prefix to avoid conflicts with EK9 user identifiers.
   *
   * @param prefix Descriptive prefix for the block
   * @return Unique block label like "_entry_1", "_if_then_2", etc.
   */
  public String generateBlockLabel(final String prefix) {
    return "_" + prefix + "_" + blockCounter.incrementAndGet();
  }

  /**
   * Generate unique label name within this context for control flow.
   * Uses underscore prefix to avoid conflicts with EK9 user identifiers.
   *
   * @param prefix Descriptive prefix for the label.
   * @return Unique label like "_var1_unset_1", "_end_label_2", etc.
   */
  public String generateLabelName(final String prefix) {
    return "_" + prefix + "_" + labelCounter.incrementAndGet();
  }

  /**
   * Get current temp counter value (for debugging/testing).
   */
  public int getTempCounter() {
    return tempCounter.get();
  }

  /**
   * Get current scope counter value (for debugging/testing).
   */
  public int getScopeCounter() {
    return scopeCounter.get();
  }

  /**
   * Get current block counter value (for debugging/testing).
   */
  public int getBlockCounter() {
    return blockCounter.get();
  }
}