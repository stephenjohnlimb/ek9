package ek9;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Integration tests demonstrating the complete workflow of runtime utilities.
 * These tests show how the components work together and validate the interfaces
 * that the generated ek9.Main class will use.
 */
class ProgramLauncherIntegrationTest {

  private final TypeConverter typeConverter = new TypeConverter();

  @Test
  void testCompleteWorkflowWithParameterlessProgram() {
    // Create a program registry as the generated ek9.Main would
    final var registry = createSampleRegistry();

    // Validate that we can look up programs correctly
    final var helloWorld = registry.get("introduction1::HelloWorld");
    assertNotNull(helloWorld);
    assertEquals("introduction1::HelloWorld", helloWorld.getQualifiedName());
    assertEquals(0, helloWorld.getParameterCount());
    assertEquals("executeHelloWorld", helloWorld.getMethodName());

    // Test signature generation for user help
    assertEquals("HelloWorld()", helloWorld.getSignature());
  }

  @Test
  void testCompleteWorkflowWithParameterizedProgram() {
    final var registry = createSampleRegistry();

    // Test program with parameters
    final var helloMessage = registry.get("introduction1::HelloMessage");
    assertNotNull(helloMessage);
    assertEquals("introduction1::HelloMessage", helloMessage.getQualifiedName());
    assertEquals(1, helloMessage.getParameterCount());
    assertTrue(helloMessage.hasParameters());
    assertEquals("executeHelloMessage", helloMessage.getMethodName());

    // Test signature generation
    assertEquals("HelloMessage(arg0: String)", helloMessage.getSignature());

    // Test parameter type information
    final var paramTypes = helloMessage.getParameterTypes();
    assertEquals(1, paramTypes.size());
    assertEquals("org.ek9.lang::String", paramTypes.getFirst());
  }

  @Test
  void testArgumentConversionWorkflow() {
    // Test the complete argument conversion workflow
    final var userArgs = new String[] {"Hello, World!", "42", "true", "3.14"};
    final var expectedTypes = new String[] {
        "org.ek9.lang::String",
        "org.ek9.lang::Integer",
        "org.ek9.lang::Boolean",
        "org.ek9.lang::Float"
    };

    // Convert each argument and validate results
    for (int i = 0; i < userArgs.length; i++) {
      final var result = typeConverter.convertToEK9Type(userArgs[i], expectedTypes[i]);
      assertNotNull(result);

      // Verify it's the correct type and is set
      switch (expectedTypes[i]) {
        case "org.ek9.lang::String":
          assertInstanceOf(org.ek9.lang.String.class, result);
          assertTrue(((org.ek9.lang.String) result)._isSet()._true());
          break;
        case "org.ek9.lang::Integer":
          assertInstanceOf(org.ek9.lang.Integer.class, result);
          assertTrue(((org.ek9.lang.Integer) result)._isSet()._true());
          break;
        case "org.ek9.lang::Boolean":
          assertInstanceOf(org.ek9.lang.Boolean.class, result);
          assertTrue(((org.ek9.lang.Boolean) result)._isSet()._true());
          break;
        case "org.ek9.lang::Float":
          assertInstanceOf(org.ek9.lang.Float.class, result);
          assertTrue(((org.ek9.lang.Float) result)._isSet()._true());
          break;
        default:
          fail("Not expecting this type " + expectedTypes[i]);
      }
    }
  }

  @Test
  void testRegistryCreationPattern() {
    // Demonstrate the pattern that generated ek9.Main will use
    final var registry = new HashMap<String, ProgramMetadata>();

    // Add programs as they would be generated from PROGRAM_ENTRY_POINT_BLOCK
    addProgramToRegistry(registry, "introduction1::HelloWorld", new String[0], "executeHelloWorld");
    addProgramToRegistry(registry, "introduction1::HelloMessage",
        new String[] {"org.ek9.lang::String"}, "executeHelloMessage");
    addProgramToRegistry(registry, "test::Calculator",
        new String[] {"org.ek9.lang::Integer", "org.ek9.lang::Integer"}, "executeCalculator");

    // Validate registry contents
    assertEquals(3, registry.size());
    assertTrue(registry.containsKey("introduction1::HelloWorld"));
    assertTrue(registry.containsKey("introduction1::HelloMessage"));
    assertTrue(registry.containsKey("test::Calculator"));

    // Validate program signatures for help output
    final var calculator = registry.get("test::Calculator");
    assertEquals("Calculator(arg0: Integer, arg1: Integer)", calculator.getSignature());
  }

  @Test
  void testTypeNameSimplification() {
    // Test the type name simplification used in error messages and help
    assertEquals("String", typeConverter.getSimpleTypeName("org.ek9.lang::String"));
    assertEquals("Integer", typeConverter.getSimpleTypeName("org.ek9.lang::Integer"));
    assertEquals("Boolean", typeConverter.getSimpleTypeName("org.ek9.lang::Boolean"));
    assertEquals("Float", typeConverter.getSimpleTypeName("org.ek9.lang::Float"));

    // Test edge cases
    assertEquals("CustomType", typeConverter.getSimpleTypeName("CustomType"));
    assertEquals("Type", typeConverter.getSimpleTypeName("very::deep::namespace::Type"));
  }

  @Test
  void testEK9TriStateErrorHandling() {
    // Test that TypeConverter follows EK9's tri-state model correctly

    // Invalid conversion should return unset object, not throw exception
    final var result = typeConverter.convertToEK9Type("not_a_number", "org.ek9.lang::Integer");
    assertNotNull(result);
    assertInstanceOf(org.ek9.lang.Integer.class, result);

    // Object should be unset, demonstrating EK9's tri-state semantics
    final var intResult = (org.ek9.lang.Integer) result;
    assertFalse(intResult._isSet()._true());

    // Error handling happens at the ProgramLauncher level
    // (This would be tested in ProgramLauncher tests with mocked scenarios)
  }

  @Test
  void testRegistryLookupAndValidation() {
    final var registry = createSampleRegistry();

    // Test successful lookup
    final var program = registry.get("introduction1::HelloMessage");
    assertNotNull(program);

    // Test failed lookup (as ProgramLauncher would handle)
    final var nonexistent = registry.get("does::not::Exist");
    assertNull(nonexistent);

    // Test argument count validation
    final var helloWorld = registry.get("introduction1::HelloWorld");
    final var userArgs = new String[] {"unexpected", "arguments"};

    // This simulates the validation that ProgramLauncher.launch() performs
    final var expectedCount = helloWorld.getParameterCount();
    final var actualCount = userArgs.length;
    assertTrue(actualCount != expectedCount); // Should trigger validation error
  }

  /**
   * Helper method to create a sample program registry.
   * This demonstrates the pattern that generated ek9.Main will use.
   */
  private Map<String, ProgramMetadata> createSampleRegistry() {
    final var registry = new HashMap<String, ProgramMetadata>();

    addProgramToRegistry(registry, "introduction1::HelloWorld", new String[0], "executeHelloWorld");
    addProgramToRegistry(registry, "introduction1::HelloMessage",
        new String[] {"org.ek9.lang::String"}, "executeHelloMessage");

    return registry;
  }

  /**
   * Helper method to add a program to the registry.
   * This demonstrates the pattern for generated registry creation.
   */
  private void addProgramToRegistry(final Map<String, ProgramMetadata> registry,
                                    final String qualifiedName,
                                    final String[] parameterTypes,
                                    final String methodName) {
    final var metadata = new ProgramMetadata(qualifiedName, parameterTypes, methodName);
    registry.put(qualifiedName, metadata);
  }
}