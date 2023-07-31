/**
 * <p>
 *  The tests in main are for valid examples that in the end should all fully compile.
 *  But as we're still working on the compiler implementation, they only compile to a specific phase.
 * </p>
 * <p>
 *  As each new phase is fully implemented, each one of these examples should be modified to move to
 *  the next phase.
 *  For example once FULL_RESOLUTION is deemed complete, move tests from TYPE_HIERARCHY_CHECKS to
 *  FULL_RESOLUTION. If the implementation is good then - we're good. If not then either error in the
 *  ek9 code or error in the compiler.
 * </p>
 * <p>
 *   Resolve those errors, normally it's an error in the compiler and may trigger the creation of a
 *   new ek9 test source file in 'src/test/resources/examples/parseButFailCompile' and an associated test
 *   in the appropriate 'phaseX' package.
 * </p>
 */
package org.ek9lang.compiler.main;