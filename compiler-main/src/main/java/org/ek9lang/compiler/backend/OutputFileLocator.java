package org.ek9lang.compiler.backend;

import static org.ek9lang.core.TargetArchitecture.LLVM_CPP;
import static org.ek9lang.core.TargetArchitecture.LLVM_GO;

import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ir.Construct;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.FileHandling;

/**
 * Given a TargetArchitecture from the configFlags, this function will provide a function that creates (if not present)
 * and returns the File for a Construct.
 */
public class OutputFileLocator implements Supplier<BiFunction<Construct, String, File>> {

  private final FileHandling fileHandling;
  private final CompilerFlags compilerFlags;

  public OutputFileLocator(final FileHandling fileHandling, final CompilerFlags compilerFlags) {
    this.fileHandling = fileHandling;
    this.compilerFlags = compilerFlags;
  }

  @Override
  public BiFunction<Construct, String, File> get() {

    return switch (compilerFlags.getTargetArchitecture()) {
      case LLVM_GO -> new org.ek9lang.compiler.backend.llvm.OutputFileAccess(fileHandling, compilerFlags, LLVM_GO);
      case LLVM_CPP -> new org.ek9lang.compiler.backend.llvm.OutputFileAccess(fileHandling, compilerFlags, LLVM_CPP);
      case JVM -> new org.ek9lang.compiler.backend.jvm.OutputFileAccess(fileHandling, compilerFlags);
      case NOT_SUPPORTED -> throw new CompilerException(
          "Target architecture " + compilerFlags.getTargetArchitecture() + " is not supported");
    };
  }


}
