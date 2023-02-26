package org.ek9lang.compiler.internals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.tokenizer.Ek9Lexer;
import org.ek9lang.compiler.tokenizer.TokenConsumptionListener;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.exception.CompilerException;
import org.ek9lang.core.utils.Digest;

/**
 * Holds a reference to the name of the file being compiled a checksum of the
 * file and the date time last modified.
 * This is used to detect changes in source that needs to have a new parse tree
 * generated.
 */
public final class CompilableSource implements Source, TokenConsumptionListener {
  // This is the full path to the filename.
  private String filename;

  /**
   * For some resources like builtin.ek9 inside the jar we load from an inputstream.
   */
  private InputStream inputStream;

  //If it was brought in as part of a package then we need to know the module name of the package.
  //This is so we can let all parts of that package resolve against each other.
  //But stop code from other packages/main etc. Accessing anything put constructs defined at this
  //top level package name.
  protected Digest.CheckSum checkSum = null;
  //Need to know if the source is a development source or a lib source or both or neither
  private boolean dev = false;
  private String packageModuleName;
  private boolean lib = false;
  private long lastModified = -1;

  private EK9Parser parser;

  private ErrorListener errorListener;

  //As the tokens get consumed by the parser and pulled from the Lexer this
  //class listens for tokenConsumed messages and records the line and token, so they can be searched
  // for in a Language Server use this is really important.
  private Map<Integer, ArrayList<Token>> tokens = null;

  /**
   * Set once parsed.
   */
  private EK9Parser.CompilationUnitContext compilationUnitContext = null;

  /**
   * Create compilable source for a specific filename.
   */
  public CompilableSource(String filename) {
    AssertValue.checkNotEmpty("Filename cannot be empty or null", filename);
    this.filename = filename;
    resetDetails();
  }

  /**
   * Create a compilable source with a name, but provide the inputStream.
   * This is useful for internal supplied sources.
   */
  public CompilableSource(String filename, InputStream inputStream) {
    AssertValue.checkNotEmpty("Filename cannot be empty or null", filename);
    AssertValue.checkNotNull("InputStream cannot be empty or null", inputStream);
    this.filename = filename;
    this.inputStream = inputStream;
    resetDetails();
  }

  /**
   * Informed when a token have been consumed out of the Lexer.
   */
  @Override
  public void tokenConsumed(Token token) {
    ArrayList<Token> line = tokens.computeIfAbsent(token.getLine(), k -> new ArrayList<>());
    //create and add in to the map
    //Now add the token.
    line.add(token);
  }

  /**
   * Get the nearest source token on a particular line and character position.
   */
  public TokenResult nearestToken(int line, int characterPosition) {
    TokenResult rtn = new TokenResult();
    ArrayList<Token> lineOfTokens = tokens.get(line);
    if (lineOfTokens != null) {
      //Now the position won't be exact, so we need to find the lower bound.
      for (int i = 0; i < lineOfTokens.size(); i++) {
        Token t = lineOfTokens.get(i);
        if (t.getCharPositionInLine() < characterPosition) {
          rtn = new TokenResult(t, lineOfTokens, i);
        } else {
          return rtn;
        }
      }
    }
    return rtn;
  }

  /**
   * Provide access to the main compilation context.
   * This is only valid if a prepareToParse and parse has been run.
   */
  public EK9Parser.CompilationUnitContext getCompilationUnitContext() {
    if (hasNotBeenSuccessfullyParsed()) {
      throw new CompilerException(
          "Need to call prepareToParse before accessing compilation unit");
    }
    return compilationUnitContext;
  }

  public boolean hasNotBeenSuccessfullyParsed() {
    return this.compilationUnitContext == null;
  }

  @Override
  public boolean isDev() {
    return dev;
  }

  public CompilableSource setDev(boolean dev) {
    this.dev = dev;
    return this;
  }

  @Override
  public boolean isLib() {
    return lib;
  }

  public String getPackageModuleName() {
    return packageModuleName;
  }

