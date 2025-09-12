package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.PrimaryReferenceProcessingDetails;
import org.ek9lang.core.AssertValue;

/**
 * Generates IR instructions for primary references (THIS and SUPER keywords).
 * <p>
 * Handles:<br>
 * - THIS keyword references<br>
 * - SUPER keyword references<br>
 * - Memory loading with appropriate variable names<br>
 * </p>
 * <p>
 * Memory management (RETAIN/SCOPE_REGISTER) is handled by higher-level expression processors.
 * This generator only creates the LOAD instruction for the reference.
 * </p>
 */
final class PrimaryReferenceGenerator implements Function<PrimaryReferenceProcessingDetails, List<IRInstr>> {

  @Override
  public List<IRInstr> apply(final PrimaryReferenceProcessingDetails details) {
    AssertValue.checkNotNull("Details cannot be null", details);

    final var ctx = details.ctx();
    final var resultVariable = details.resultVariable();
    final var debugInfo = details.debugInfo();

    final String variableName;
    if (ctx.THIS() != null) {
      variableName = IRConstants.THIS;
    } else if (ctx.SUPER() != null) {
      variableName = IRConstants.SUPER;
    } else {
      throw new IllegalStateException("Unexpected primary reference type: " + ctx.getText());
    }

    // Generate LOAD instruction for the reference
    // Memory management (RETAIN/SCOPE_REGISTER) will be handled by higher-level processors
    return List.of(MemoryInstr.load(resultVariable, variableName, debugInfo));
  }
}