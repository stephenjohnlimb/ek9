package org.ek9lang.compiler.phase7;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.IRModule;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.AllProgramsSupplier;
import org.ek9lang.compiler.common.CompilableSourceHasErrors;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.core.SharedThreadContext;

/**
 * MULTI THREADED
 * All symbols resolve and so should be able to create an intermediate representation.
 * But this phase will be for non-generic types ONLY - see later how the IR is created for
 * Template/generic types.
 * NOTE: when processing and generating nodes - YOU must visit down the tree - don't be tempted
 * to use just the symbols from the previous phases. That was mainly to ensure semantics,
 * this is 'the generate an IR' yes you can use information from the previous stages but the
 * types of nodes generated are really important, and it is the context of where they are defined
 * that adds real value.
 */
public final class IRGenerator extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.IR_GENERATION;
  private final CompilableSourceHasErrors sourceHasErrors = new CompilableSourceHasErrors();
  private final AllProgramsSupplier allProgramsSupplier = new AllProgramsSupplier();

  public IRGenerator(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                     final Consumer<CompilationEvent> listener,
                     final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {
    return underTakeIRDefinition(workspace, compilerFlags);
  }

  private boolean underTakeIRDefinition(final Workspace workspace, final CompilerFlags compilerFlags) {

    defineIRMultiThreaded(workspace, compilerFlags);

    return !sourceHasErrors.test(workspace.getSources());
  }

  private void defineIRMultiThreaded(final Workspace workspace, final CompilerFlags compilerFlags) {

    final var allPrograms = allProgramsSupplier.apply(compilableProgramAccess);

    workspace.getSources()
        .parallelStream()
        .forEach(source -> defineIR(source, allPrograms, compilerFlags));

  }

  /**
   * THIS IS WHERE THE DEFINITION PHASE 7 LISTENER IS CREATED AND USED.
   * Here, we are creating the Intermediate Representation of code.
   * Clearly we do not create that for built-in EK9 code (as that will be provided).
   */
  private void defineIR(final CompilableSource source,
                        final List<AggregateSymbol> allPrograms,
                        final CompilerFlags compilerFlags) {

    final var irModule = new IRModule(source, allPrograms);
    irModule.acceptCompilationUnitContext(source.getCompilationUnitContext());
    if (irModule.isExtern()) {
      //Nothing to do with IR generation of built-in or extern ek9 code. As that will be provided as a library
      //When generating the final application/lib.
      return;
    }

    compilableProgramAccess.accept(compilableProgram -> compilableProgram.add(irModule));

    final var generator = new IRDfnGenerator(compilableProgramAccess, source, irModule, compilerFlags);

    //Now for the particular source and its new IR Module, create the IR.
    generator.create(source.getCompilationUnitContext());

    listener.accept(new CompilationEvent(thisPhase, getParsedModuleForSource(source), source));
  }
}
