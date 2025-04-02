package org.ek9lang.compiler.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.OsSupport;
import org.ek9lang.core.TargetArchitecture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ConstructToFileTest {

  private final OsSupport osSupport = new OsSupport(true);
  private final FileHandling fileHandling = new FileHandling(osSupport);

  @ParameterizedTest
  @CsvSource({"JVM", "LLVM"})
  void testOutputFileLocator(final String architecture) {

    final var compilerFlags = new CompilerFlags();
    compilerFlags.setTargetArchitecture(TargetArchitecture.valueOf(architecture));

    final var underTest = new OutputFileLocator(fileHandling, compilerFlags);
    final var locator = underTest.get();
    assertNotNull(locator);

  }

  @ParameterizedTest
  @CsvSource({"JVM", "LLVM"})
  void testTargetLocator(final String architecture) {
    final var underTest = new TargetLocator();

    final var targetArchitecture = TargetArchitecture.valueOf(architecture);
    final var target = underTest.apply(targetArchitecture);
    assertEquals(targetArchitecture, target.getArchitecture());

  }

  /**
   * Inlined this to avoid threading race condition in creating temp directory.
   *
   * @throws IOException If the file cannot be created.
   */
  @Test
  void testOutputVisitorLocator() throws IOException {
    final var underTest = new OutputVisitorLocator();

    assertNotNull(fileHandling.getTempDirectory());

    final var jvmOutputFile = fileHandling.makeFileInTempDirectory(TargetArchitecture.JVM.toString());
    assertTrue(jvmOutputFile.createNewFile());
    final var jvmVisitor = underTest.apply(TargetArchitecture.JVM, jvmOutputFile);
    assertNotNull(jvmVisitor);

    final var llvmOutputFile = fileHandling.makeFileInTempDirectory(TargetArchitecture.LLVM.toString());
    assertTrue(llvmOutputFile.createNewFile());
    final var llvmVisitor = underTest.apply(TargetArchitecture.LLVM, llvmOutputFile);
    assertNotNull(llvmVisitor);

  }
}
