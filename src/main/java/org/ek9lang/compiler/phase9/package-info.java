/**
 * <b>M - Intermediate Representation Analysis and Optimization</b>.
 * <p>
 * This phase is designed to analyse and optimize the Intermediate Representation of the developed code.
 * </p>
 * <p>
 * While this could take quite a bit of effort, it's probably worth doing the basics and then consider
 * what the final code generation layer will be. This could be Java byte code or LVM code.
 * </p>
 * <p>
 * Both of the above potential output forms have very good optimizers built in, so it might be worth
 * depending on those initially.
 * </p>
 * <p>
 * {@link org.ek9lang.compiler.phase9.IRAnalysis} and
 * {@link org.ek9lang.compiler.phase9.IROptimisation} are the main entry points for this phase.
 * </p>
 */

package org.ek9lang.compiler.phase9;