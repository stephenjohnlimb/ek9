package org.ek9lang.compiler;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.core.Logger;

/**
 * Designed to represent one or more source files that are part of a workspace.
 * Needs to become thread safe, especially around parsing.
 * This is because when a user is typing via lsp mode - this will get triggered over and over.
 */
public class Workspace {
  //The maps of source code file to compilable source objects

  //The ordering sources are added is important for bootstrap of the compiler.
  //So uses a linked hashmap to preserve the order of addition.
  private final Map<String, CompilableSource> sources = new LinkedHashMap<>();

  /**
   * ReParses or loads and parses a source file.
   */
  public CompilableSource reParseSource(final Path path) {

    return reParseSource(path.toString());
  }

  /**
   * Typically used via the language server.
   */
  public CompilableSource reParseSource(final String uri, final InputStream inputStream) {

    Logger.debug("parsing/re-parsing [" + uri + "] but with direct input stream");

    final var compilableSource = ensureCompilableSourceAvailable(uri);
    compilableSource.prepareToParse(inputStream).parse();

    return compilableSource;
  }

  /**
   * Triggers the re-parsing of the source file. Normally after an edit so errors can be checked.
   */
  public CompilableSource reParseSource(final String uri) {

    //Consider a queue of requests per uri as in an interactive mode the same file
    //will be triggered for parsing over and over again. We only need one request to be honoured!
    Logger.debug("parsing/re-parsing [" + uri + "]");

    final var compilableSource = ensureCompilableSourceAvailable(uri);
    compilableSource.prepareToParse().parse();

    return compilableSource;
  }

  private CompilableSource ensureCompilableSourceAvailable(final String uri) {

    if (isSourcePresent(uri)) {
      return getSource(uri);
    }

    return addSource(new CompilableSource(uri));
  }

  /*
  ParsedModule and IRModule to be added in
  */

  public void addSource(final File file) {

    addSource(file.toPath());

  }

  public void addSource(final Path path) {

    addSource(path.toString());

  }

  public void addSource(final String fileName) {

    addSource(new CompilableSource(fileName));

  }

  public CompilableSource addSource(final CompilableSource source) {

    sources.put(source.getFileName(), source);

    return source;
  }

  public boolean isSourcePresent(final String fileName) {

    return sources.containsKey(fileName);
  }

  public CompilableSource getSource(final Path path) {

    return getSource(path.toString());
  }

  public CompilableSource getSource(final String fileName) {

    return sources.get(fileName);
  }

  /**
   * Remove some source code from the work space. Maybe a file has been deleted or renamed.
   */
  public Optional<ErrorListener> removeSource(final Path path) {

    //use a reentrant lock around the sources.
    return removeSource(path.toString());
  }

  public Optional<ErrorListener> removeSource(final String fileName) {

    return Optional.ofNullable(sources.remove(fileName)).map(CompilableSource::getErrorListener);
  }

  public Collection<CompilableSource> getSources() {

    return sources.values();
  }
}
