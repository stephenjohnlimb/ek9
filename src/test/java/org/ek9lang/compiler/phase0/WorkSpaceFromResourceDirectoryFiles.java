package org.ek9lang.compiler.phase0;

import java.util.function.Function;
import org.ek9lang.compiler.Workspace;

/**
 * Create a loaded workspace with ek9 source files from a resource directory.
 */
public class WorkSpaceFromResourceDirectoryFiles implements Function<String, Workspace> {
  private final SourceFileList sourceFileList = new SourceFileList();

  @Override
  public Workspace apply(String fromDirectory) {
    Workspace rtn = new Workspace();
    sourceFileList.apply(fromDirectory).forEach(rtn::addSource);
    return rtn;
  }
}
