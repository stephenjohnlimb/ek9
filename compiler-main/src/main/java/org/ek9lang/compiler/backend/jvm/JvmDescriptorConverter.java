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
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_VOID;
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

import java.util.function.UnaryOperator;

/**
 * Converts EK9 type names to JVM type descriptors.
 * Handles Java primitives, EK9 built-in types, and custom types.
 * Implements UnaryOperator for functional composition and reuse.
 * <p>
 * JVM Type Descriptors:
 * - Java primitives: Z (boolean), B (byte), C (char), S (short), I (int), J (long), F (float), D (double)
 * - Void: V
 * - EK9 built-in types: Lorg/ek9/lang/String;, Lorg/ek9/lang/Integer;, etc.
 * - Object types: Lpackage/ClassName;
 * </p>
 */
final class JvmDescriptorConverter implements UnaryOperator<String> {

  private final FullyQualifiedJvmName fullyQualifiedJvmName;

  /**
   * Create converter with JVM name resolution capability.
   *
   * @param fullyQualifiedJvmName Utility for converting EK9 names to JVM internal names
   */
  JvmDescriptorConverter(final FullyQualifiedJvmName fullyQualifiedJvmName) {
    this.fullyQualifiedJvmName = fullyQualifiedJvmName;
  }

  /**
   * Convert EK9 type name to JVM descriptor format.
   * Handles Java primitives, EK9 built-in types, and custom types.
   *
   * @param ek9TypeName The EK9 type name to convert
   * @return JVM type descriptor (e.g., "Z" for boolean, "Lorg/ek9/lang/String;" for EK9 String)
   */
  @Override
  public String apply(final String ek9TypeName) {
    // Handle void type specially
    if (EK9_VOID.equals(ek9TypeName)) {
      return "V";
    }

    // Handle Java primitive types first (used in IR for method return types)
    return switch (ek9TypeName) {
      case "boolean" -> "Z";
      case "byte" -> "B";
      case "char" -> "C";
      case "short" -> "S";
      case "int" -> "I";
      case "long" -> "J";
      case "float" -> "F";
      case "double" -> "D";

      // EK9 built-in types - basic types
      case EK9_STRING -> DESC_EK9_STRING;
      case EK9_INTEGER -> DESC_EK9_INTEGER;
      case EK9_FLOAT -> DESC_EK9_FLOAT;
      case EK9_BOOLEAN -> DESC_EK9_BOOLEAN;
      case EK9_CHARACTER -> DESC_EK9_CHARACTER;
      case EK9_BITS -> DESC_EK9_BITS;

      // EK9 built-in types - date/time types
      case EK9_DATE -> DESC_EK9_DATE;
      case EK9_DATETIME -> DESC_EK9_DATETIME;
      case EK9_TIME -> DESC_EK9_TIME;
      case EK9_DURATION -> DESC_EK9_DURATION;
      case EK9_MILLISECOND -> DESC_EK9_MILLISECOND;

      // EK9 built-in types - physical/visual types
      case EK9_DIMENSION -> DESC_EK9_DIMENSION;
      case EK9_RESOLUTION -> DESC_EK9_RESOLUTION;
      case EK9_COLOUR -> DESC_EK9_COLOUR;

      // EK9 built-in types - financial/pattern types
      case EK9_MONEY -> DESC_EK9_MONEY;
      case EK9_REGEX -> DESC_EK9_REGEX;

      // All other types - convert using FullyQualifiedJvmName
      default -> "L" + fullyQualifiedJvmName.apply(ek9TypeName) + ";";
    };
  }
}
