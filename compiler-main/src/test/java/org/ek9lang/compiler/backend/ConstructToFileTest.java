package org.ek9lang.compiler.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.OsSupport;
import org.ek9lang.core.TargetArchitecture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ConstructToFileTest {

  private final OsSupport osSupport = new OsSupport(true);
  private final FileHandling fileHandling = new FileHandling(osSupport);

  @ParameterizedTest
  @CsvSource({"jvm", "llvm-go", "llvm-cpp"})
  void testOutputFileLocator(final String architecture) {

    final var compilerFlags = new CompilerFlags();
    compilerFlags.setTargetArchitecture(TargetArchitecture.from(architecture));

    final var underTest = new OutputFileLocator(fileHandling, compilerFlags);
    final var locator = underTest.get();
    assertNotNull(locator);

  }

  @ParameterizedTest
  @CsvSource({"jvm", "llvm-go", "llvm-cpp"})
  void testTargetLocator(final String architecture) {
    final var underTest = new TargetLocator();

    final var targetArchitecture = TargetArchitecture.from(architecture);
    final var target = underTest.apply(targetArchitecture);
    assertEquals(targetArchitecture, target.getArchitecture());

  }

}
