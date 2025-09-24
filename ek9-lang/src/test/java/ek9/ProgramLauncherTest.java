package ek9;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ProgramLauncher using the new testable interface.
 * Tests all scenarios including error handling without System.exit() crashes.
 */
class ProgramLauncherTest {

  private Map<String, ProgramMetadata> registry;

  @BeforeEach
  void setUp() {
    registry = createTestRegistry();
  }

  @Test
  void testLaunchWithValidParameterlessProgram() {
    final var args = new String[] {"-r", "test::HelloWorld"};
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify program was executed
    assertEquals("executeHelloWorld", interaction.lastMethodCalled);
    assertEquals(0, interaction.lastArgumentsPassed.length);

    // Verify no errors or exits
    assertTrue(interaction.errorMessages.isEmpty());
    assertTrue(interaction.exitCodes.isEmpty());
  }

  @Test
  void testLaunchWithValidSingleStringParameter() {
    final var args = new String[] {"-r", "test::HelloMessage", "Claude"};
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify program was executed with correct arguments
    assertEquals("executeHelloMessage", interaction.lastMethodCalled);
    assertEquals(1, interaction.lastArgumentsPassed.length);
    assertNotNull(interaction.lastArgumentsPassed[0]);

    // Verify no errors or exits
    assertTrue(interaction.errorMessages.isEmpty());
    assertTrue(interaction.exitCodes.isEmpty());
  }

  @Test
  void testLaunchWithValidIntegerParameter() {

    final var args = new String[] {"-r", "test::IntegerProgram", "42"};
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify program was executed
    assertEquals("executeIntegerProgram", interaction.lastMethodCalled);
    assertEquals(1, interaction.lastArgumentsPassed.length);

    // Verify no errors or exits
    assertTrue(interaction.errorMessages.isEmpty());
    assertTrue(interaction.exitCodes.isEmpty());
  }

