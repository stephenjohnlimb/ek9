package org.ek9lang.compiler.main;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.exception.AssertValue;

/**
 * The main EK9 compiler (HERE FOR COMPILER ENTRY).
 * In parts this will be multithreaded - mainly by using parallel stream.
 * But may use CompletableFutures and when virtual threads are available with Java 19+
 * we should see significant speed improvement in file (IO) operations.
 * I've had a tinker, and it's about 3 times faster.
 * This compiler actually just coordinates all the 'phases' of compilation.
 * This is now just a wrapper, the appropriate phases are supplied.
 */
public class Ek9Compiler implements Compiler {

  private final Supplier<List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>>> compilationPhaseSupplier;

  /**
   * Create a new compiler. Configure it with your event listener and flags.
   */
  public Ek9Compiler(
      Supplier<List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>>> compilationPhaseSupplier) {
    AssertValue.checkNotNull("CompilationPhaseSupplier Listener must be provided", compilationPhaseSupplier);
    this.compilationPhaseSupplier = compilationPhaseSupplier;
  }

  /**
   * HERE FOR COMPILER ENTRY.
   * This is THE main entry point in to initiating compilation.
   * It basically just runs through each of the phases configured.
   */
  @Override
  public boolean compile(Workspace workspace, CompilerFlags flags) {
    AssertValue.checkNotNull("Workspace must be provided", workspace);
    AssertValue.checkNotNull("Compiler Flags must be provided", flags);
    var reporter = new CompilerReporter(flags.isVerbose());

    //Get all the phases and process in turn, if phase fails then stop (false)
    //If the phase was the final one required, then also stop (but true).
    for (var phase : compilationPhaseSupplier.get()) {
      long before = System.nanoTime();
      var phaseResult = phase.apply(workspace, flags);
      long after = System.nanoTime();
      reporter.log(String.format("%s duration %d ns", phaseResult.phase(), after - before));
      if (!phaseResult.phaseSuccess()) {
        return false;
      } else if (phaseResult.phaseMatch()) {
        break;
      }
    }
    return true;
  }
}
