/**
 * <b>P - Packaging</b>.
 * <p>
 * Depending on the target architecture the developed code and associated libraries will be bound and pckaged.
 * </p>
 * <p>
 * For Java/JVm runtimes this will probably be a jar (including all the dependencies)
 * </p>
 * <p>
 * If the target architecture is a binary executable, then the appropriate 'EXE' pr 'ELF' will be created.
 * </p>
 * <p>
 * See the following for processing step in this final phase.
 * {@link org.ek9lang.compiler.phase12.PluginLinkage},
 * {@link org.ek9lang.compiler.phase12.Packaging},
 * {@link org.ek9lang.compiler.phase12.PackagingPostProcessing}
 * </p>
 */

package org.ek9lang.compiler.phase12;