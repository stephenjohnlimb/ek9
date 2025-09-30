package org.ek9lang.compiler.backend.llvm.cpp;

import org.ek9lang.compiler.backend.IMainEntryVisitor;
import org.ek9lang.compiler.backend.MainEntryTargetTuple;
import org.ek9lang.core.AssertValue;

/**
 * LLVM/C++ main entry visitor for generating native binary main entry points.
 * This is a placeholder implementation for future LLVM backend main generation.
 */
public final class MainEntryVisitor implements IMainEntryVisitor {

  private final MainEntryTargetTuple mainEntryTargetTuple;

  public MainEntryVisitor(final MainEntryTargetTuple mainEntryTargetTuple) {
    AssertValue.checkNotNull("MainEntryTargetTuple cannot be null", mainEntryTargetTuple);
    this.mainEntryTargetTuple = mainEntryTargetTuple;
  }

  @Override
  public void visit() {
    generateMainEntry();
  }

  /**
   * Generate native main entry point (placeholder implementation).
   * Determines own file path and naming conventions for LLVM target.
   * Future implementation will generate C++ main function or equivalent for LLVM target.
   */
  private void generateMainEntry() {
    // LLVM-specific file naming: no extension for native binary
    final var outputDir = mainEntryTargetTuple.outputDirectory();
    final var mainExecutableFile = new java.io.File(outputDir, "Main");

    // Placeholder for future LLVM main entry generation
    System.out.println("LLVM main entry generation not yet implemented - placeholder called for "
        + mainEntryTargetTuple.programEntryPoint().getAvailablePrograms().size() + " program(s)");
    System.out.println("Would generate native executable: " + mainExecutableFile.getAbsolutePath());

    // Future implementation will:
    // 1. Generate C++ main function
    // 2. Create native binary entry point at mainExecutableFile
    // 3. Handle program selection for multiple programs
    // 4. Use fileHandling utilities from mainEntryTargetTuple.fileHandling()
  }

}