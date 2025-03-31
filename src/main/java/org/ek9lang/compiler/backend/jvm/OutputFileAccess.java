package org.ek9lang.compiler.backend.jvm;

import java.io.File;
import java.util.function.BiFunction;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ir.Construct;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.TargetArchitecture;

/**
 * Returns File Object in the appropriate location for jvm output.
 * It may not actually exist yet, but the path to it will.
 */
public final class OutputFileAccess implements BiFunction<Construct, String, File> {

  private final FileHandling fileHandling;
  private final CompilerFlags compilerFlags;
  private final FullyQualifiedFileName fullyQualifiedNameMapper = new FullyQualifiedFileName();

  public OutputFileAccess(final FileHandling fileHandling, final CompilerFlags compilerFlags) {
    this.fileHandling = fileHandling;
    this.compilerFlags = compilerFlags;
  }


  @Override
  public File apply(final Construct construct, final String projectDotEK9Directory) {
    if (compilerFlags.isDevBuild()) {
      final File dir = fileHandling.getDevGeneratedOutputDirectory(projectDotEK9Directory, TargetArchitecture.JVM);
      return (createOutputFile(dir, construct));
    }
    final File dir = fileHandling.getMainGeneratedOutputDirectory(projectDotEK9Directory, TargetArchitecture.JVM);
    return (createOutputFile(dir, construct));

  }

  private File createOutputFile(final File dir, final Construct construct) {
    final var fqn = fullyQualifiedNameMapper.apply(construct.getFullyQualifiedName());
    final File rtn = new File(dir, fqn);
    fileHandling.makeDirectoryIfNotExists(rtn.getParentFile());
    return rtn;
  }
}
