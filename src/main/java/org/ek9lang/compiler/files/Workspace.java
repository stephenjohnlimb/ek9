package org.ek9lang.compiler.files;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.core.utils.Logger;

/**
 * Designed to represent one or more source files that are part of a workspace.
 * Needs to become thread safe, especially around re-parsing.
 * This is because when a user is typing via lsp mode - this will get triggered over and over.
 */
public class Workspace {
  //The maps of source code file to compilable source objects

  //Maybe put a re-entrant lock around this.
  private final Map<String, CompilableSource> sources = new HashMap<>();

  /**
   * ReParses or loads and parses a source file.
   */
  public ErrorListener reParseSource(Path path) {
    return reParseSource(path.toString());
  }

  /**
   * Typically used via the language server.
   */
  public ErrorListener reParseSource(String uri, InputStream inputStream) {
    Logger.debug("parsing/reparsing [" + uri + "] but with direct inputstream");
    CompilableSource compilableSource = ensureCompilableSourceAvailable(uri);
    compilableSource.prepareToParse(inputStream).parse();
    return compilableSource.getErrorListener();
  }

  /**
   * Triggers the reparsing of the source file. Normally after an edit so errors can be checked.
   */
  public ErrorListener reParseSource(String uri) {
    //Consider a queue of requests per uri as in an interactive mode the same file
    //will be triggered for reparsing over and over again. We only need one request to be honoured!

    Logger.debug("parsing/reparsing [" + uri + "]");
    CompilableSource compilableSource = ensureCompilableSourceAvailable(uri);
    compilableSource.prepareToParse().parse();
    return compilableSource.getErrorListener();
  }

  private CompilableSource ensureCompilableSourceAvailable(String uri) {
    if (isSourcePresent(uri)) {
      return getSource(uri);
    }

    return addSource(new CompilableSource(uri));
  }

  /*
  ParsedModule and IRModule to be added in
  */

  public CompilableSource addSource(Path path) {
    return addSource(path.toString());
  }

  public CompilableSource addSource(String fileName) {
    return addSource(new CompilableSource(fileName));
  }

  public CompilableSource addSource(CompilableSource source) {
    sources.put(source.getFileName(), source);
    return source;
  }

  public boolean isSourcePresent(String fileName) {
    return sources.containsKey(fileName);
  }

  public CompilableSource getSource(Path path) {
    return getSource(path.toString());
  }

  public CompilableSource getSource(String fileName) {
    return sources.get(fileName);
  }

  /**
   * Remove some source code from the work space. Maybe a file has been deleted or renamed.
   */
  public Optional<ErrorListener> removeSource(Path path) {
    //use a reentrant lock around the sources.
    return removeSource(path.toString());
  }

  public Optional<ErrorListener> removeSource(String fileName) {
    return Optional.ofNullable(sources.remove(fileName)).map(CompilableSource::getErrorListener);
  }

  public Collection<CompilableSource> getSources() {
    return sources.values();
  }
}
