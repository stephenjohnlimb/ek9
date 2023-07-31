/**
 * <p>
 * As mentioned in the 'org.ek9lang.compiler' package, these are the suppliers of 'phases'.
 * It's just a list of 'BiFunction of Workspace, CompilerFlags, CompilationPhaseResult' really, that's all
 * a phase actually is. Accept a 'WorkSpace' and 'CompilerFLags' and return a 'CompilationPhaseResult'.
 * This package contains groupings of those phases for use in different scenarios:
 * </p>
 * <ul>
 *   <li>Bootstrap</li>
 *   <li>LSP</li>
 *   <li>CommandLine</li>
 * </ul>
 */

package org.ek9lang.compiler.config;