  /**
   * Set this compilable source as a library.
   */
  public CompilableSource setLib(String packageModuleName, boolean lib) {
    this.packageModuleName = packageModuleName;
    this.lib = lib;
    return this;
  }

  /**
   * Checks if the file content have changed from when first calculated.
   *
   * @return true if modified, false if modified time and checksum are the same.
   */
  public boolean isModified() {
    return lastModified != calculateLastModified() || !checkSum.equals(calculateCheckSum());
  }

  /**
   * Just updates the last modified and the check sum to current values.
   */
  public CompilableSource resetDetails() {
    updateFileDetails();
    resetTokens();
    setErrorListener(new ErrorListener(getGeneralIdentifier()));
    return this;
  }

  private void resetTokens() {
    tokens = new HashMap<>();
  }

  private void updateFileDetails() {
    lastModified = calculateLastModified();
    checkSum = calculateCheckSum();
  }

  private long calculateLastModified() {
    //Do not try and check if modified as it is internal to the package.
    if (inputStream != null) {
      return 0;
    }
    AssertValue.checkCanReadFile("Unable to read file", filename);
    File file = new File(filename);
    return file.lastModified();
  }

  private Digest.CheckSum calculateCheckSum() {
    if (inputStream != null) {
      return Digest.digest("filename");
    }
    AssertValue.checkCanReadFile("Unable to read file", filename);
    return Digest.digest(new File(filename));
  }

  @Override
  public int hashCode() {
    return filename.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    var rtn = false;
    if (obj instanceof CompilableSource cs) {
      rtn = cs.filename.equals(filename);
    }

    return rtn;
  }

  /**
   * Used in debugging built-in sources.
   */
  public String getSourceAsStringForDebugging() {
    try (InputStream input = getInputStream()) {
      return new String(input.readAllBytes());
    } catch (Exception ex) {
      throw new CompilerException("Unable load file " + filename, ex);
    }
  }

  /**
   * Sets up the compilable source to be parsed.
   */
  public CompilableSource prepareToParse() {
    try (InputStream input = getInputStream()) {
      return prepareToParse(input);
    } catch (Exception ex) {
      throw new CompilerException("Unable to parse file " + filename, ex);
    }
  }

  /**
   * Prepare to parse but with a provided input stream of what to parse.
   */
  public CompilableSource prepareToParse(final InputStream inputStream) {
    try {
      //So make a new error listener to get all the errors
      setErrorListener(new ErrorListener(getGeneralIdentifier()));
      //we will set the parsed module once parsed.
      Ek9Lexer lexer = new Ek9Lexer(CharStreams.fromStream(inputStream), EK9Parser.INDENT,
          EK9Parser.DEDENT).setSourceName(filename);
      lexer.setTokenListener(this);
      parser = new EK9Parser(new CommonTokenStream(lexer));
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);
      return this;
    } catch (Exception ex) {
      throw new CompilerException("Unable to parse file " + filename, ex);
    }
  }

  public CompilableSource completeParsing() {
    this.parse();
    return this;
  }

  /**
   * Actually parse the source code.
   */
  public EK9Parser.CompilationUnitContext parse() {
    if (parser != null) {
      resetTokens();
      compilationUnitContext = parser.compilationUnit();
      return compilationUnitContext;
    }
    throw new CompilerException("Need to call prepareToParse before accessing compilation unit");
  }

  /**
   * Access the error listener, only check after parsing.
   */
  public ErrorListener getErrorListener() {
    return this.errorListener;
  }

  private void setErrorListener(ErrorListener listener) {
    this.errorListener = listener;
  }

  private InputStream getInputStream() throws FileNotFoundException {
    //In the case where an input stream was provided.
    if (inputStream != null) {
      return inputStream;
    }
    return new FileInputStream(filename);
  }

  @Override
  public String toString() {
    return getFileName();
  }

  @Override
  public String getFileName() {
    return filename;
  }

  public String getGeneralIdentifier() {
    return getEncodedFileName(toString());
  }

  private String getEncodedFileName(String fileName) {
    String uri = Path.of(fileName).toUri().toString();
    return uri.replace("c:", "c%3A").replace("C:", "c%3A");
  }
}
