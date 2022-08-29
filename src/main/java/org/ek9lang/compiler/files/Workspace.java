package org.ek9lang.compiler.files;

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
   * Triggers the reparsing of the source file. Normally after an edit so errors can be checked.
   */
  public ErrorListener reParseSource(String uri) {
    //Consider a queue of requests per uri as in an interactive mode the same file
    //will be triggered for reparsing over and over again. We only need one request to be honoured!

    Logger.debug("parsing/reparsing [" + uri + "]");
    CompilableSource compilableSource;
    if (isSourcePresent(uri)) {
      compilableSource = getSource(uri);
    } else {
      compilableSource = new CompilableSource(uri);
      addSource(compilableSource);
    }
    compilableSource.prepareToParse().parse();
    return compilableSource.getErrorListener();
  }

  /*
  public synchronized Workspace addParsedModule(ParsedModule module) {
    modules.put(module.getCompilableSource(), module);
    return this;
  }

  public ParsedModule getParsedModule(CompilableSource source) {
    return modules.get(source);
  }

  public synchronized Workspace addIRModule(IRModule irModule) {
    irModules.put(irModule.getCompilableSource(), irModule);
    return this;
  }

  public IRModule getIRModule(CompilableSource source) {
    return irModules.get(source);
  }
  */

  public Workspace addSource(Path path) {
    return addSource(path.toString());
  }

  public Workspace addSource(String fileName) {
    return addSource(new CompilableSource(fileName));
  }

  public Workspace addSource(CompilableSource source) {
    sources.put(source.getFileName(), source);
    return this;
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
    return Optional.ofNullable(sources.remove(fileName))
        .map(CompilableSource::getErrorListener);
  }

  public Collection<CompilableSource> getSources() {
    return sources.values();
  }
}
