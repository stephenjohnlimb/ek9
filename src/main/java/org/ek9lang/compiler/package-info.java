/**
 * <b>B - For the EK9 compiler</b> itself.
 * <p>
 * The {@link org.ek9lang.compiler.Compiler} interface is the abstract concept and
 * {@link org.ek9lang.compiler.Ek9Compiler} is the concrete actual implementation.
 * The {@link org.ek9lang.compiler.Workspace} contains the list of sources to be compiled.
 * Compilation is done in a number of 'phases'.
 * </p>
 * <p>
 * When creating an instance of the {@link org.ek9lang.compiler.Ek9Compiler}, the constructor accepts a supplier of
 * 'phases'. The reason for this is to enable the same compiler and some parts of the compilation process to be used
 * in a 'language server'.
 * So when the compiler is employed it can be used via the command line ({@link org.ek9lang.cli} to do full and
 * complete compilations, but also be used via something like 'VSCode' in an {@link org.ek9lang.lsp}.
 * </p>
 * <p>
 * The 'LSP' ({@link org.ek9lang.lsp}) only really does the 'front/middle' end of compilation from
 * {@link org.ek9lang.compiler.config}.
 * Whereas the 'CLI' ({@link org.ek9lang.cli} uses all of the phases from {@link org.ek9lang.compiler.config}.
 * </p>
 * <p>
 * Boot strapping the compiler is done via {@link org.ek9lang.compiler.Ek9LanguageBootStrap} and
 * {@link org.ek9lang.compiler.Ek9BuiltinLangSupplier}.
 * Strangely the {@link org.ek9lang.compiler.Ek9LanguageBootStrap}  uses an 'empty'
 * {@link org.ek9lang.compiler.Ek9Compiler} with just the {@link org.ek9lang.compiler.config.FrontEndSupplier}
 * phases to parse and populate a
 * {@link org.ek9lang.compiler.CompilableProgram} with all the basic types and standard library types/functions ready
 * for the developers code to be compiled. All the ek9 types are built in as text inside
 * {@link org.ek9lang.compiler.Ek9BuiltinLangSupplier}. The module is marked as 'extern'.
 * Other ek9 developers ek9 source can also employ 'extern' as there is a 'linking' phase that resolves
 * the declared constructs later. For the EK9 compiler, the built-in 'extern' constructs are shipped with the compiler.
 * </p>
 * <p>
 * Once the bootstrapping process has taken place, the {@link org.ek9lang.compiler.CompilableProgram}
 * will have all the EK9 constructs in memory.
 * Now a {@link org.ek9lang.compiler.config.FullPhaseSupplier} can be used with a new instance of the
 * {@link org.ek9lang.compiler.Ek9Compiler} (not the one used in boot strapping).
 * So really the EK9 Compiler itself is quite dumb and just consists of a <b>for loop</b>, calling each 'phase' with the
 * {@link org.ek9lang.compiler.Workspace} and {@link org.ek9lang.compiler.CompilerFlags}.
 * The phases are created and given access to the {@link org.ek9lang.compiler.CompilableProgram}, so that
 * during the processing of that phase it is possible to resolve symbols and also add in a new
 * {@link org.ek9lang.compiler.ParsedModule} to a set of {@link org.ek9lang.compiler.ParsedModules}.
 * </p>
 * <p>
 * The {@link org.ek9lang.compiler.CompilableProgram} just keeps the sets of
 * {@link org.ek9lang.compiler.ParsedModules} in a map, it then just coordinates the resolution of symbols in those
 * {@link org.ek9lang.compiler.ParsedModules}.
 * </p>
 * <p>
 * Really the only activity the {@link org.ek9lang.compiler.Ek9Compiler} does is trigger each phase in turn with the
 * {@link org.ek9lang.compiler.Workspace} and check the {@link org.ek9lang.compiler.CompilationPhaseResult} from that
 * phase. If that phase fails then the compiler stops and returns 'false' i.e. failed to compile the EK9 source files.
 * During the processing of each phase a listener of {@link org.ek9lang.compiler.common.CompilationEvent} and a
 * {@link org.ek9lang.compiler.common.CompilerReporter} are provided for each phase.
 * It is this mechanism that enables the EK9 developer 'issues' to be issued and reported.
 * </p>
 * <p>
 * The main critical functionality is all in the phases. See those packages for details of what each specific phase
 * is attempting to accomplish. Each phase can is numbered, but can have multiple passes within that phase.
 * </p>
 */

package org.ek9lang.compiler;