  @Test
  void testLaunchWithNullRegistryThrowsException() {
    final var args = new String[] {"-r", "test::HelloWorld"};
    final var interaction = TestMain.getInteraction();
    assertThrows(NullPointerException.class, () ->
        ProgramLauncher.launch(null, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain"));
  }

  @Test
  void testLaunchWithNullArgsThrowsException() {
    final var interaction = TestMain.getInteraction();
    assertThrows(NullPointerException.class, () ->
        ProgramLauncher.launch(registry, null, interaction.errorMessages::add, interaction.exitCodes::add,
            "ek9.TestMain"));
  }

  @Test
  void testInvalidCommandLineFormatMissingRFlag() {
    final var args = new String[] {"test::HelloWorld"};

    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify usage error
    assertTrue(
        interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Usage: java ek9.Main -r <program-name>")));
    assertEquals(List.of(2), interaction.exitCodes);
  }

  @Test
  void testInvalidCommandLineFormatTooFewArgs() {
    final var args = new String[] {"-r"};
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify usage error
    assertTrue(
        interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Usage: java ek9.Main -r <program-name>")));
    assertEquals(List.of(2), interaction.exitCodes);
  }

  @Test
  void testProgramNotFoundError() {
    final var args = new String[] {"-r", "nonexistent::Program"};
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify error messages
    assertTrue(
        interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Program 'nonexistent::Program' not found")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Available programs:")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("HelloWorld()")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("HelloMessage(arg0: String)")));
    assertEquals(List.of(1), interaction.exitCodes);
  }

  @Test
  void testArgumentCountMismatchTooFew() {
    final var args = new String[] {"-r", "test::HelloMessage"}; // Missing required argument
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify error messages
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Argument count mismatch")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Expected 1 arguments, got 0")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Program signature:")));
    assertEquals(List.of(1), interaction.exitCodes);
  }

  @Test
  void testArgumentCountMismatchTooMany() {
    final var args = new String[] {"-r", "test::HelloWorld", "extra"}; // Too many arguments
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify error messages
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Argument count mismatch")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Expected 0 arguments, got 1")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("This program takes no arguments")));
    assertEquals(List.of(1), interaction.exitCodes);
  }

  @Test
  void testTypeConversionErrorWithInteger() {
    final var args = new String[] {"-r", "test::IntegerProgram", "not_an_integer"};
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify conversion error messages
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Argument conversion failed")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Invalid integer for argument 1")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("not_an_integer")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Expected a whole number")));
    assertEquals(List.of(1), interaction.exitCodes);
  }

  @Test
  void testTypeConversionErrorWithBoolean() {

    final var args = new String[] {"-r", "test::BooleanProgram", "maybe"};
    final var interaction = TestMain.getInteraction();

    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify conversion error messages
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Argument conversion failed")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Invalid boolean for argument 1")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("maybe")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Expected 'true' or 'false'")));
    assertEquals(List.of(1), interaction.exitCodes);
  }

  @Test
  void testMultipleParameterProgram() {

    final var args = new String[] {"-r", "test::MultiParamProgram", "Alice", "25"};
    final var interaction = TestMain.getInteraction();

    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify program was executed
    assertEquals("executeMultiParamProgram", interaction.lastMethodCalled);
    assertEquals(2, interaction.lastArgumentsPassed.length);

    // Verify no errors or exits
    assertTrue(interaction.errorMessages.isEmpty());
    assertTrue(interaction.exitCodes.isEmpty());
  }

  @Test
  void testClassNotFoundError() {
    final var args = new String[] {"-r", "test::HelloWorld"};
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add,
        "nonexistent.ClassName");

    // Verify execution error
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Program execution failed")));
    assertEquals(List.of(1), interaction.exitCodes);
  }

  @Test
  void testMethodNotFoundError() {

    final var args = new String[] {"-r", "test::InvalidProgram"};
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify execution error
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Program execution failed")));
    assertEquals(List.of(1), interaction.exitCodes);
  }

  @Test
  void testCreateConversionErrorMessageForAllTypes() {
    // Test error messages for all 17 EK9 types by triggering conversion failures

    // Integer type error
    testTypeConversionError("org.ek9.lang::Integer", "not_a_number",
        "Invalid integer for argument 1: 'not_a_number'. Expected a whole number like '42' or '-10'.");

    // Boolean type error
    testTypeConversionError("org.ek9.lang::Boolean", "maybe",
        "Invalid boolean for argument 1: 'maybe'. Expected 'true' or 'false'.");

    // Float type error
    testTypeConversionError("org.ek9.lang::Float", "not_a_float",
        "Invalid float for argument 1: 'not_a_float'. Expected a decimal number like '3.14' or '2.0'.");

    // Character type error
    testTypeConversionError("org.ek9.lang::Character", "",
        "Invalid character for argument 1: ''. Expected any non-empty string like 'a', 'Z', or '123'.");

    // Bits type error
    testTypeConversionError("org.ek9.lang::Bits", "invalid_bits",
        "Invalid bits for argument 1: 'invalid_bits'. Expected binary format like '0b01010101' or '0b1100'.");

    // Date type error
    testTypeConversionError("org.ek9.lang::Date", "invalid_date",
        "Invalid date for argument 1: 'invalid_date'. Expected date format like '2020-10-03' or '2023-12-31'.");

    // DateTime type error
    testTypeConversionError("org.ek9.lang::DateTime", "invalid_datetime",
        "Invalid datetime for argument 1: 'invalid_datetime'. Expected datetime format like '2020-10-03T12:00:00Z' or '2020-10-04T12:15:00-05:00'.");

    // Time type error
    testTypeConversionError("org.ek9.lang::Time", "invalid_time",
        "Invalid time for argument 1: 'invalid_time'. Expected time format like '12:00:01' or '09:15:30'.");

    // Duration type error
    testTypeConversionError("org.ek9.lang::Duration", "invalid_duration",
        "Invalid duration for argument 1: 'invalid_duration'. Expected duration format like 'P1Y1M4D', 'PT2H30M', or 'P1DT2H30M'.");

    // Millisecond type error
    testTypeConversionError("org.ek9.lang::Millisecond", "invalid_ms",
        "Invalid millisecond for argument 1: 'invalid_ms'. Expected millisecond format like '100ms' or '250ms'.");

    // Dimension type error
    testTypeConversionError("org.ek9.lang::Dimension", "invalid_dimension",
        "Invalid dimension for argument 1: 'invalid_dimension'. Expected dimension format like '1cm', '10px', or '4.5em'.");

    // Resolution type error
    testTypeConversionError("org.ek9.lang::Resolution", "invalid_resolution",
        "Invalid resolution for argument 1: 'invalid_resolution'. Expected resolution format like '1920x1080' or '800x600'.");

    // Colour type error
    testTypeConversionError("org.ek9.lang::Colour", "invalid_color",
        "Invalid colour for argument 1: 'invalid_color'. Expected color format like '#FF186276', '#000000', or '#FFFFFF'.");

    // Money type error
    testTypeConversionError("org.ek9.lang::Money", "invalid_money",
        "Invalid money for argument 1: 'invalid_money'. Expected money format like '10#GBP', '30.89#USD', or '6798.9288#CLF'.");


    // NOTE: List._of() doesn't fail conversion - it creates an empty list regardless of input
    // So we skip testing this case as it doesn't produce conversion errors

    // Test unsupported type instead which triggers the default case in createConversionErrorMessage
    testUnsupportedTypeConversionError(
    );
  }

  private void testTypeConversionError(final String typeName, final String invalidValue, final String expectedMessage) {

    // Create a program that requires the specific type
    final var testProgram = new ProgramMetadata(
        "test::TypeTestProgram",
        new String[] {typeName},
        "executeTypeTestProgram"
    );
    registry.put("test::TypeTestProgram", testProgram);

    final var args = new String[] {"-r", "test::TypeTestProgram", invalidValue};
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify conversion error message
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Argument conversion failed")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains(expectedMessage)),
        "Expected message containing: " + expectedMessage + " but got: " + interaction.errorMessages);
    assertEquals(List.of(1), interaction.exitCodes);

    // Clean up for next test
    registry.remove("test::TypeTestProgram");
  }

  private void testUnsupportedTypeConversionError() {

    // Create a program that requires the unsupported type

    final var args = new String[] {"-r", "test::UnsupportedTypeProgram", "any_value"};
    final var interaction = TestMain.getInteraction();
    ProgramLauncher.launch(registry, args, interaction.errorMessages::add, interaction.exitCodes::add, "ek9.TestMain");

    // Verify conversion error message
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains("Argument conversion failed")));
    assertTrue(interaction.errorMessages.stream().anyMatch(msg -> msg.contains(
            "Unsupported type conversion: org.ek9.lang::UnsupportedType")),
        "Expected message containing: " + "Unsupported type conversion: org.ek9.lang::UnsupportedType" + " but got: " + interaction.errorMessages);
    assertEquals(List.of(1), interaction.exitCodes);

  }

  private Map<String, ProgramMetadata> createTestRegistry() {
    final var newRegistry = new HashMap<String, ProgramMetadata>();

    final var booleanProgram = new ProgramMetadata(
        "test::BooleanProgram",
        new String[] {"org.ek9.lang::Boolean"},
        "executeBooleanProgram"
    );
    newRegistry.put("test::BooleanProgram", booleanProgram);

    final var integerProgram = new ProgramMetadata(
        "test::IntegerProgram",
        new String[] {"org.ek9.lang::Integer"},
        "executeIntegerProgram"
    );
    newRegistry.put("test::IntegerProgram", integerProgram);

    // Parameterless program
    newRegistry.put("test::HelloWorld", new ProgramMetadata(
        "test::HelloWorld",
        new String[0],
        "executeHelloWorld"
    ));

    // Program with one String parameter
    newRegistry.put("test::HelloMessage", new ProgramMetadata(
        "test::HelloMessage",
        new String[] {"org.ek9.lang::String"},
        "executeHelloMessage"
    ));

    final var multiParamProgram = new ProgramMetadata(
        "test::MultiParamProgram",
        new String[] {"org.ek9.lang::String", "org.ek9.lang::Integer"},
        "executeMultiParamProgram"
    );
    newRegistry.put("test::MultiParamProgram", multiParamProgram);

    final var invalidProgram = new ProgramMetadata(
        "test::InvalidProgram",
        new String[0],
        "nonExistentMethod"
    );
    newRegistry.put("test::InvalidProgram", invalidProgram);

    final var testProgram = new ProgramMetadata(
        "test::UnsupportedTypeProgram",
        new String[] {"org.ek9.lang::UnsupportedType"},
        "executeUnsupportedTypeProgram"
    );
    newRegistry.put("test::UnsupportedTypeProgram", testProgram);

    return newRegistry;
  }
}