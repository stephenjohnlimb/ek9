package org.ek9lang.compiler.phase7.support;

/**
 * Enumeration of different IR generation frame types.
 * 
 * <p>Each type represents a different kind of scope context
 * during IR generation, allowing the stack to provide
 * appropriate context-specific behavior.</p>
 */
public enum IRFrameType {
  
  /** Module-level scope (base frame) */
  MODULE,
  
  /** Class definition scope */
  CLASS,
  
  /** Record definition scope */
  RECORD,
  
  /** Trait definition scope */
  TRAIT,
  
  /** Component definition scope */
  COMPONENT,
  
  /** Function definition scope */
  FUNCTION,
  
  /** Method definition scope */
  METHOD,
  
  /** Operator definition scope */
  OPERATOR,
  
  /** Constructor definition scope */
  CONSTRUCTOR,
  
  /** Program definition scope */
  PROGRAM,
  
  /** Application definition scope */
  APPLICATION,
  
  /** Block statement scope (for, while, if, etc.) */
  BLOCK,
  
  /** Expression evaluation scope */
  EXPRESSION,
  
  /** Call expression scope (method/function calls) */
  CALL,
  
  /** Assignment expression scope */
  ASSIGNMENT,
  
  /** Literal value scope */
  LITERAL,
  
  /** Variable declaration scope */
  VARIABLE_DECLARATION,
  
  /** Synthetic method scope (for generated operators) */
  SYNTHETIC_METHOD,
  
  /** Enumeration scope (for enum operator synthesis) */
  ENUMERATION,
  
  /** Generic type instantiation scope */
  GENERIC_INSTANTIATION,
  
  /** Aspect weaving scope */
  ASPECT_WEAVING,
  
  /** Dependency injection scope */
  INJECTION_POINT,
  
  /** Application binding scope */
  APPLICATION_BINDING
}