package org.ek9lang.compiler.ir.instructions;

import java.util.List;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.data.ProgramDetails;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.core.AssertValue;

/**
 * IR instruction containing all discovered programs and their metadata.
 * <p>
 * Used by backends to generate main() entry point and program selection logic.
 * Processed once per compilation - backends should skip duplicate occurrences.
 * </p>
 * <p>
 * Contains complete program registry including:
 * - Qualified program names
 * - Parameter signatures with types
 * - Application associations for dependency injection
 * </p>
 */
public final class ProgramEntryPointInstr extends IRInstr {

  private final List<ProgramDetails> availablePrograms;

  /**
   * Create program entry point instruction with complete program metadata.
   *
   * @param availablePrograms Complete list of all programs discovered during compilation
   * @param debugInfo Debug information for source mapping
   */
  public ProgramEntryPointInstr(final List<ProgramDetails> availablePrograms,
                                final DebugInfo debugInfo) {
    super(IROpcode.PROGRAM_ENTRY_POINT, null, debugInfo);
    AssertValue.checkNotNull("Available programs cannot be null", availablePrograms);
    this.availablePrograms = List.copyOf(availablePrograms);

    // Add human-readable operand for debugging/testing
    addOperand("programs_count=" + availablePrograms.size());
    for (var program : availablePrograms) {
      addOperand(program.toString());
    }
  }

  /**
   * Get the complete list of available programs.
   *
   * @return Immutable list of program details
   */
  public List<ProgramDetails> getAvailablePrograms() {
    return availablePrograms;
  }

  /**
   * Check if any programs have associated applications.
   *
   * @return true if at least one program has an application
   */
  public boolean hasApplications() {
    return availablePrograms.stream().anyMatch(ProgramDetails::hasApplication);
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    if (visitor instanceof ProgramEntryPointInstrVisitor programVisitor) {
      programVisitor.visitProgramEntryPointInstr(this);
    } else {
      super.accept(visitor);
    }
  }

  @Override
  public String toString() {
    final var sb = new StringBuilder();
    sb.append("PROGRAM_ENTRY_POINT_BLOCK\n");
    sb.append("[\n");
    sb.append("programs_count: ").append(availablePrograms.size()).append("\n");

    if (!availablePrograms.isEmpty()) {
      sb.append("available_programs:\n");
      sb.append("[\n");
      for (var program : availablePrograms) {
        sb.append(program).append("\n");
      }
      sb.append("]\n");
    }

    sb.append("]");
    return sb.toString();
  }

  /**
   * Visitor interface for specialized handling of ProgramEntryPointInstr.
   * Backends should implement this to process program metadata.
   */
  public interface ProgramEntryPointInstrVisitor {
    void visitProgramEntryPointInstr(ProgramEntryPointInstr instruction);
  }
}