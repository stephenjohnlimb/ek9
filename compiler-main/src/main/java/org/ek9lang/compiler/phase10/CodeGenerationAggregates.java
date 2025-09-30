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
import org.ek9lang.compiler.backend.MainEntryTargetTuple;
import org.ek9lang.compiler.backend.MainEntryVisitorLocator;
import org.ek9lang.compiler.backend.OutputFileLocator;
import org.ek9lang.compiler.backend.OutputVisitorLocator;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.ProgramEntryPointInstr;
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
  private final MainEntryVisitorLocator mainEntryVisitorLocator = new MainEntryVisitorLocator();

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
    compilableProgramAccess.accept(program -> {
      // First generate all construct outputs
      workspace
          .getSources()
          .parallelStream()
          .forEach(compilableSource -> generateForSource(workspace, compilableSource, program));

      // Then generate Main.class if there are any programs defined.
      generateMainClassIfNeeded(workspace, program);
    });
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
        .filter(CompilableSource::isNotExtern)
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

  /**
   * Generate main entry point once if programs are present.
   * Uses target-agnostic locator pattern to support both JVM and LLVM backends.
   */
  private void generateMainClassIfNeeded(final Workspace workspace, final CompilableProgram program) {

    // Find first construct with ProgramEntryPointInstr
    final var programEntryPoint = workspace.getSources().stream()
        .filter(CompilableSource::isNotExtern)
        .map(program::getIRModuleForCompilableSource)
        .map(IRModule::getConstructs)
        .flatMap(List::stream)
        .map(IRConstruct::getProgramEntryPoint)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();

    // Generate main entry point if program entry point found
    programEntryPoint.ifPresent(entryPoint -> generateMainEntry(workspace, entryPoint));
  }

  /**
   * Generate main entry point using target-agnostic locator pattern.
   * Delegates to appropriate backend visitor (JVM or LLVM).
   */
  private void generateMainEntry(final Workspace workspace, final ProgramEntryPointInstr entryPoint) {
    // Get output directory structure - let visitors determine their own file names
    final var projectDirectory = workspace.getSourceFileBaseDirectory();
    final var projectDotEK9Directory = fileHandling.getDotEk9Directory(projectDirectory);
    final var outputDirectory = fileHandling.getMainGeneratedOutputDirectory(projectDotEK9Directory,
        compilerFlags.getTargetArchitecture());

    // Create tuple with context needed by visitors to determine their own target files
    final var mainEntryTargetTuple = new MainEntryTargetTuple(entryPoint, compilerFlags, fileHandling, outputDirectory);
    final var mainEntryVisitor = mainEntryVisitorLocator.apply(mainEntryTargetTuple);

    // Generate main entry point using target-specific visitor
    mainEntryVisitor.visit();
  }

}
