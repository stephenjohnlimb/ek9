/**
 * <p>
 * The directives are really aimed at helping the creation of the EK9 language, but they
 * maybe very useful in the early stages of adoption of ek9 (should there be any).
 * Because they enable an ek9 developer to send the ek9 compiler developers (me) an example of
 * code that is broken.
 * </p>
 * <p>
 * The example below shows that the type for 'i' could not be worked out by the
 * compiler and due to that later on a second error is triggered. The 'at' Error is this directive.
 * </p>
 * <p>
 * The 'FULL_RESOLUTION' is the 'phase' of compilation this error was detected in, and the
 * 'UNABLE_TO_DETERMINE_COMMON_TYPE' is the error enumeration from 'ErrorListener.SemanticClassification'.
 * </p>
 * <pre>
 * #!ek9
 * defines module bad.forloops.check
 *   defines program
 *
 *     BadForLoopIncompatibleTypes1
 *       stdout &larr; Stdout()
 *
 *       {@code @Error:}:  FULL_RESOLUTION: UNABLE_TO_DETERMINE_COMMON_TYPE
 *       for i in 1 ... 'c'
 *         {@code @Error:} FULL_RESOLUTION: TYPE_NOT_RESOLVED
 *         stdout.println(`Value [${i}]`)
 * //EOF
 * </pre>
 */

package org.ek9lang.compiler.directives;