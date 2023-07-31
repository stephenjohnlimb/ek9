package org.ek9lang.compiler.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import org.antlr.v4.gui.TestRig;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.ek9lang.compiler.tokenizer.DelegatingLexer;
import org.ek9lang.compiler.tokenizer.LexerPlugin;
import org.ek9lang.compiler.tokenizer.TokenStreamAssessment;
import org.ek9lang.core.Logger;

/**
 * Support of Antlr.
 */
public abstract class AntlrSupport {

  /**
   * Show the stream of tokens for the file being parsed (lexed for tokens).
   */
  private static final boolean STREAM_TOKENS = true;

  private final TestRig testRig;

  protected AntlrSupport(String inputFileName)
      throws IOException, IllegalArgumentException, SecurityException {
    if (!new File(inputFileName).exists()) {
      Logger.log("AntlrSupport cannot find file [" + inputFileName + "]");
    } else {
      Logger.log("AntlrSupport for EK9");
    }

    //This is useful to see the tokens that are being presented to the parser
    //The parse need to type and find the right combinations to create the parse structure.
    if (STREAM_TOKENS) {
      streamLexerTokensFor(inputFileName);
    }

    testRig = makeTestRig(inputFileName);
  }

  /**
   * Run the rig now it has been created.
   */
  public void runTestRig() {
    if (testRig != null) {
      try {
        testRig.process();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  protected abstract Lexer getLexer(InputStream input, String sourceName);

  protected abstract String getGrammarName();

  private void streamLexerTokensFor(String inputFileName)
      throws IOException, IllegalArgumentException, SecurityException {
    LexerPlugin lexer1 = new DelegatingLexer(
        (LexerPlugin) getLexer(new FileInputStream(inputFileName), inputFileName));
    new TokenStreamAssessment().assess(lexer1, true);
  }

  private TestRig makeTestRig(String inputFileName) {
    String grammarName = getGrammarName();
    String[] params = {grammarName, "compilationUnit", "-gui", inputFileName};
    Logger.log("Running TestRig with " + Arrays.toString(params));
    try {

      //Now because the test Rig is written the way it is; and we want to use our
      //own lexer we have to do this
      return new TestRig(params) {
        @Override
        public void process() throws Exception {
          ClassLoader cl = Thread.currentThread().getContextClassLoader();
          Lexer lexer = getLexer(null, inputFileName);

          Class<? extends Parser> parserClass = null;
          Parser parser = null;
          if (!startRuleName.equals(LEXER_START_RULE_NAME)) {
            String parserName = grammarName + "Parser";
            parserClass = cl.loadClass(parserName).asSubclass(Parser.class);
            Constructor<? extends Parser> parserCtor =
                parserClass.getConstructor(TokenStream.class);
            parser = parserCtor.newInstance((TokenStream) null);
          }

          Charset charset =
              (encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
          if (inputFiles.isEmpty()) {
            CharStream charStream = CharStreams.fromStream(System.in, charset);
            process(lexer, parserClass, parser, charStream);
            return;
          }
          for (String inputFile : inputFiles) {
            CharStream charStream = CharStreams.fromPath(Paths.get(inputFile), charset);
            if (inputFiles.size() > 1) {
              Logger.error(inputFile);
            }
            process(lexer, parserClass, parser, charStream);
          }
        }
      };
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
