/**
 * <p>
 * The directives are really aimed at helping the creation of the EK9 language, but they
 * maybe very useful in the early stages of adoption of EK9 (should there be any).
 * </p>
 * <p>
 * Because they enable an EK9 developer to send the EK9 compiler developers (me) an example of
 * code that is broken.
 * </p>
 * <p>
 * The example below shows that the type for 'value' is an integer at declaration, but
 * later in the code an attempt is made to assign a float value to it.
 * The 'at' Error is this directive that looks for this error to ensue it it present.
 * </p>
 * <p>
 * The 'FULL_RESOLUTION' is the 'phase' of compilation this error was detected in, and the
 * 'INCOMPATIBLE_TYPES' is the error enumeration from 'ErrorListener.SemanticClassification'.
 * </p>
 * <pre>
 * #!ek9
 * defines module bad.assignment.check
 *   defines program
 *
 *     badAssignment()
 *       value as Integer: 1
 *
 *       //As it is an Integer, you cannot assign a Float to it.
 *       {@code @Error}: FULL_RESOLUTION: INCOMPATIBLE_TYPES
 *       value := 7.0
 *       assert value?
 *
 *  //EOF
 * </pre>
 * <p>
 *   This mechanism is used widely in development, to ensure that correct code does not emit errors, but
 *   importantly incorrect code does emit errors (and the right sort of errors in the right phase of compilation).
 * </p>
 */

package org.ek9lang.compiler.directives;