package org.ek9lang.compiler.files;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.ek9lang.core.utils.Logger;

/**
 * Designed to represent one or more source files that are part of a workspace.
 * Needs to become thread safe. use synchronized for now.
 */
public class Workspace {
  //The maps of source code file to compliable source objects
  private final Map<String, CompilableSource> sources = new HashMap<>();

  /**
   * ReParses or loads and parses a source file.
   */
  public synchronized CompilableSource reParseSource(Path path) {
    return reParseSource(path.toString());
  }

  /**
   * Triggers the reparsing of the source file. Normally after an edit so errors can be checked.
   */
  public synchronized CompilableSource reParseSource(String uri) {
    Logger.error("parsing/reparsing [" + uri + "]");
    CompilableSource compilableSource;
    if (isSourcePresent(uri)) {
      compilableSource = getSource(uri);
    } else {
      compilableSource = new CompilableSource(uri);
      addSource(compilableSource);
    }
    compilableSource.prepareToParse().parse();

    Logger.error("Workspace has " + sources.size() + " source files");
    return compilableSource;
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

  public Workspace addSource(CompilableSource source) {
    sources.put(source.getFileName(), source);
    return this;
  }

  public boolean isSourcePresent(String fileName) {
    return sources.containsKey(fileName);
  }

  public CompilableSource getSource(Path path) {
    return sources.get(path.toString());
  }

  public CompilableSource getSource(String fileName) {
    return sources.get(fileName);
  }

  /**
   * Remove some source code from the work space. Maybe a file has been deleted or renamed.
   */
  public synchronized CompilableSource removeSource(Path path) {
    CompilableSource rtn = sources.remove(path.toString());
    //also remove from modules.
    //modules.remove(rtn);

    Logger.error("Workspace now has " + sources.size() + " source files");
    return rtn;
  }

  public Collection<CompilableSource> getSources() {
    return sources.values();
  }
}
