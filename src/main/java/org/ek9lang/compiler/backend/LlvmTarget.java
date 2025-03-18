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
    return clangExecutableSupported;
  }

  private void checkLlvmAccess() {
    var path = System.getenv("PATH");

    var pathParts = path.split(File.pathSeparator);
    var clangExecutable = Arrays.stream(pathParts)
        .map(part -> part + File.separator + CLANG)
        .map(File::new)
        .filter(File::canExecute)
        .findFirst();

    if (clangExecutable.isEmpty()) {
      System.err.println("Could not find clang executable");
    }
    clangExecutable.ifPresent(executable -> {
      //listFile(executable);
      this.pathToClang = executable;
    });
  }

  /**
   * While not used, maybe useful if I have issues with clang and llvm again.
   */
  private void listFile(final File file) {

    String[] command = {"/bin/ls", "-l", file.getAbsolutePath()};
    try {
      var process = Runtime.getRuntime().exec(command);
      BufferedReader stdInput = new BufferedReader(new
          InputStreamReader(process.getInputStream()));

      String s;
      while ((s = stdInput.readLine()) != null) {
        System.err.println(s);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void checkLLvmVersionValid() {

    if (pathToClang == null) {
      return;
    }

    String[] command = {pathToClang.getAbsolutePath(), "--version"};
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

      while ((s = stdError.readLine()) != null) {
        System.err.println(s);
        clangExecutableSupported = false;
      }
    } catch (IOException e) {
      System.err.println("Error in checking LLVM Version: " + e.getMessage());
      clangExecutableSupported = false;
    }

  }
}
