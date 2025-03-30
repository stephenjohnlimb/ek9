package org.ek9lang.compiler.phase10;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.IRModule;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.backend.OutputFileLocator;
import org.ek9lang.compiler.common.CompilableSourceHasErrors;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.ir.Construct;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.SharedThreadContext;

/**
 * MULTI THREADED
 * Preparation to generate code. Now this may vary in implementation significantly depending on the
 * target architecture output.
 * See compilerFlags.getTargetArchitecture to determine what to prepare to create.
 */
public class CodeGenerationPreparation extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.CODE_GENERATION_PREPARATION;
  private final CompilableSourceHasErrors sourceHasErrors = new CompilableSourceHasErrors();
  private final FileHandling fileHandling;
  private CompilerFlags compilerFlags;
  private OutputFileLocator outputFileLocator;

  public CodeGenerationPreparation(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                   final FileHandling fileHandling, final Consumer<CompilationEvent> listener,
                                   final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);
    this.fileHandling = fileHandling;
  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {
    this.compilerFlags = compilerFlags;
    this.outputFileLocator = new OutputFileLocator(fileHandling, compilerFlags);
    return underTakeFileCreation(workspace);
  }

  /**
   * Just creates the empty output files ready for code generation to populate.
   */
  private boolean underTakeFileCreation(final Workspace workspace) {

    createFilesMultiThreaded(workspace);

    return !sourceHasErrors.test(workspace.getSources());
  }

  private void createFilesMultiThreaded(final Workspace workspace) {

    final var projectDirectory = workspace.getSourceFileBaseDirectory();

    //Now get the .ek9 directory under that, this is where we will store the built artefacts.
    String projectDotEK9Directory = fileHandling.getDotEk9Directory(projectDirectory);

    //This will check or make the whole .ek9 tree.
    fileHandling.validateEk9Directory(projectDotEK9Directory, compilerFlags.getTargetArchitecture());

    compilableProgramAccess.accept(program ->
        program
            .getParsedModuleNames()
            .parallelStream()
            .map(program::getIRModules)
            .flatMap(List::parallelStream)
            .filter(IRModule::isNotEk9Core)
            .map(IRModule::getConstructs)
            .flatMap(List::parallelStream)
            .forEach(construct -> createFile(construct, projectDotEK9Directory))
    );

  }

  /**
   * THIS IS WHERE THE OUTPUT FILE IS CREATED.
   * Note that it is only created, see other parts for the actual generation of jvm bytecode/llvm code etc.
   */
  private void createFile(final Construct construct, final String projectDotEK9Directory) {

    final var locator = outputFileLocator.get();
    final var file = locator.apply(construct, projectDotEK9Directory);
    AssertValue.checkNotNull("File should have been created", file);

  }
}
