package org.ek9lang.compiler.support;

import java.io.IOException;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.tokenizer.Ek9Lexer;
import org.ek9lang.core.utils.Logger;

/**
 * Support for the EK9 language based on antlr GUI.
 */
public class Ek9Support extends AntlrSupport {

  /**
   * Create an EK9Support object for running the antlr test rig with a specidic file.
   */
  public Ek9Support(String inputFileName)
      throws IOException, IllegalArgumentException, SecurityException {
    super(inputFileName);
  }

  /**
   * The main application, just runs up the antlr GUI and tree presentation.
   */
  public static void main(String... args)
      throws IOException, IllegalArgumentException, SecurityException {
    if (args.length != 1) {
      Logger.log("Expect a single argument of the ek9 source file to process");
    } else {
      var ek9Support = new Ek9Support(args[0]);
      ek9Support.runTestRig();
    }
  }

  @Override
  protected Lexer getLexer(CharStream input, String sourceName) {
    return new Ek9Lexer(input, EK9Parser.INDENT, EK9Parser.DEDENT).setPrintTokensAsSupplied(false)
        .setSourceName(sourceName);
  }

  @Override
  protected String getGrammarName() {
    return "org.ek9lang.antlr.EK9";
  }
}