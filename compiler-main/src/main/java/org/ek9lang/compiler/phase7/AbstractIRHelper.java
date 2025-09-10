package org.ek9lang.compiler.phase7;

import org.ek9lang.compiler.phase7.support.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRInstructionBuilder;
import org.ek9lang.core.AssertValue;

/**
 * Abstract base class for all IR generation helpers.
 * 
 * <p>Provides common infrastructure that all focused IR helpers need:
 * access to the IR generation context and instruction builder.</p>
 * 
 * <p>This base class follows the focused helper pattern where each
 * concrete helper handles a single responsibility in IR generation.</p>
 */
public abstract class AbstractIRHelper {

  /** The shared IR generation context with stack-based state management. */
  protected final IRGenerationContext context;
  
  /** The instruction builder for creating IR instructions with automatic context. */
  protected final IRInstructionBuilder instructionBuilder;

  /**
   * Create a new IR helper with the required context and builder.
   * 
   * @param context The IR generation context for scope and debug management
   * @param instructionBuilder The builder for creating IR instructions
   */
  protected AbstractIRHelper(final IRGenerationContext context, 
                            final IRInstructionBuilder instructionBuilder) {
    AssertValue.checkNotNull("Context cannot be null", context);
    AssertValue.checkNotNull("Instruction builder cannot be null", instructionBuilder);
    
    this.context = context;
    this.instructionBuilder = instructionBuilder;
  }
}