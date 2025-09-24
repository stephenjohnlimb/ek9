package ek9;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for ProgramMetadata.
 */
class ProgramMetadataTest {

  @Test
  void testBasicConstruction() {
    final var metadata = new ProgramMetadata(
        "test::HelloWorld",
        new String[0],
        "executeHelloWorld"
    );

    assertEquals("test::HelloWorld", metadata.getQualifiedName());
    assertEquals(0, metadata.getParameterCount());
    assertFalse(metadata.hasParameters());
    assertEquals("executeHelloWorld", metadata.getMethodName());
    assertTrue(metadata.getParameterTypes().isEmpty());
  }

  @Test
  void testConstructionWithParameters() {
    final var paramTypes = new String[] {"org.ek9.lang::String", "org.ek9.lang::Integer"};
    final var metadata = new ProgramMetadata(
        "test::HelloMessage",
        paramTypes,
        "executeHelloMessage"
    );

    assertEquals("test::HelloMessage", metadata.getQualifiedName());
    assertEquals(2, metadata.getParameterCount());
    assertTrue(metadata.hasParameters());
    assertEquals("executeHelloMessage", metadata.getMethodName());
    assertEquals(2, metadata.getParameterTypes().size());
    assertEquals("org.ek9.lang::String", metadata.getParameterTypes().get(0));
    assertEquals("org.ek9.lang::Integer", metadata.getParameterTypes().get(1));
  }

  @Test
  void testParameterTypesAreImmutable() {
    final var paramTypes = new String[] {"org.ek9.lang::String"};
    final var metadata = new ProgramMetadata(
        "test::TestProgram",
        paramTypes,
        "executeTest"
    );

    // Modifying original array should not affect metadata
    paramTypes[0] = "modified";
    assertEquals("org.ek9.lang::String", metadata.getParameterTypes().getFirst());

    // Returned list should be immutable
    assertThrows(UnsupportedOperationException.class, () ->
        metadata.getParameterTypes().add("new.type"));
  }

  @Test
  void testNullParameterTypesHandled() {
    final var metadata = new ProgramMetadata(
        "test::NoParams",
        null,
        "executeNoParams"
    );

    assertEquals(0, metadata.getParameterCount());
    assertFalse(metadata.hasParameters());
    assertTrue(metadata.getParameterTypes().isEmpty());
  }

  @Test
  void testSignatureGeneration() {
    // Test parameterless program
    final var noParams = new ProgramMetadata(
        "introduction1::HelloWorld",
        new String[0],
        "executeHelloWorld"
    );
    assertEquals("HelloWorld()", noParams.getSignature());

    // Test single parameter program
    final var oneParam = new ProgramMetadata(
        "introduction1::HelloMessage",
        new String[] {"org.ek9.lang::String"},
        "executeHelloMessage"
    );
    assertEquals("HelloMessage(arg0: String)", oneParam.getSignature());

    // Test multiple parameter program
    final var multiParam = new ProgramMetadata(
        "test::MultiParam",
        new String[] {"org.ek9.lang::String", "org.ek9.lang::Integer", "org.ek9.lang::Boolean"},
        "executeMultiParam"
    );
    assertEquals("MultiParam(arg0: String, arg1: Integer, arg2: Boolean)", multiParam.getSignature());
  }

  @Test
  void testSimpleNameExtraction() {
    // Test standard qualified name
    final var metadata1 = new ProgramMetadata(
        "module::ClassName",
        new String[0],
        "execute"
    );
    assertEquals("ClassName()", metadata1.getSignature());

    // Test deeply qualified name
    final var metadata2 = new ProgramMetadata(
        "very::deep::namespace::ClassName",
        new String[0],
        "execute"
    );
    assertEquals("ClassName()", metadata2.getSignature());

    // Test simple name (no ::)
    final var metadata3 = new ProgramMetadata(
        "SimpleName",
        new String[0],
        "execute"
    );
    assertEquals("SimpleName()", metadata3.getSignature());
  }

  @Test
  void testTypeNameSimplification() {
    final var metadata = new ProgramMetadata(
        "test::Program",
        new String[] {"org.ek9.lang::String", "SimpleType", "very::deep::Type"},
        "execute"
    );

    final var signature = metadata.getSignature();
    assertTrue(signature.contains("arg0: String"));  // org.ek9.lang::String -> String
    assertTrue(signature.contains("arg1: SimpleType"));  // SimpleType -> SimpleType
    assertTrue(signature.contains("arg2: Type"));  // very::deep::Type -> Type
  }

  @Test
  void testValidationNullQualifiedName() {
    assertThrows(NullPointerException.class, () ->
        new ProgramMetadata(null, new String[0], "execute"));
  }

  @Test
  void testValidationEmptyQualifiedName() {
    assertThrows(IllegalArgumentException.class, () ->
        new ProgramMetadata("", new String[0], "execute"));

    assertThrows(IllegalArgumentException.class, () ->
        new ProgramMetadata("   ", new String[0], "execute"));
  }

  @Test
  void testValidationNullMethodName() {
    assertThrows(NullPointerException.class, () ->
        new ProgramMetadata("test::Program", new String[0], null));
  }

  @Test
  void testValidationEmptyMethodName() {
    assertThrows(IllegalArgumentException.class, () ->
        new ProgramMetadata("test::Program", new String[0], ""));

    assertThrows(IllegalArgumentException.class, () ->
        new ProgramMetadata("test::Program", new String[0], "   "));
  }

  @Test
  void testEqualsAndHashCode() {
    final var metadata1 = new ProgramMetadata(
        "test::Program",
        new String[] {"org.ek9.lang::String"},
        "execute"
    );

    final var metadata2 = new ProgramMetadata(
        "test::Program",
        new String[] {"org.ek9.lang::String"},
        "execute"
    );

    final var metadata3 = new ProgramMetadata(
        "test::Different",
        new String[] {"org.ek9.lang::String"},
        "execute"
    );

    // Test equals
    assertEquals(metadata1, metadata2);
    assertNotEquals(metadata1, metadata3);

    // Test hashCode consistency
    assertEquals(metadata1.hashCode(), metadata2.hashCode());

    // Test reflexivity
    assertEquals(metadata1, metadata1);
  }

  @Test
  void testToString() {
    final var metadata = new ProgramMetadata(
        "test::Program",
        new String[] {"org.ek9.lang::String"},
        "execute"
    );

    final var toString = metadata.toString();
    assertTrue(toString.contains("test::Program"));
    assertTrue(toString.contains("execute"));
    assertTrue(toString.contains("org.ek9.lang::String"));
  }
}