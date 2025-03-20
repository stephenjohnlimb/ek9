package org.ek9lang.compiler.phase7;

import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.IRModule;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilableSourceHasErrors;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.support.AggregateManipulator;
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
public class IRGeneration extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.SIMPLE_IR_GENERATION;
  private final CompilableSourceHasErrors sourceHasErrors = new CompilableSourceHasErrors();

  public IRGeneration(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                      final Consumer<CompilationEvent> listener,
                      final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {
    return underTakeIRDefinition(workspace);
  }

  private boolean underTakeIRDefinition(final Workspace workspace) {

    defineIRMultiThreaded(workspace);

    return !sourceHasErrors.test(workspace.getSources());
  }

  private void defineIRMultiThreaded(final Workspace workspace) {

    workspace.getSources()
        .parallelStream()
        .forEach(this::defineIR);

  }

  /**
   * THIS IS WHERE THE DEFINITION PHASE 7 LISTENER IS CREATED AND USED.
   * Here, we are creating the Intermediate Representation of code.
   * Clearly we do not create that for built-in EK9 code (as that will be provided).
   */
  private void defineIR(final CompilableSource source) {

    final var irModule = new IRModule(source, compilableProgramAccess);
    irModule.acceptCompilationUnitContext(source.getCompilationUnitContext());
    if (AggregateManipulator.EK9_LANG.equals(irModule.getScopeName())
        || AggregateManipulator.EK9_MATH.equals(irModule.getScopeName())) {
      //Nothing to do with IR generation of built-in ek9 code. As that will be provided as a library
      //When generating the final application/lib.
      return;
    }


    compilableProgramAccess.accept(compilableProgram -> compilableProgram.add(irModule));

    final IRDefinitionVisitor irDefinitionVisitor = new IRDefinitionVisitor(compilableProgramAccess, source, irModule);
    irDefinitionVisitor.visitCompilationUnit(source.getCompilationUnitContext());

  }
}
