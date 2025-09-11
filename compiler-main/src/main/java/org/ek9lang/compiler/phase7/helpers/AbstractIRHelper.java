package org.ek9lang.compiler.phase7.helpers;

import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
import org.ek9lang.core.AssertValue;

/**
 * Abstract base class for all IR generation helpers.
 * 
 * <p>Provides common infrastructure that all focused IR helpers need:
 * access to the instruction builder which contains the IR generation context.</p>
 * 
 * <p>This base class follows the focused helper pattern where each
 * concrete helper handles a single responsibility in IR generation.</p>
 */
public abstract class AbstractIRHelper {
  
  /** The instruction builder for creating IR instructions with automatic context. */
  protected final IRInstructionBuilder instructionBuilder;

  /**
   * Create a new IR helper with the instruction builder.
   * The builder contains the IRGenerationContext for scope and debug management.
   * 
   * @param instructionBuilder The builder for creating IR instructions and accessing context
   */
  protected AbstractIRHelper(final IRInstructionBuilder instructionBuilder) {
    AssertValue.checkNotNull("Instruction builder cannot be null", instructionBuilder);
    
    this.instructionBuilder = instructionBuilder;
  }
  
  /**
   * Get the IR generation context from the instruction builder.
   * Convenience method for helpers that need direct context access.
   */
  protected IRGenerationContext getContext() {
    return instructionBuilder.getContext();
  }
}