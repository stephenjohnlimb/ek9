package org.ek9lang.compiler.common;

import java.util.function.Function;
import org.ek9lang.compiler.Workspace;

/**
 * Create a loaded workspace with ek9 source files from a resource directory.
 */
public final class WorkSpaceFromResourceDirectoryFiles implements Function<String, Workspace> {
  private final SourceFileList sourceFileList = new SourceFileList();

  private final ActualPathFromResourcesDirectory actualPathFromResourcesDirectory =
      new ActualPathFromResourcesDirectory();

  @Override
  public Workspace apply(String fromDirectory) {
    Workspace rtn = new Workspace(actualPathFromResourcesDirectory.apply(fromDirectory));
    sourceFileList.apply(fromDirectory).forEach(rtn::addSource);
    return rtn;
  }
}
