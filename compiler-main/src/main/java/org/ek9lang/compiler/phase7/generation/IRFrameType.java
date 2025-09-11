package org.ek9lang.compiler.phase7.generation;

/**
 * Enumeration of different IR generation frame types.
 *
 * <p>Each type represents a different kind of scope context
 * during IR generation, allowing the stack to provide
 * appropriate context-specific behavior.</p>
 */
public enum IRFrameType {
  MODULE,
  CLASS,
  RECORD,
  TRAIT,
  COMPONENT,
  FUNCTION,
  METHOD,
  OPERATOR,
  CONSTRUCTOR,
  PROGRAM,
  APPLICATION,
  BLOCK,
  EXPRESSION,
  CALL,
  ASSIGNMENT,
  LITERAL,
  VARIABLE_DECLARATION,
  SYNTHETIC_METHOD,
  ENUMERATION,
  GENERIC_INSTANTIATION,
  ASPECT_WEAVING,
  INJECTION_POINT,
  APPLICATION_BINDING
}