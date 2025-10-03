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
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_BITS;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_BOOLEAN;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_CHARACTER;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_COLOUR;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_DATE;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_DATETIME;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_DIMENSION;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_DURATION;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_FLOAT;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_INTEGER;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_MILLISECOND;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_MONEY;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_REGEX;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_RESOLUTION;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_TIME;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_OBJECT;

import java.util.function.UnaryOperator;

/**
 * Converts EK9 type names to JVM type descriptors for bytecode generation.
 * Handles built-in EK9 types and provides fallback for custom types.
 */
public final class EK9TypeToJVMDescriptor implements UnaryOperator<String> {
  private final FullyQualifiedJvmName fullyQualifiedJvmName = new FullyQualifiedJvmName();

  @Override
  public String apply(final String ek9Type) {
    // Convert EK9 type names to JVM descriptors
    // Covers all types supported by ProgramArgumentPredicate
    return switch (ek9Type) {
      // Basic types
      case EK9_STRING -> DESC_EK9_STRING;
      case EK9_INTEGER -> DESC_EK9_INTEGER;
      case EK9_FLOAT -> DESC_EK9_FLOAT;
      case EK9_BOOLEAN -> DESC_EK9_BOOLEAN;
      case EK9_CHARACTER -> DESC_EK9_CHARACTER;
      case EK9_BITS -> DESC_EK9_BITS;

      // Date/Time types
      case EK9_DATE -> DESC_EK9_DATE;
      case EK9_DATETIME -> DESC_EK9_DATETIME;
      case EK9_TIME -> DESC_EK9_TIME;
      case EK9_DURATION -> DESC_EK9_DURATION;
      case EK9_MILLISECOND -> DESC_EK9_MILLISECOND;

      // Physical/Visual types
      case EK9_DIMENSION -> DESC_EK9_DIMENSION;
      case EK9_RESOLUTION -> DESC_EK9_RESOLUTION;
      case EK9_COLOUR -> DESC_EK9_COLOUR;

      // Financial/Pattern types
      case EK9_MONEY -> DESC_EK9_MONEY;
      case EK9_REGEX -> DESC_EK9_REGEX;

      default -> {
        // Handle generic types and custom types
        if (ek9Type.contains("::")) {
          // Convert org.ek9.lang::ClassName to Lorg/ek9/lang/ClassName;
          yield "L" + fullyQualifiedJvmName.apply(ek9Type) + ";";
        }
        yield DESC_OBJECT; // Fallback
      }
    };
  }
}