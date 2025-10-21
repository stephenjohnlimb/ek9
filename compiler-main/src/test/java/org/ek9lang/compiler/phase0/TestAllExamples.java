package org.ek9lang.compiler.phase0;

import static org.ek9lang.core.AssertValue.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.function.Function;
import org.antlr.v4.runtime.CommonTokenStream;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SourceFileList;
import org.ek9lang.compiler.tokenizer.DelegatingLexer;
import org.ek9lang.compiler.tokenizer.Ek9LexerForInput;
import org.ek9lang.compiler.tokenizer.LexerPlugin;
import org.ek9lang.compiler.tokenizer.TokenStreamAssessment;
import org.ek9lang.core.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Locates both good examples and checks they parse, but also badExamples to check they don't.
 */
final class TestAllExamples {

  private final Ek9LexerForInput ek9LexerForInput = new Ek9LexerForInput();
  /**
   * Function just to convert File to file name, ready for output.
   */
  private final Function<File, String> fileToFileName = File::getName;

  private final SourceFileList sourceFileList = new SourceFileList();

  /**
   * Assesses the readability of a file and returns the result of that readability.
   */
  private final Function<File, String> readabilityAssessor = ek9SourceFile -> {
    try (var is = new FileInputStream(ek9SourceFile)) {
      ErrorListener errorListener = new ErrorListener(ek9SourceFile.getName());
      LexerPlugin lexer = getEK9Lexer(is);
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);

      String readability = new TokenStreamAssessment().assess(lexer, false);
      return "Readability of " + ek9SourceFile.getName() + " is " + readability;
    } catch (Exception ex) {
      throw new RuntimeException((ex));
    }
  };

  @Test
  void testValidEK9ExampleSource() {
    var func = readabilityAssessor.compose(getTestFunction(false));
    var before = System.currentTimeMillis();

    //Tried using virtual threads, but parallel stream is just as fast.
    //Normal stream is 12 seconds, parallelStream 4.5 seconds, virtual thirds direct is 4.7 seconds.

    //If you wish to see the readablity of each file alter the final consumer to use System.out::println.
    sourceFileList.apply("/examples")
        .parallelStream()
        .map(func)
        .forEach(Assertions::assertNotNull);

    var after = System.currentTimeMillis();

    //noinspection unused
    final var duration = (after - before);

    assertTrue(duration > 0, "Expect time to process to be positive");
    //Uncomment if you need to see how long it takes to process all the examples.
    //System.out.println("Time taken for testValidEK9ExampleSource " + duration + " ms");
  }

  @Test
  void testInvalidEK9ExampleSource() {
    var func = fileToFileName.compose(getTestFunction(true));
    sourceFileList.apply("/badExamples")
        .stream()
        .map(func)
        .forEach(Assertions::assertNotNull);
  }

  private Function<File, File> getTestFunction(final boolean expectError) {
    return ek9SourceFile -> {
      try (var is = new FileInputStream(ek9SourceFile)) {
        ErrorListener errorListener = new ErrorListener(ek9SourceFile.getName());
        LexerPlugin lexer = getEK9Lexer(is);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        EK9Parser parser = new EK9Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        long before = System.currentTimeMillis();
        EK9Parser.CompilationUnitContext context = parser.compilationUnit();
        long after = System.currentTimeMillis();
        final var errorFree = errorListener.isErrorFree();

        if ((!expectError && errorFree) || expectError && !errorFree) {
          return ek9SourceFile;
        }

        //Otherwise we have a failure situation

        Logger.log("Parsed " + ek9SourceFile.getName() + " in " + (after - before) +
            "ms. Expecting Error [" + expectError + "]");

        if (!errorFree) {
          errorListener.getErrors().forEachRemaining(System.out::println);
        } else {
          assertNotNull(context);
        }

        assertFalse(expectError, "Parsing of " + ek9SourceFile.getName() + " failed");

        fail("Parsing of " + ek9SourceFile.getName() + " should have failed");


        return ek9SourceFile;
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    };
  }

  private LexerPlugin getEK9Lexer(InputStream inputStream) {
    return new DelegatingLexer(ek9LexerForInput.apply(inputStream));
  }
}
