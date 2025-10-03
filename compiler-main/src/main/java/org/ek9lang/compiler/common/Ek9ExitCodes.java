package org.ek9lang.compiler.common;

/**
 * Defines all exit codes used by the EK9 compiler and generated code.
 * These exit codes form an internal protocol between the compiler and the
 * ek9 wrapper, which maps some codes to follow Unix conventions.
 * <p>
 * The ek9 wrapper (ek9.c) translates these internal codes:
 * - Exit code 0: Wrapper executes command from stdout, returns program's exit code
 * - Exit code 1: Wrapper maps to 0 (Unix success for operations with no program to run)
 * - Exit codes 2-10: Wrapper returns same code (various error conditions)
 * </p>
 */
public final class Ek9ExitCodes {

  /**
   * Exit code 0: Compiler prints command to run.
   * The wrapper executes that command and returns the program's exit code.
   */
  public static final int RUN_COMMAND_EXIT_CODE = 0;

  /**
   * Exit code 1: Operation successful, nothing to run.
   * Examples: -C compile only, -V version, -Gk generate keys, -PV print version.
   * The wrapper maps this to 0 (Unix success).
   */
  public static final int SUCCESS_EXIT_CODE = 1;

  /**
   * Exit code 2: Invalid command line parameters.
   * The command line parameters provided were not valid or could not be parsed.
   */
  public static final int BAD_COMMANDLINE_EXIT_CODE = 2;

  /**
   * Exit code 3: File processing error.
   * The specified file was not found, could not be read, or had missing content.
   */
  public static final int FILE_ISSUE_EXIT_CODE = 3;

  /**
   * Exit code 4: Invalid combination of parameters.
   * The parameters provided were individually valid but incompatible when used together.
   */
  public static final int BAD_COMMAND_COMBINATION_EXIT_CODE = 4;

  /**
   * Exit code 5: No programs found.
   * The EK9 file does not contain any programs that can be executed.
   */
  public static final int NO_PROGRAMS_EXIT_CODE = 5;

  /**
   * Exit code 6: Program not specified.
   * The EK9 file contains more than one program, and you must specify which to run.
   */
  public static final int PROGRAM_NOT_SPECIFIED_EXIT_CODE = 6;

  /**
   * Exit code 7: Language Server failed to start.
   * The EK9 compiler could not start in Language Server Protocol mode.
   */
  public static final int LANGUAGE_SERVER_NOT_STARTED_EXIT_CODE = 7;

  /**
   * Exit code 8: Compilation failed with errors.
   * The EK9 source code contains syntax errors, type errors, or other compilation issues.
   */
  public static final int COMPILATION_FAILED_EXIT_CODE = 8;

  /**
   * Exit code 9: Wrong number of program arguments.
   * The program requires a specific number of arguments, but a different number was provided.
   * This is detected during the generated code's argument validation.
   */
  public static final int PROGRAM_ARGUMENT_COUNT_MISMATCH_EXIT_CODE = 9;

  /**
   * Exit code 10: Cannot convert argument to required type.
   * A program argument could not be converted to the type required by the program's parameter.
   * This is detected during the generated code's argument conversion.
   */
  public static final int PROGRAM_ARGUMENT_TYPE_MISMATCH_EXIT_CODE = 10;

  private Ek9ExitCodes() {
    // Utility class, prevent instantiation
  }
}
