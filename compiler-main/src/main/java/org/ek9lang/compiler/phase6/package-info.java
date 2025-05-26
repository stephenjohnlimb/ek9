/**
 * <b>J - Plugin Resolution</b>.
 * <p>
 * At this point it is important to be able to resolve all the standard library (and any other 'extern' components)
 * for the target architecture the software is being compiled for.
 * </p>
 * <p>
 * {@link org.ek9lang.compiler.phase6.PluginResolution} is the main entry point for this phase.
 * </p>
 * <p>
 * The main mechanism for defining the standard EK9 software components has been through
 * {@link org.ek9lang.compiler.Ek9BuiltinLangSupplier}. In effect this provides the EK9 classes/functions and
 * signatures (but not the implementation) of the EK9 standard library components. It is the equivalent of a
 * 'C' header file in some ways.
 * </p>
 * <p>
 * Specifically the declaration via <b>defines <u>extern</u> module org.ek9.lang</b> highlights that the definitions
 * are just that 'definitions' and not actually implementations.
 * </p>
 * <p>
 * Now this phase 'Plugin Resolution', ties up these signatures with some form or real implementations.
 * This is not limited to the EK9 standard library - but can and will be applied to any additional code base that
 * has an EK9 'extern' set of software components but a real implementation that is provided via a binary library.
 * </p>
 * <p>
 * The EK9 standard library will be provided in various forms so that it can be linked ready for final code generation.
 * Other 'extern' libraries may only support certain platforms or specific hardware.
 * </p>
 */

package org.ek9lang.compiler.phase6;