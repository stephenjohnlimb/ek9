package org.ek9lang.compiler;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.ek9lang.compiler.common.ErrorListener;

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

  private final String sourceFileBaseDirectory;

  public Workspace() {
    sourceFileBaseDirectory = ".";
  }

  public Workspace(String sourceFileBaseDirectory) {
    this.sourceFileBaseDirectory = sourceFileBaseDirectory;
  }

  public void addSource(final File file) {

    addSource(file.toPath());

  }

  public void addSource(final Path path) {

    addSource(path.toString());

  }

  public void addSource(final String fileName) {

    addSource(new CompilableSource(this.sourceFileBaseDirectory, fileName));

  }

  public void addSource(final CompilableSource source) {

    if (!isSourcePresent(source.getFileName())) {
      sources.put(source.getFileName(), source);
    }

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

    return new ArrayList<>(sources.values());
  }

  public String getSourceFileBaseDirectory() {
    return sourceFileBaseDirectory;
  }
}
