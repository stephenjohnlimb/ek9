package org.ek9lang.compiler.backend.jvm;

import java.util.function.UnaryOperator;

/**
 * Converts EK9 type names to JVM type descriptors for bytecode generation.
 * Handles built-in EK9 types and provides fallback for custom types.
 */
public final class EK9TypeToJVMDescriptor implements UnaryOperator<String> {

  @Override
  public String apply(final String ek9Type) {
    // Convert EK9 type names to JVM descriptors
    // Covers all types supported by ProgramArgumentPredicate
    return switch (ek9Type) {
      // Basic types
      case "org.ek9.lang::String" -> "Lorg/ek9/lang/String;";
      case "org.ek9.lang::Integer" -> "Lorg/ek9/lang/Integer;";
      case "org.ek9.lang::Float" -> "Lorg/ek9/lang/Float;";
      case "org.ek9.lang::Boolean" -> "Lorg/ek9/lang/Boolean;";
      case "org.ek9.lang::Character" -> "Lorg/ek9/lang/Character;";
      case "org.ek9.lang::Bits" -> "Lorg/ek9/lang/Bits;";

      // Date/Time types
      case "org.ek9.lang::Date" -> "Lorg/ek9/lang/Date;";
      case "org.ek9.lang::DateTime" -> "Lorg/ek9/lang/DateTime;";
      case "org.ek9.lang::Time" -> "Lorg/ek9/lang/Time;";
      case "org.ek9.lang::Duration" -> "Lorg/ek9/lang/Duration;";
      case "org.ek9.lang::Millisecond" -> "Lorg/ek9/lang/Millisecond;";

      // Physical/Visual types
      case "org.ek9.lang::Dimension" -> "Lorg/ek9/lang/Dimension;";
      case "org.ek9.lang::Resolution" -> "Lorg/ek9/lang/Resolution;";
      case "org.ek9.lang::Colour" -> "Lorg/ek9/lang/Colour;";

      // Financial/Pattern types
      case "org.ek9.lang::Money" -> "Lorg/ek9/lang/Money;";
      case "org.ek9.lang::RegEx" -> "Lorg/ek9/lang/RegEx;";

      default -> {
        // Handle generic types and custom types
        if (ek9Type.contains("::")) {
          yield "L" + ek9Type.replace("::", "/") + ";";
        }
        yield "Ljava/lang/Object;"; // Fallback
      }
    };
  }
}