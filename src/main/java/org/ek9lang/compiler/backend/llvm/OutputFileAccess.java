package org.ek9lang.compiler.backend.llvm;

import java.io.File;
import java.util.function.BiFunction;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ir.Construct;
import org.ek9lang.core.FileHandling;

/**
 * Creates the File Object in the appropriate location for llvm output.
 */
public final class OutputFileAccess implements BiFunction<Construct, String, File> {

  private final FileHandling fileHandling;
  private final CompilerFlags compilerFlags;

  public OutputFileAccess(final FileHandling fileHandling, final CompilerFlags compilerFlags) {
    this.fileHandling = fileHandling;
    this.compilerFlags = compilerFlags;
  }


  @Override
  public File apply(final Construct construct, final String projectDotEK9Directory) {
    //TODO
    return null;
  }
}
