package org.ek9lang.compiler.phase10;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.backend.OutputFileLocator;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
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
    //As this point any sort of failure is a failure of the compiler itself.
    return true;
  }

  private void createFilesMultiThreaded(final Workspace workspace) {

    final var projectDirectory = workspace.getSourceFileBaseDirectory();

    //Now get the .ek9 directory under that, this is where we will store the built artefacts.
    final var projectDotEK9Directory = fileHandling.getDotEk9Directory(projectDirectory);

    //This will check or make the whole .ek9 tree.
    fileHandling.validateEk9Directory(projectDotEK9Directory, compilerFlags.getTargetArchitecture());

    compilableProgramAccess.accept(program ->
        workspace
            .getSources()
            .parallelStream()
            .filter(CompilableSource::isNotExtern)
            .map(source -> getSourceTargetTuple(program, source, projectDotEK9Directory))
            .flatMap(List::parallelStream)
            .filter(this::targetOutOfDateWithSource)
            .forEach(tuple -> createFile(tuple.targetFile))
    );

  }

  private boolean targetOutOfDateWithSource(final SourceTargetTuple tuple) {
    if (!tuple.targetFile().exists()) {
      return true;
    }

    final var sourceFile = new File(tuple.compilableSource().getFileName());

    return sourceFile.lastModified() >= tuple.targetFile().lastModified();
  }

  /**
   * THIS IS WHERE THE OUTPUT FILE IS CREATED.
   * Note that it is only created, see other parts for the actual generation of jvm bytecode/llvm code etc.
   */
  private void createFile(final File targetFile) {

    fileHandling.createOrRecreateFile(targetFile);
    AssertValue.checkTrue("File should have been created", targetFile.exists());

  }

  private List<SourceTargetTuple> getSourceTargetTuple(final CompilableProgram program,
                                                       final CompilableSource compilableSource,
                                                       final String projectDotEK9Directory) {
    final var locator = outputFileLocator.get();
    return program
        .getIRModuleForCompilableSource(compilableSource)
        .getConstructs()
        .parallelStream()
        .map(construct ->
            new SourceTargetTuple(compilableSource, locator.apply(construct, projectDotEK9Directory)))
        .toList();
  }

  private record SourceTargetTuple(CompilableSource compilableSource, File targetFile) {

  }
}
