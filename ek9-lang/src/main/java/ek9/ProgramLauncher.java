package ek9;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Launches EK9 programs by parsing command-line arguments and dispatching to the appropriate program method.
 * Handles program selection via -r flag, argument validation, type conversion, and execution.
 */
public final class ProgramLauncher {

  private ProgramLauncher() {
    //To enforce static method use only.
  }

  /**
   * Launch an EK9 program from command-line arguments (testable version).
   * Allows dependency injection for error output, exit handling, and main class name.
   *
   * @param registry Program registry mapping qualified names to metadata
   * @param args     Command-line arguments: [-r, programName, userArg1, userArg2, ...]
   * @param errorOutput Consumer for error messages (System.err::println in production)
   * @param exitHandler Consumer for exit codes (System::exit in production)
   * @param mainClassName Name of main class to execute ("ek9.Main" in production)
   */
  public static void launch(final Map<String, ProgramMetadata> registry,
                           final String[] args,
                           final Consumer<String> errorOutput,
                           final IntConsumer exitHandler,
                           final String mainClassName) {
    Objects.requireNonNull(registry, "Program registry cannot be null");
    Objects.requireNonNull(args, "Arguments cannot be null");
    Objects.requireNonNull(mainClassName, "MainClassName cannot be null");

    try {
      // Parse command-line arguments
      final var parsedArgs = parseArguments(args, errorOutput, exitHandler);
      if (parsedArgs == null) {
        return; // Parsing failed, error already handled
      }

      // Look up program in registry
      final var program = registry.get(parsedArgs.programName);
      if (program == null) {
        printProgramNotFound(parsedArgs.programName, registry, errorOutput);
        exitHandler.accept(1);
        return;
      }

      // Validate argument count
      if (parsedArgs.userArgs.length != program.getParameterCount()) {
        printArgumentCountMismatch(program, parsedArgs.userArgs.length, errorOutput);
        exitHandler.accept(1);
        return;
      }

      // Convert arguments to EK9 types
      final var typeConverter = new TypeConverter();
      final var convertedArgs = convertArguments(parsedArgs.userArgs, program, typeConverter);

      // Execute the program
      executeProgram(program, convertedArgs, mainClassName);

    } catch (TypeConverter.TypeConversionException e) {
      errorOutput.accept("Argument conversion failed: " + e.getMessage());
      exitHandler.accept(1);
    } catch (Exception e) {
      errorOutput.accept("Program execution failed: " + e.getMessage());
      exitHandler.accept(1);
    }
  }

  /**
   * Parse command-line arguments to extract program name and user arguments.
   * Returns null if parsing fails (after calling error handlers).
   */
  private static ParsedArguments parseArguments(final String[] args,
                                               final Consumer<String> errorOutput,
                                               final IntConsumer exitHandler) {
    if (args.length < 2 || !"-r".equals(args[0])) {
      printUsage(errorOutput, exitHandler);
      return null; // Parsing failed
    }

    final var programName = args[1];
    final var userArgs = Arrays.copyOfRange(args, 2, args.length);

    return new ParsedArguments(programName, userArgs);
  }

  /**
   * Convert user string arguments to EK9 types using the program's parameter signature.
   * Validates that all conversions succeed and provides helpful error messages.
   */
  private static Object[] convertArguments(final String[] userArgs,
                                          final ProgramMetadata program,
                                          final TypeConverter typeConverter) {
    final var parameterTypes = program.getParameterTypes();
    final var convertedArgs = new Object[userArgs.length];

    for (int i = 0; i < userArgs.length; i++) {
      final var userValue = userArgs[i];
      final var expectedType = parameterTypes.get(i);

      // Convert using EK9's tri-state semantics
      final var convertedArg = typeConverter.convertToEK9Type(userValue, expectedType);

      // Check if conversion succeeded (result is set)
      if (!isEK9ObjectSet(convertedArg)) {
        throw new TypeConverter.TypeConversionException(
            createConversionErrorMessage(userValue, expectedType, i, typeConverter)
        );
      }

      convertedArgs[i] = convertedArg;
    }

    return convertedArgs;
  }

  /**
   * Check if an EK9 object is set (conversion succeeded).
   */
  private static boolean isEK9ObjectSet(final Object ek9Object) {
    if (ek9Object instanceof org.ek9.lang.BuiltinType builtinType) {
      return builtinType._isSet()._true();
    }
    return ek9Object != null;
  }

