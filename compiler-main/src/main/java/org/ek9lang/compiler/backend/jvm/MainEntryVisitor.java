package org.ek9lang.compiler.backend.jvm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.ek9lang.compiler.backend.IMainEntryVisitor;
import org.ek9lang.compiler.backend.MainEntryTargetTuple;
import org.ek9lang.core.AssertValue;

/**
 * JVM-specific main entry visitor that generates ek9.Main class with Java bytecode.
 * Uses MainClassGenerator to create proper Java main method with reflection-based
 * program execution.
 */
public final class MainEntryVisitor implements IMainEntryVisitor {

  private final MainEntryTargetTuple mainEntryTargetTuple;
  private final MainClassGenerator mainClassGenerator = new MainClassGenerator();

  public MainEntryVisitor(final MainEntryTargetTuple mainEntryTargetTuple) {
    AssertValue.checkNotNull("MainEntryTargetTuple cannot be null", mainEntryTargetTuple);
    this.mainEntryTargetTuple = mainEntryTargetTuple;
  }

  @Override
  public void visit() {
    try {
      // Get output directory and create ek9 subdirectory (JVM-specific structure)
      final var outputDir = mainEntryTargetTuple.outputDirectory();
      final var ek9Dir = new File(outputDir, "ek9");
      if (!ek9Dir.exists() && !ek9Dir.mkdirs()) {
        System.err.println("Failed to create ek9 directory: " + ek9Dir.getAbsolutePath());
        return;
      }

      // Generate bytecode using MainClassGenerator
      final var programEntryPoint = mainEntryTargetTuple.programEntryPoint();

      final var bytecode = mainClassGenerator.apply(programEntryPoint);

      // Create JVM-specific target file: Main.class
      final var mainClassFile = new File(ek9Dir, "Main.class");
      try (var outputStream = new FileOutputStream(mainClassFile)) {
        outputStream.write(bytecode);
      }

    } catch (IOException e) {
      System.err.println("Failed to generate ek9/Main.class: " + e.getMessage());
      // Don't throw - allow compilation to continue even if Main.class generation fails
    }
  }
}