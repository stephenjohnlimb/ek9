package org.ek9lang.compiler.backend.jvm;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_BITS;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_BOOLEAN;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_CHARACTER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_COLOUR;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DATE;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DATETIME;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DIMENSION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DURATION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_FLOAT;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_INTEGER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_MILLISECOND;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_MONEY;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_REGEX;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_RESOLUTION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_STRING;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_TIME;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_OBJECT;

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
      case EK9_STRING -> "Lorg/ek9/lang/String;";
      case EK9_INTEGER -> "Lorg/ek9/lang/Integer;";
      case EK9_FLOAT -> "Lorg/ek9/lang/Float;";
      case EK9_BOOLEAN -> "Lorg/ek9/lang/Boolean;";
      case EK9_CHARACTER -> "Lorg/ek9/lang/Character;";
      case EK9_BITS -> "Lorg/ek9/lang/Bits;";

      // Date/Time types
      case EK9_DATE -> "Lorg/ek9/lang/Date;";
      case EK9_DATETIME -> "Lorg/ek9/lang/DateTime;";
      case EK9_TIME -> "Lorg/ek9/lang/Time;";
      case EK9_DURATION -> "Lorg/ek9/lang/Duration;";
      case EK9_MILLISECOND -> "Lorg/ek9/lang/Millisecond;";

      // Physical/Visual types
      case EK9_DIMENSION -> "Lorg/ek9/lang/Dimension;";
      case EK9_RESOLUTION -> "Lorg/ek9/lang/Resolution;";
      case EK9_COLOUR -> "Lorg/ek9/lang/Colour;";

      // Financial/Pattern types
      case EK9_MONEY -> "Lorg/ek9/lang/Money;";
      case EK9_REGEX -> "Lorg/ek9/lang/RegEx;";

      default -> {
        // Handle generic types and custom types
        if (ek9Type.contains("::")) {
          // Convert org.ek9.lang::ClassName to org/ek9/lang/ClassName
          yield "L" + ek9Type.replace(".", "/").replace("::", "/") + ";";
        }
        yield DESC_OBJECT; // Fallback
      }
    };
  }
}