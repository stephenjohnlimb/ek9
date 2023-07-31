/**
 * The main package for entry into the Ek9 compiler.
 * <p>
 * The 'Compiler' interface is the abstract concept and 'Ek9Compiler' is the concrete actual implementation.
 * The 'Workspace' contains the list of sources to be compiled. Compilation is done in a number of 'phases'.
 * </p>
 * <p>
 * When creating an instance of the EK9Compiler, the constructor accepts a supplier of 'phases'. The reason for this
 * is to enable the same compiler and some parts of the compilation process to be used in a 'language server'.
 * So when the compiler is employed it can be used via the commandline to do full and complete compilations, but also
 * be used via something like 'VSCode' in an 'LSP'. The 'LSP' only really does the 'front/middle' end of compilation.
 * </p>
 * <p>
 * Boot strapping the compiler is done via 'Ek9LanguageBootStrap' and 'Ek9BuiltinLangSupplier'. Strangely the
 * 'Ek9LanguageBootStrap' uses an 'empty' EKCompiler with just the 'frontend' phases to parse and populate a
 * 'CompilableProgram' with all the basic types and standard library types/functions ready for the developers code
 * to be compiled. All the ek9 types are built in as text inside 'Ek9BuiltinLangSupplier'. The module is marked as
 * 'extern'. Other ek9 developers ek9 source can also employ 'extern' as there is a 'linking' phase that resolves
 * the declared constructs later. For the EK9 compiler, the 'extern' constructs are shipped with the compiler.
 * </p>
 * <p>
 * Once the bootstrapping process has taken place, the 'CompilableProgram' will have all the EK9 constructs in memory.
 * Now a 'FullPhaseSupplier' can be used with a new instance of the Ek9Compiler (not the one used in boot strapping).
 * So really the EK9 Compiler itself is quite dumb and just consists of a 'for loop', calling each 'phase' with the
 * 'Workspace' and 'Compiler Flags'. The phases are created and given access to the 'CompilableProgram', so that
 * during the processing of that phase it is possible to resolve symbols and also add in new 'ParsedModules'.
 * </p>
 * <p>
 * Really the only activity the 'EK9Compiler' does is trigger each phase in turn with the 'Workspace' and check the
 * result from that phase. If that phase fails then the compiler stops and returns 'false' i.e. failed to compile.
 * During the processing of each phase a listener of 'CompilationEvent' and a 'CompilerReporter' are provided for
 * each phase. It is this mechanism that enables the EK9 developer 'issues' to be issued and reported.
 * </p>
 * <p>
 * The main critical functionality is all in the phases. See those packages for details of what each specific phase
 * is attempting to accomplish.
 * </p>
 */

package org.ek9lang.compiler;