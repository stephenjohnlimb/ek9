package org.ek9lang.compiler.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.ek9lang.core.exception.CompilerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Just checks all the functionality present in the CompilableSource class.
 * But this does use a real parser and real files. There is no mocking.
 */
final class CompilableSourceTest {

  private static final Supplier<CompilableSource> validEk9Source = () -> {
    URL helloWorld = CompilableSourceTest.class.getResource("/examples/basics/HelloWorld.ek9");

    assertNotNull(helloWorld, "Expecting URL for hello world to be available.");
    var helloWorldSource = new CompilableSource(helloWorld.getPath());
    assertNotNull(helloWorldSource, "Expecting source to be available");
    return helloWorldSource;
  };

  private static final UnaryOperator<CompilableSource> processEk9Source = source -> {
    source.prepareToParse().parse();
    return source;
  };

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "no_such_file"})
  void testConstruction(String fileName) {
    try {
      new CompilableSource(fileName);
      fail("Expecting an exception as file does not exist");
    } catch (IllegalArgumentException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  void checkValidEk9SourceFileNotModified() {
    var helloWorldSource = validEk9Source.get();
    assertFalse(helloWorldSource.isModified());
  }

  @Test
  void testEquality() {
    var helloWorldSource1 = validEk9Source.get();
    var helloWorldSource2 = validEk9Source.get();

    assertEquals(helloWorldSource1, helloWorldSource2);
    assertEquals(helloWorldSource1, helloWorldSource1);

    assertEquals(helloWorldSource1.hashCode(), helloWorldSource2.hashCode());
  }

  @Test
  void testLibDev() {
    var helloWorldSource1 = validEk9Source.get();
    assertFalse(helloWorldSource1.isDev());
    assertFalse(helloWorldSource1.isLib());
    assertNull(helloWorldSource1.getPackageModuleName());

    helloWorldSource1.setDev(true);
    helloWorldSource1.setLib("somePackageModuleName", true);
    assertTrue(helloWorldSource1.isDev());
    assertTrue(helloWorldSource1.isLib());
    assertEquals("somePackageModuleName", helloWorldSource1.getPackageModuleName());
  }

  @Test
  void loadingValidEk9SourceFile() {
    var helloWorldSource = validEk9Source.get();

    assertNotNull(helloWorldSource.getGeneralIdentifier());

    helloWorldSource.prepareToParse();

    //That's the file loaded but not parsed.
    //end of this test.
  }

  @Test
  void parseValidEk9SourceFile() {
    var helloWorldSource = validEk9Source.get();
    helloWorldSource.prepareToParse();
    var parseResult = helloWorldSource.parse();
    assertNotNull(parseResult);

    assertTrue(helloWorldSource.getErrorListener().isErrorFree());

    var context = helloWorldSource.getCompilationUnitContext();
    //Just check same instance provided.
    assertEquals(parseResult, context);
  }

  @ParameterizedTest
  @MethodSource("getInvalidCalls")
  void testIncorrectProcessingOfEk9SourceFile(Supplier<?> invalidCallToMake) {
    try {
      invalidCallToMake.get();
      fail("Expecting exception");
    } catch (CompilerException ex) {
      assertNotNull(ex);
    }
  }

  @ParameterizedTest
  @MethodSource("getPositionsToSearchFor")
  void testFindingToken(ExpectedTokenPosition expectation) {
    var helloWorldSource = processEk9Source.apply(validEk9Source.get());
    var result =
        helloWorldSource.nearestToken(expectation.lineNumber, expectation.characterPosition);
    expectation.assertCorrectTextOfToken(result);
  }

  @Test
  void testNotFindingToken() {
    var helloWorldSource = processEk9Source.apply(validEk9Source.get());
    var result = helloWorldSource.nearestToken(0, 0);
    assertFalse(result.isPresent());
  }

  static Stream<Supplier<?>> getInvalidCalls() {
    var helloWorldSource = validEk9Source.get();

    return Stream.of(helloWorldSource::getCompilationUnitContext, helloWorldSource::parse);
  }

  static Stream<ExpectedTokenPosition> getPositionsToSearchFor() {
    return Stream.of(new ExpectedTokenPosition(2, 1, "defines"),
        new ExpectedTokenPosition(2, 8, "defines"), new ExpectedTokenPosition(2, 9, "module"),
        new ExpectedTokenPosition(2, 15, "module"),
        new ExpectedTokenPosition(2, 16, "introduction"),
        new ExpectedTokenPosition(3, 5, "defines"), new ExpectedTokenPosition(4, 5, "HelloWorld"),
        new ExpectedTokenPosition(5, 7, "stdout"), new ExpectedTokenPosition(5, 14, "<-"));
  }

  private static class ExpectedTokenPosition {
    int lineNumber;
    int characterPosition;
    String expectedTextOfToken;

    public ExpectedTokenPosition(int lineNumber, int characterPosition,
                                 String expectedTextOfToken) {
      this.lineNumber = lineNumber;
      this.characterPosition = characterPosition;
      this.expectedTextOfToken = expectedTextOfToken;
    }

    public void assertCorrectTextOfToken(TokenResult result) {
      assertNotNull(result);
      assertTrue(result.isPresent(), "no result for " + this);
      assertEquals(expectedTextOfToken, result.getToken().getText());
    }

    @Override
    public String toString() {
      return lineNumber + " " + characterPosition + " " + expectedTextOfToken;
    }
  }
}
