/**
 * <p>
 * This is the main source of EK9 Symbols used within the compiler.
 * There is some degree of complexity here, start with 'ISymbol' and look at the type hierarchy.
 * </p>
 * <p>
 * Just about everything the the ek9 developer adds to an ek9 source file becomes one or more symbols.
 * </p>
 * <p>
 * For complex aggregates like classes, their is also a resolution hierarchy that resolves in enclosing scopes.
 * But even in flow constructs the resolution flows upwards and back to the very top level 'CompilableProgram'.
 * </p>
 */

package org.ek9lang.compiler.symbols;