package org.ek9lang.compiler.backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import org.ek9lang.core.TargetArchitecture;

/**
 * Target for LLVM code output.
 * i.e. this Target would produce LLVM Code that can be
 * converted to an executable by the llvm software.
 */
public class LlvmTarget implements Target {
  private static final int MIN_LLVM_VERSION_REQUIRED = 18;
  private static final String CLANG = "clang";
  private File pathToClang;
  private boolean clangExecutableSupported;

  public LlvmTarget() {
    checkLlvmAccess();
    checkLLvmVersionValid();
  }


  @Override
  public TargetArchitecture getArchitecture() {

    return TargetArchitecture.LLVM;
  }

  @Override
  public boolean isSupported() {
    System.out.println("LLVMTarget isSupported: " + clangExecutableSupported);
    return clangExecutableSupported;
  }

  private void checkLlvmAccess() {
    var path = System.getenv("PATH");

    System.out.println("LLVMTarget path: " + path);
    var pathParts = path.split(File.pathSeparator);
    var clangExecutable = Arrays.stream(pathParts)
        .map(part -> part + File.separator + CLANG)
        .map(File::new)
        .findFirst();

    if (clangExecutable.isEmpty()) {
      System.out.println("Could not find clang executable");
    }
    clangExecutable.ifPresent(executable -> {
      System.out.println("Found clang executable");
      if (executable.canExecute()) {
        this.pathToClang = executable;
      } else {
        System.out.println("Cannot execute clang executable");
      }
    });
  }

  private void checkLLvmVersionValid() {
    if (pathToClang == null) {
      return;
    }

    String[] command = {pathToClang.getPath(), "--version"};
    try {
      var process = Runtime.getRuntime().exec(command);
      BufferedReader stdInput = new BufferedReader(new
          InputStreamReader(process.getInputStream()));

      BufferedReader stdError = new BufferedReader(new
          InputStreamReader(process.getErrorStream()));

      // Read the output from the command
      String s;
      while ((s = stdInput.readLine()) != null) {
        if (s.contains("version")) {
          //Now pick up everything after the word version (as the 'version').
          //i.e. we'd get something like: Homebrew clang version 19.1.7 and 19.1.7
          var justVersion = s.split("/*version ")[1];

          var majorMinorPatch = justVersion.split("\\.");

          var majorVersion = Integer.parseInt(majorMinorPatch[0]);
          if (majorVersion >= MIN_LLVM_VERSION_REQUIRED) {
            this.clangExecutableSupported = true;
          } else {
            System.err.printf("LLVM Version too low: %d required, but %d found on PATH",
                MIN_LLVM_VERSION_REQUIRED, majorVersion);
          }
        }
      }

      //There should be no errors for this.
      while (stdError.lines().findAny().isPresent()) {
        clangExecutableSupported = false;
      }
    } catch (IOException e) {
      System.err.println("Error in checking LLVM Version: " + e.getMessage());
      clangExecutableSupported = false;
    }

  }
}
