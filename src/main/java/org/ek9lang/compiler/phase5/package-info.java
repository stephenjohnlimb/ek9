/**
 * <b>I - PRE Intermediate Representation</b> generation checks.
 * <p>
 * At this point, before the compiler builds the intermediate representation it complete some more basic checks.
 * </p>
 * <p>
 * {@link org.ek9lang.compiler.phase5.PreIntermediateRepresentationChecks} is the main entry point for this phase.
 * Most of the processing is delegated to {@link org.ek9lang.compiler.phase5.PreIRListener}.
 * </p>
 * <p>
 * This now follows the same pattern as most of the other Listeners, using
 * {@link org.ek9lang.compiler.common.SymbolsAndScopes} for handling scope traversals and locating recorded
 * symbols and scopes.
 * </p>
 * <p>
 * There is a 'complexity' calculator built into this phase, it is quite tolerant of high complexity. It does
 * emit compiler errors if a single method/function has complexity at level 50 or higher or if an aggregate has a
 * total complexity of 500 or higher. The rules for calculating complexity are based on a modified McCabe region
 * algorithm. It is quite simple in it's approach, as code elements are encountered a level of complexity is
 * incremented.
 * </p>
 * <p>
 * There are additional checks around simple evaluation of whether variables have been initialised or not.
 * In some cases properties on aggregates may need to be defined in uninitialised for i.e.
 * </p>
 * <pre>
 *   prop1 as String?
 * </pre>
 * <p>
 *   In such cases the EK9 compiler will mandate that the EK9 developer create all necessary coded constructors,
 *   it will <b>not</b> mandate that that 'prop1' is then initialised - it will leave that to the Ek9 developer to
 *   'take the risk' and understand what may or may not go wrong in their code.
 * </p>
 * <p>
 *   Ideally, all properties and variables are always declared in initialised form (even if the value is 'un-set').
 *   But this cannot always be mandated, so the EK9 compiler has some tolerance for these uninitialised variables.
 * </p>
 */

package org.ek9lang.compiler.phase5;