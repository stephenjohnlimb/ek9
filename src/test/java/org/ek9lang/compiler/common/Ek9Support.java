package org.ek9lang.compiler.common;

import java.io.IOException;
import java.io.InputStream;
import org.antlr.v4.runtime.Lexer;
import org.ek9lang.compiler.tokenizer.Ek9LexerForInput;
import org.ek9lang.core.Logger;

/**
 * Support for the EK9 language based on antlr GUI.
 */
public class Ek9Support extends AntlrSupport {

  /**
   * Create an EK9Support object for running the antlr test rig with a specidic file.
   */
  public Ek9Support(final String inputFileName, final boolean showReadability)
      throws IOException, IllegalArgumentException, SecurityException {
    super(inputFileName, showReadability);
  }

  /**
   * The main application, just runs up the antlr GUI and tree presentation.
   */
  public static void main(String... args)
      throws IOException, IllegalArgumentException, SecurityException {
    if (args.length != 1) {
      Logger.log("Expect a single argument of the ek9 source file to process");
    } else {
      var ek9Support = new Ek9Support(args[0], true);
      ek9Support.runTestRig();
    }
  }

  @Override
  protected Lexer getLexer(InputStream input, String sourceName) {
    var ek9LexerForInput = new Ek9LexerForInput();
    return ek9LexerForInput.apply(input)
        .setPrintTokensAsSupplied(false)
        .setSourceName(sourceName);
  }

  @Override
  protected String getGrammarName() {
    return "org.ek9lang.antlr.EK9";
  }
}
