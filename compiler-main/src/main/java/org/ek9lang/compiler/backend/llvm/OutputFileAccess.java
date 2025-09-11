package org.ek9lang.compiler.backend.llvm;

import java.io.File;
import java.util.function.BiFunction;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.TargetArchitecture;

/**
 * Creates the File Object in the appropriate location for llvm output.
 */
public final class OutputFileAccess implements BiFunction<IRConstruct, String, File> {

  private final FileHandling fileHandling;
  private final CompilerFlags compilerFlags;
  private final FullyQualifiedFileName fullyQualifiedNameMapper = new FullyQualifiedFileName();
  private final TargetArchitecture llvmTarget;

  public OutputFileAccess(final FileHandling fileHandling, final CompilerFlags compilerFlags,
                          final TargetArchitecture llvmTarget) {
    this.fileHandling = fileHandling;
    this.compilerFlags = compilerFlags;
    this.llvmTarget = llvmTarget;
  }

  @Override
  public File apply(final IRConstruct construct, final String projectDotEK9Directory) {
    if (compilerFlags.isDevBuild()) {
      final File dir = fileHandling.getDevGeneratedOutputDirectory(projectDotEK9Directory, llvmTarget);
      return (createOutputFile(dir, construct));
    }
    final File dir = fileHandling.getMainGeneratedOutputDirectory(projectDotEK9Directory, llvmTarget);
    return (createOutputFile(dir, construct));

  }

  private File createOutputFile(final File dir, final IRConstruct construct) {
    final var fqn = fullyQualifiedNameMapper.apply(construct.getFullyQualifiedName());
    final File rtn = new File(dir, fqn);
    fileHandling.makeDirectoryIfNotExists(rtn.getParentFile());
    return rtn;
  }
}
