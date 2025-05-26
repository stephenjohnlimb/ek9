/**
 * <p>
 * The tests in main are for valid examples that in the end should all fully compile.
 * But as we're still working on the compiler implementation, they only compile to a specific phase.
 * </p>
 * <p>
 * As each new phase is fully implemented, each one of these examples should be modified to move to
 * the next phase.
 * For example once FULL_RESOLUTION is deemed complete, move tests from TYPE_HIERARCHY_CHECKS to
 * FULL_RESOLUTION. If the implementation is good then - we're good. If not then either error in the
 * ek9 code or error in the compiler.
 * </p>
 * <p>
 * Resolve those errors, normally it's an error in the compiler and may trigger the creation of a
 * new ek9 test source file in 'src/test/resources/examples/parseButFailCompile' and an associated test
 * in the appropriate 'phaseX' package.
 * </p>
 * <p>
 * Note that the whole purpose of this approach is to move in a 'wave' like manner pushing gently forward
 * across th whole broad scope of the EK9 language. It is NOT to try and just get simple stuff fully working
 * from end to end. This 'wave' approach is arguably much harder as it means it is necessary to fully code in the
 * whole EK9 language and while doing so uncover defects (those defects are valuable in the sense of enabling specific
 * 'bad' code examples to be captured and added to the correct 'examples/parseButFailCompile' part).
 * </p>
 * This above wave approach, may take me much longer. It has the advantage of me being able to write code in EK9
 * so it is much more familiar and also highlights to me what the common issues are likely to be when developers use
 * the language. It also has enabled me to remove features or add compiler checks to make the language more robust.
 */
package org.ek9lang.compiler.main;