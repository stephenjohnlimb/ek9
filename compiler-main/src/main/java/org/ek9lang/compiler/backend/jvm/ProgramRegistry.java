package org.ek9lang.compiler.backend.jvm;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.ir.data.ProgramDetails;

/**
 * Registry for collecting EK9 program metadata during compilation.
 * Used to generate the unified ek9.Main entry point class.
 */
public final class ProgramRegistry {

  private final List<ProgramDetails> programs = new ArrayList<>();
  private boolean isCollected = false;

  /**
   * Add program metadata to the registry.
   * Should be called during IR processing when ProgramEntryPointInstr is encountered.
   */
  public void addPrograms(final List<ProgramDetails> programDetails) {
    if (!isCollected) {
      this.programs.clear(); // Clear any previous data
      this.programs.addAll(programDetails);
      this.isCollected = true;
    }
  }

  /**
   * Get all collected programs.
   */
  public List<ProgramDetails> getPrograms() {
    return List.copyOf(programs);
  }

  /**
   * Check if any programs have been collected.
   */
  public boolean hasPrograms() {
    return !programs.isEmpty();
  }

  /**
   * Get the count of collected programs.
   */
  public int getProgramCount() {
    return programs.size();
  }

  /**
   * Check if program registry has been populated.
   */
  public boolean isPopulated() {
    return isCollected;
  }
}