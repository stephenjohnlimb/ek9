package org.ek9lang.compiler.parsing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.function.Function;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.tokenizer.DelegatingLexer;
import org.ek9lang.compiler.tokenizer.Ek9Lexer;
import org.ek9lang.compiler.tokenizer.LexerPlugin;
import org.ek9lang.compiler.tokenizer.TokenStreamAssessment;
import org.ek9lang.core.utils.Logger;
import org.junit.jupiter.api.Test;

/**
 * Locates both good examples and checks they parse, but also badExamples to check they don't.
 */
final class TestAllExamples {

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
      LexerPlugin lexer = getEK9Lexer(CharStreams.fromStream(is));
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);

      String readability = new TokenStreamAssessment().assess(lexer, false);
      return "Readability of " + ek9SourceFile.getName() + " is " + readability;
    }
    catch (Exception ex) {
      throw new RuntimeException((ex));
    }
  };

  @Test
  void testValidEK9ExampleSource() {
    var func = readabilityAssessor.compose(getTestFunction(false));

    var before = System.currentTimeMillis();
    sourceFileList.apply("/examples")
        .parallelStream()
        .map(func)
        .forEach(System.out::println);

    var after = System.currentTimeMillis();
    System.out.println("Time taken for testValidEK9ExampleSource " + (after-before) + " ms");
  }

  @Test
  void testInvalidEK9ExampleSource() {
    var func = fileToFileName.compose(getTestFunction(true));
    sourceFileList.apply("/badExamples")
        .stream()
        .map(func)
        .forEach(System.out::println);
  }

  private Function<File, File> getTestFunction(final boolean expectError) {
    return ek9SourceFile -> {
      try (var is = new FileInputStream(ek9SourceFile)) {
        ErrorListener errorListener = new ErrorListener(ek9SourceFile.getName());
        LexerPlugin lexer = getEK9Lexer(CharStreams.fromStream(is));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        EK9Parser parser = new EK9Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        long before = System.currentTimeMillis();
        EK9Parser.CompilationUnitContext context = parser.compilationUnit();
        long after = System.currentTimeMillis();

        Logger.log("Parsed " + ek9SourceFile.getName() + " in " + (after - before) +
            "ms. Expecting Error [" + expectError + "]");

        if (!expectError) {
          if (!errorListener.isErrorFree()) {
            errorListener.getErrors().forEachRemaining(System.out::println);
          }
          assertTrue(errorListener.isErrorFree(),
              "Parsing of " + ek9SourceFile.getName() + " failed");
          assertNotNull(context);
        }
        else {
          assertFalse(errorListener.isErrorFree(),
              "Parsing of " + ek9SourceFile.getName() + " should have failed");
        }
        return ek9SourceFile;
      }
      catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    };
  }

  private LexerPlugin getEK9Lexer(CharStream charStream) {
    return new DelegatingLexer(new Ek9Lexer(charStream, EK9Parser.INDENT, EK9Parser.DEDENT));
  }
}
