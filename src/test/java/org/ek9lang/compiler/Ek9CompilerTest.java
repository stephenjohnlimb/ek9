package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.ek9lang.compiler.common.CompilableProgramSuitable;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.config.FullPhaseSupplier;
import org.ek9lang.compiler.support.PathToSourceFromName;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.OsSupport;
import org.ek9lang.core.SharedThreadContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * Checks both valid and invalid parsing. For valid source cycles through each of the compilation phases.
 */
class Ek9CompilerTest {

  private final CompilerReporter reporter = new CompilerReporter(false, true);
  private final OsSupport osSupport = new OsSupport(true);

  private final Supplier<SharedThreadContext<CompilableProgram>> sharedContext
      = new CompilableProgramSuitable();

  private final Function<String, Workspace> validEk9Workspace = subDir -> {
    final var fullPath = new PathToSourceFromName().apply("/examples/basics/HelloWorld.ek9");
    final var base = osSupport.makeSubDirectoryIfNotExists(new File(osSupport.getTempDirectory()), subDir);
    Workspace rtn = new Workspace(base);
    rtn.addSource(fullPath);
    return rtn;
  };

  private final Supplier<Workspace> inValidEk9Workspace = () -> {
    var fullPath = new PathToSourceFromName().apply("/badExamples/basics/missingModuleKeyWord.ek9");
    Workspace rtn = new Workspace();
    rtn.addSource(fullPath);
    return rtn;
  };

  private static Stream<CompilationPhase> allCompilationPhases() {
    return Stream.of(CompilationPhase.values());
  }

  @ParameterizedTest
  @MethodSource("allCompilationPhases")
  void testSimpleSuccessfulParsing(final CompilationPhase upToPhase) {
    CompilationPhaseListener listener = compilationEvent -> {
      var source = compilationEvent.source();
      var phase = compilationEvent.phase();
      if (source.getErrorListener().hasErrors()) {
        reporter.report("Errors  : " + phase + ", source: " + source.getFileName());
        source.getErrorListener().getErrors().forEachRemaining(System.out::println);
      }
    };

    var sharedCompilableProgram = sharedContext.get();

    final var fileHandling = new FileHandling(osSupport);
    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedCompilableProgram, fileHandling,
        listener, reporter);

    var compiler = new Ek9Compiler(allPhases, reporter.isMuteReportedErrors());
    final var workspace = validEk9Workspace.apply(upToPhase.name());
    var result = compiler.compile(workspace, new CompilerFlags(upToPhase, reporter.isVerbose()));
    assertTrue(result);
  }

  @Test
  void testSimpleUnSuccessfulParsing() {
    CompilationPhaseListener listener = _ -> {
    };
    final var fileHandling = new FileHandling(osSupport);

    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedContext.get(), fileHandling,
        listener, new CompilerReporter(false, reporter.isMuteReportedErrors()));

    var compiler = new Ek9Compiler(allPhases, reporter.isMuteReportedErrors());
    assertFalse(compiler.compile(inValidEk9Workspace.get(), new CompilerFlags()));
  }
}
