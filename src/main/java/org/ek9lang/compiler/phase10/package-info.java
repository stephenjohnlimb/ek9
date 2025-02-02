/**
 * <b>N - Code Generation</b>.
 * <p>
 * This is the phase that actually generates output code (in some form) ready to be optimised and packaged.
 * </p>
 * <p>
 * Depending on the target Architecture specified, this phase will produce some form of file based
 * output that can either be packaged into something executable or further processed by something like LLVM.
 * </p>
 * <p>
 * If the target is a JVM, then this phase will produce byte code that can be packaged into a 'Jar'.
 * </p>
 * <p>
 * On the other hand if the target was LLVM (and a specific binary target) then the output would just be
 * llvm source files that can be consumed by the llvm compiler.
 * </p>
 * <p>
 * {@link org.ek9lang.compiler.phase10.CodeGenerationPreparation},
 * {@link org.ek9lang.compiler.phase10.CodeGenerationConstants},
 * {@link org.ek9lang.compiler.phase10.CodeGenerationAggregates} and
 * {@link org.ek9lang.compiler.phase10.CodeGenerationFunctions}
 * </p>
 */

package org.ek9lang.compiler.phase10;