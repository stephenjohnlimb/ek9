package org.ek9lang.compiler.backend.jvm;

import java.io.File;
import java.util.function.BiFunction;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ir.Construct;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.TargetArchitecture;

/**
 * Creates the File Object in the appropriate location for jvm output.
 */
public final class OutputFileCreator implements BiFunction<Construct, String, File> {

  private final FileHandling fileHandling;
  private final CompilerFlags compilerFlags;
  private final FullyQualifiedFileName fullyQualifiedNameMapper = new FullyQualifiedFileName();

  public OutputFileCreator(final FileHandling fileHandling, final CompilerFlags compilerFlags) {
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
    final var jvmFQN = fullyQualifiedNameMapper.apply(construct.getFullyQualifiedName());
    final File rtn = new File(dir, jvmFQN);
    if(!rtn.exists()) {
      fileHandling.makeDirectoryIfNotExists(rtn.getParentFile());
      fileHandling.createOrRecreateFile(rtn);
    }
    return rtn;
  }
}
