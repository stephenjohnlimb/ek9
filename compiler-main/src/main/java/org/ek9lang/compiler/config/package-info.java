/**
 * <b>C - For the Suppliers</b> of the 'compilation phases' as mentioned in the {@link org.ek9lang.compiler} package.
 * <p>
 * It's just a list of 'BiFunction of {@link org.ek9lang.compiler.Workspace}, {@link org.ek9lang.compiler.CompilerFlags}
 * , {@link org.ek9lang.compiler.CompilationPhaseResult}' really, that's all a phase actually is.
 * </p>
 * <p>Accept a {@link org.ek9lang.compiler.Workspace} and {@link org.ek9lang.compiler.CompilerFlags}
 * and return a {@link org.ek9lang.compiler.CompilationPhaseResult}.
 * This package contains groupings of those phases for use in different scenarios:
 * </p>
 * <ul>
 *   <li>Bootstrap - {@link org.ek9lang.compiler.Ek9LanguageBootStrap},
 *   {@link org.ek9lang.compiler.Ek9BuiltinLangSupplier}</li>
 *   <li>LSP - {@link org.ek9lang.compiler.config.FrontEndSupplier},
 *   {@link org.ek9lang.compiler.config.MiddleEndSupplier}</li>
 *   <li>CommandLine - {@link org.ek9lang.compiler.config}</li>
 * </ul>
 */

package org.ek9lang.compiler.config;