package org.ek9lang.compiler.phase7;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.phase7.support.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRInstructionBuilder;

/**
 * Handles IR generation for method and function calls.
 * 
 * <p>This helper consolidates call generation that was previously
 * handled by CallInstrGenerator and related classes. Uses stack-based 
 * context to eliminate parameter threading.</p>
 */
public class CallIRHelper extends AbstractIRHelper {

  public CallIRHelper(IRGenerationContext context, IRInstructionBuilder instructionBuilder) {
    super(context, instructionBuilder);
  }

  /**
   * Generate IR for a call expression.
   */
  public void generateFor(EK9Parser.CallContext ctx) {
    // TODO: Implement call IR generation
    // Consolidate logic from CallInstrGenerator and parameter promotion
  }
}