package org.ek9lang.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.compiler.tokenizer.ParserCreator;
import org.ek9lang.compiler.tokenizer.ParserSpec;
import org.ek9lang.compiler.tokenizer.TokenConsumptionListener;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.Digest;
import org.ek9lang.core.ExceptionConverter;
import org.ek9lang.core.Processor;

/**
 * Holds a reference to the name of the file being compiled a checksum of the
 * file and the date time last modified.
 * This is used to detect changes in source that needs to have a new parse tree
 * generated.
 * <br/>
 * But also note it can be created with just an inputStream so the in memory ek9 source can
 * also be supplied and parsed.
 */
public final class CompilableSource implements Source, Serializable, TokenConsumptionListener {

  // This is the full path to the filename.
  private final String filename;

  /**
   * For some resources like builtin.ek9 inside the jar we load from an inputStream.
   */
  private transient InputStream inputStream;

  //If it was brought in as part of a package then we need to know the module name of the package.
  //This is so we can let all parts of that package resolve against each other.
  //But stop code from other packages/main etc. Accessing anything put constructs defined at this
  //top level package name.
  private Digest.CheckSum checkSum;

  //Need to know if the source is a development source or a lib source or both or neither
  private boolean dev = false;
  private String packageModuleName;
  private boolean lib = false;
  private long lastModified = -1;
  private transient EK9Parser parser;
  private ErrorListener errorListener;

  //As the tokens get consumed by the parser and pulled from the Lexer this
  //class listens for tokenConsumed messages and records the line and token, so they can be searched
  // for in a Language Server use this is really important.
  private Map<Integer, ArrayList<IToken>> tokens = null;

  /**
   * Set once parsed.
   */
  private transient EK9Parser.CompilationUnitContext compilationUnitContext = null;

  /**
   * Create compilable source for a specific filename.
   */
  public CompilableSource(final String filename) {

    AssertValue.checkNotEmpty("Filename cannot be empty or null", filename);
    this.filename = filename;
    resetDetails();

  }

  /**
   * Create a compilable source with a name, but provide the inputStream.
   * This is useful for internal supplied sources.
   */
  public CompilableSource(final String filename, final InputStream inputStream) {

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
  public void tokenConsumed(final Token token) {

    final var line = tokens.computeIfAbsent(token.getLine(), k -> new ArrayList<>());
    line.add(new Ek9Token(token));

  }

  /**
   * Get the nearest source token on a particular line and character position.
   */
  public TokenResult nearestToken(final int line, final int characterPosition) {

    final var lineOfTokens = tokens.get(line);

    TokenResult rtn = new TokenResult();
    if (lineOfTokens != null) {
      //Now the position won't be exact, so we need to find the lower bound.
      for (int i = 0; i < lineOfTokens.size(); i++) {
        var t = lineOfTokens.get(i);
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
          "Need to call prepareToParse before accessing compilation unit for [" + filename + "]");
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

  public CompilableSource setDev(final boolean dev) {

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
  public void setLib(final String packageModuleName, final boolean lib) {

    this.packageModuleName = packageModuleName;
    this.lib = lib;

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
  public void resetDetails() {

    updateFileDetails();
    resetTokens();
    initialiseErrorListener();

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
    final var file = new File(filename);

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
  public boolean equals(final Object obj) {

    if (obj == this) {
      return true;
    }

    if (obj instanceof CompilableSource cs) {
      return cs.filename.equals(filename);
    }

    return false;
  }

  /**
   * Used in debugging built-in sources.
   */
  public String getSourceAsStringForDebugging() {

    final Processor<String> processor = () -> {
      StringBuilder builder = new StringBuilder("\n");
      int lineNo = 1;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream()))) {
        while (reader.ready()) {
          var line = reader.readLine();
          builder.append(String.format("%3d %s%n", lineNo++, line));
        }
        return builder.toString();
      }
    };

    return new ExceptionConverter<String>().apply(processor);
  }

  /**
   * Sets up the compilable source to be parsed.
   */
  public CompilableSource prepareToParse() {

    final Processor<CompilableSource> processor = () -> {
      try (InputStream input = getInputStream()) {
        return prepareToParse(input);
      }
    };

    return new ExceptionConverter<CompilableSource>().apply(processor);
  }

  /**
   * Prepare to parse but with a provided input stream of what to parse.
   */
  public CompilableSource prepareToParse(final InputStream inputStream) {

    AssertValue.checkNotNull("InputStream cannot be null", inputStream);
    initialiseErrorListener();

    final var spec = new ParserSpec(this::getGeneralIdentifier, inputStream, errorListener, this);
    parser = new ParserCreator().apply(spec);

    return this;
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

  private void initialiseErrorListener() {

    setErrorListener(new ErrorListener(getGeneralIdentifier()));

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

  private String getEncodedFileName(final String fileName) {

    final var uri = Path.of(fileName).toUri().toString();
    return uri.replace("c:", "c%3A").replace("C:", "c%3A");
  }
}
