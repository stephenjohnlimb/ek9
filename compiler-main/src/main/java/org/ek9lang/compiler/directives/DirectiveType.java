package org.ek9lang.compiler.directives;

/**
 * To be used in EK9 source code, for things like internal tests
 * or compiler directives, instrumentation, reified type retention etc.
 * Each one can have different following structures in a free and easy format.
 */

//I want to use these names in EK9 source code, I want to use @Error and not @ERROR.
@SuppressWarnings("java:S115")
public enum DirectiveType {
  Error,
  Complexity,
  Resolved,
  Implements,
  NotResolved,
  Genus,
  Symbols,
  Compiler,
  Instrument,
  IR
}