  /**
   * Create a helpful error message for type conversion failures.
   */
  private static String createConversionErrorMessage(final String userValue,
                                                     final String expectedType,
                                                     final int position,
                                                     final TypeConverter typeConverter) {
    final var simpleType = typeConverter.getSimpleTypeName(expectedType);
    final var message = new StringBuilder();

    message.append("Invalid ").append(simpleType.toLowerCase())
           .append(" for argument ").append(position + 1)
           .append(": '").append(userValue).append("'. ");

    switch (expectedType) {
      case "org.ek9.lang::Integer":
        message.append("Expected a whole number like '42' or '-10'.");
        break;
      case "org.ek9.lang::Boolean":
        message.append("Expected 'true' or 'false'.");
        break;
      case "org.ek9.lang::Float":
        message.append("Expected a decimal number like '3.14' or '2.0'.");
        break;
      case "org.ek9.lang::Character":
        message.append("Expected any non-empty string like 'a', 'Z', or '123'.");
        break;
      case "org.ek9.lang::Bits":
        message.append("Expected binary format like '0b01010101' or '0b1100'.");
        break;
      case "org.ek9.lang::Date":
        message.append("Expected date format like '2020-10-03' or '2023-12-31'.");
        break;
      case "org.ek9.lang::DateTime":
        message.append("Expected datetime format like '2020-10-03T12:00:00Z' or '2020-10-04T12:15:00-05:00'.");
        break;
      case "org.ek9.lang::Time":
        message.append("Expected time format like '12:00:01' or '09:15:30'.");
        break;
      case "org.ek9.lang::Duration":
        message.append("Expected duration format like 'P1Y1M4D', 'PT2H30M', or 'P1DT2H30M'.");
        break;
      case "org.ek9.lang::Millisecond":
        message.append("Expected millisecond format like '100ms' or '250ms'.");
        break;
      case "org.ek9.lang::Dimension":
        message.append("Expected dimension format like '1cm', '10px', or '4.5em'.");
        break;
      case "org.ek9.lang::Resolution":
        message.append("Expected resolution format like '1920x1080' or '800x600'.");
        break;
      case "org.ek9.lang::Colour":
        message.append("Expected color format like '#FF186276', '#000000', or '#FFFFFF'.");
        break;
      case "org.ek9.lang::Money":
        message.append("Expected money format like '10#GBP', '30.89#USD', or '6798.9288#CLF'.");
        break;
      case "org.ek9.lang::RegEx":
        message.append("Expected regex pattern like '/[a-zA-Z0-9]{6}/', '/[S|s]te(?:ven?|phen)/', or '/.*\\/.*/'.");
        break;
      case "org.ek9.lang::_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1":
        message.append("Expected multiple string arguments for List<String>.");
        break;
      default:
        message.append("Expected a valid ").append(simpleType).append(" value.");
        break;
    }

    return message.toString();
  }

  /**
   * Execute the program by calling its method via reflection.
   * This is a simplified implementation - the real version will be generated by ASM.
   */
  private static void executeProgram(final ProgramMetadata program,
                                    final Object[] convertedArgs,
                                    final String mainClassName) {
    try {
      // This is a placeholder - in the real implementation, the generated ek9.Main class
      // will have direct method calls instead of reflection
      final var mainClass = Class.forName(mainClassName);
      final var methodName = program.getMethodName();

      // Find method by name and parameter count
      Method targetMethod = null;
      for (final var method : mainClass.getDeclaredMethods()) {
        if (method.getName().equals(methodName) && method.getParameterCount() == convertedArgs.length) {
          targetMethod = method;
          break;
        }
      }

      if (targetMethod == null) {
        throw new RuntimeException("Program method not found: " + methodName);
      }

      // Invoke the program method
      targetMethod.invoke(null, convertedArgs);

    } catch (Exception e) {
      throw new RuntimeException("Failed to execute program: " + program.getQualifiedName(), e);
    }
  }

  /**
   * Print usage information and exit.
   */
  private static void printUsage(final Consumer<String> errorOutput, final IntConsumer exitHandler) {
    errorOutput.accept("Usage: java ek9.Main -r <program-name> [arguments...]");
    errorOutput.accept("");
    errorOutput.accept("Options:");
    errorOutput.accept("  -r <program-name>  Select the program to run");
    errorOutput.accept("");
    errorOutput.accept("Use -r followed by a program name to select which program to execute.");
    errorOutput.accept("Run without arguments to see available programs.");
    exitHandler.accept(2);
  }

  /**
   * Print error when requested program is not found.
   */
  private static void printProgramNotFound(final String programName,
                                          final Map<String, ProgramMetadata> registry,
                                          final Consumer<String> errorOutput) {
    errorOutput.accept("Program '" + programName + "' not found.");
    errorOutput.accept("");
    if (registry.isEmpty()) {
      errorOutput.accept("No programs are available.");
    } else {
      errorOutput.accept("Available programs:");
      registry.values().stream()
          .sorted(Comparator.comparing(ProgramMetadata::getQualifiedName))
          .forEach(program -> errorOutput.accept("  " + program.getSignature()));
    }
  }

  /**
   * Print error when argument count doesn't match program signature.
   */
  private static void printArgumentCountMismatch(final ProgramMetadata program,
                                                final int providedCount,
                                                final Consumer<String> errorOutput) {
    errorOutput.accept("Argument count mismatch for program: " + program.getQualifiedName());
    errorOutput.accept("Expected " + program.getParameterCount() + " arguments, got " + providedCount);
    errorOutput.accept("");
    errorOutput.accept("Program signature: " + program.getSignature());
    errorOutput.accept("");

    if (program.hasParameters()) {
      errorOutput.accept("Expected arguments:");
      final var parameterTypes = program.getParameterTypes();
      final var typeConverter = new TypeConverter(); // Create instance for error display
      for (int i = 0; i < parameterTypes.size(); i++) {
        errorOutput.accept("  " + (i + 1) + ". " + typeConverter.getSimpleTypeName(parameterTypes.get(i)));
      }
    } else {
      errorOutput.accept("This program takes no arguments.");
    }
  }

  /**
   * Parsed command-line arguments.
   */
  private static class ParsedArguments {
    final String programName;
    final String[] userArgs;

    ParsedArguments(final String programName, final String[] userArgs) {
      this.programName = programName;
      this.userArgs = userArgs;
    }
  }
}