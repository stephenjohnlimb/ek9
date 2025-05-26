package org.ek9lang.compiler.phase10;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.IRModule;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.backend.OutputFileLocator;
import org.ek9lang.compiler.backend.OutputVisitorLocator;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.SharedThreadContext;

/**
 * MULTI THREADED
 * Generate all type of aggregates, typically classes, components, records, etc.
 * See compilationContext.commandLine().targetArchitecture to determine what to prepare to create.
 */
public class CodeGenerationAggregates extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.CODE_GENERATION_AGGREGATES;
  private OutputFileLocator outputFileLocator;
  private CompilerFlags compilerFlags;
  private final FileHandling fileHandling;

  public CodeGenerationAggregates(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                  final FileHandling fileHandling, final Consumer<CompilationEvent> listener,
                                  final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);
    this.fileHandling = fileHandling;
  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {
    this.compilerFlags = compilerFlags;
    this.outputFileLocator = new OutputFileLocator(fileHandling, compilerFlags);

    return generateConstructOutput(workspace);
  }

  /**
   * Expects an empty file to have been created in previous phase.
   * This is the code generation phase, so it accesses the appropriate IRModule
   * and generates the output for all the Constructs in that module.
   */
  private boolean generateConstructOutput(final Workspace workspace) {

    generateOutputMultiThreaded(workspace);

    //As this point any sort of failure is a failure of the compiler itself.
    return true;
  }

  private void generateOutputMultiThreaded(final Workspace workspace) {
    compilableProgramAccess.accept(program ->
        workspace
            .getSources()
            .parallelStream()
            .forEach(compilableSource -> generateForSource(workspace, compilableSource, program)));

  }

  private void generateForSource(final Workspace workspace,
                                 final CompilableSource compilableSource,
                                 final CompilableProgram program) {

    //Now get the .ek9 directory under that, this is where we will store the built artefacts.
    final var projectDirectory = workspace.getSourceFileBaseDirectory();
    var fileName = compilableSource.getFileName().replace(projectDirectory, "");
    final var relativeFileName = fileName.startsWith(File.separator) ? fileName.substring(1) : fileName;

    final var projectDotEK9Directory = fileHandling.getDotEk9Directory(projectDirectory);
    final var locator = outputFileLocator.get();

    Optional.of(compilableSource).stream()
        .map(program::getIRModuleForCompilableSource)
        .map(IRModule::getConstructs)
        .flatMap(List::parallelStream)
        .map(construct -> new ConstructTargetTuple(construct, relativeFileName, compilerFlags,
            locator.apply(construct, projectDotEK9Directory)))
        .forEach(this::produceConstructOutput);
  }

  /**
   * This will use a visitor to create the appropriate output for the file.
   * A new visitor is created for each Construct to be visited and written output to a File.
   */
  private void produceConstructOutput(final ConstructTargetTuple constructTargetTuple) {
    final var outputVisitor =
        new OutputVisitorLocator().apply(constructTargetTuple);
    outputVisitor.visit();

  }
}
