package org.ek9lang.compiler.phase0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.ek9lang.compiler.common.Ek9SourceVisitor;
import org.ek9lang.compiler.common.JustParser;
import org.ek9lang.compiler.common.PackageDetails;
import org.junit.jupiter.api.Test;

/**
 * Just test the parsing of packages using the Basic Parser used in the compiler
 * This also checks that the basic EK9 source visitor works.
 * <p>
 * These are both used in the command line tool as part of the packaging options.
 */
final class TestPackageParsing {

  /**
   * Deals with the conversion of url to file and the specific exceptions we may encounter.
   */
  private final Function<URL, File> urlToFile = url -> {
    try {
      return new File(url.toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  };

  /**
   * Function to take a resource name for a file and open create a File object for it.
   */
  private final Function<String, File> fileForClassPathResource = resourceName -> {
    var optionalURL = Optional.ofNullable(getClass().getResource(resourceName));
    if (optionalURL.isEmpty()) {
      fail("Unable to open file " + resourceName);
    }

    return urlToFile.apply(optionalURL.get());
  };

  /**
   * Create EK9 source visitor and parse the file, returning the visitor.
   */
  private final Function<File, Ek9SourceVisitor> validFileParser = file -> {
    Ek9SourceVisitor visitor = new Ek9SourceVisitor();
    JustParser underTest = new JustParser();
    boolean result = underTest.readSourceFile(file, visitor);
    assertTrue(result);
    return visitor;
  };

  /**
   * Accept a file name (resource name), get file for it open it parse it and return visitor.
   */
  private final Function<String, Ek9SourceVisitor> validParsedFile =
      resourceName -> Optional.of(resourceName).stream().map(fileForClassPathResource)
          .map(validFileParser).findFirst()
          .orElseThrow(() -> new RuntimeException("Expecting to parse file " + resourceName));

  /**
   * Check we can load a file with bad EK9 source code in it.
   */
  private final Consumer<File> checkBadPackage = file -> {
    Ek9SourceVisitor visitor = new Ek9SourceVisitor();
    JustParser underTest = new JustParser();
    boolean result = underTest.readSourceFile(file, visitor);
    assertFalse(result);
  };

  /**
   * Just the code that validates the simple package, by accepting the optional package details to validate.
   */
  private final Consumer<PackageDetails> validateSimplePackage = details -> {
    assertEquals("net.customer", details.moduleName());
    assertEquals("1.0.0-0", details.version());
    assertTrue(details.publicAccess());
    assertEquals("Simulation of something to exclude", details.description());

    assertTrue(details.tags().contains("tools"));
    assertTrue(details.includeFiles().contains("**.{csv,jpeg}"));
    assertTrue(details.includeFiles().contains("Chelford"));
    assertTrue(details.includeFiles().contains("Guff"));

    assertTrue(details.deps().containsKey("ekopen.network.support.utils"));
    assertEquals("1.6.1-9", details.deps().get("ekopen.network.support.utils"));
    assertTrue(details.deps().containsKey("ekopen.net.handy.tools"));
    assertEquals("3.2.1-0", details.deps().get("ekopen.net.handy.tools"));

    assertTrue(details.devDeps().containsKey("ekopen.org.supertools.util"));
    assertEquals("4.6.1-6", details.devDeps().get("ekopen.org.supertools.util"));
    assertTrue(details.devDeps().containsKey("ekopen.org.net.tools.misc"));
    assertEquals("3.2.3-21", details.devDeps().get("ekopen.org.net.tools.misc"));

    assertTrue(details.excludeDeps().containsKey("ekopen.some.bad.dependency.pack"));
    assertEquals("ekopen.org.supertools.util",
        details.excludeDeps().get("ekopen.some.bad.dependency.pack"));
  };

  private final Consumer<PackageDetails> validateFullPackage = details -> {
    assertEquals("example.networking", details.moduleName());
    assertFalse(details.publicAccess());
    assertEquals("2.3.14-20", details.version());

    assertFalse(details.applyStandardIncludes());
    assertTrue(details.applyStandardExcludes());

    assertTrue(details.includeFiles().contains("**.{txt,cal}"));
    assertTrue(details.excludeFiles().contains("sample/images/{perch.png,nonSuch.jpeg}"));

    assertEquals(25, details.versionNumberOnLine());
    assertTrue(details.packagePresent());
    assertEquals(4, details.programs().size());
    assertEquals("MIT", details.license());

    //Will always be this for the dependencies in this example (TCPExample.ek9)
    assertEquals("0C5F4976C78292B001221E300A80F414D4B6F161CF4EFBA17B66DBF7DA7E3A5A",
        details.dependencyFingerPrint());
  };

  private final Consumer<PackageDetails> validateSmallPackage =
      details -> assertEquals("ekopen.net.handy.tools", details.moduleName());

  private final Function<Ek9SourceVisitor, PackageDetails> getVisitedPackageDetails =
      visitor -> visitor.getPackageDetails()
          .orElseThrow(() -> new RuntimeException("Expecting package details"));

  @Test
  void testUnableToOpenFile() {
    JustParser underTest = new JustParser();

    File file = new File("nonSuch.ek9");
    Ek9SourceVisitor visitor = new Ek9SourceVisitor();
    boolean result = underTest.readSourceFile(file, visitor);
    assertFalse(result);
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testBadPackages() {
    // A couple of source file with bad packaging in them.
    Stream.of("/examples/constructs/packages/BadPackage.ek9",
            "/badExamples/basics/unevenIndentation.ek9")
        .map(fileForClassPathResource)
        .forEach(checkBadPackage);
  }

  @Test
  void testPackaging() {
    var toTest = Map.of("/examples/constructs/packages/HandyTools.ek9", validateSmallPackage,
        "/examples/fullPrograms/networking/TCPExample.ek9", validateFullPackage,
        "/examples/constructs/packages/SinglePackage.ek9", validateSimplePackage);

    toTest.forEach(this::processTest);
  }

  private void processTest(String resourceName, Consumer<PackageDetails> validator) {
    Optional.of(resourceName).stream().map(validParsedFile).map(getVisitedPackageDetails)
        .forEach(validator);
  }
}
