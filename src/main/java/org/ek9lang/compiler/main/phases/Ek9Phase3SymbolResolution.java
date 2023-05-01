package org.ek9lang.compiler.main.phases;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.definition.ResolveDefineInferredTypeListener;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.exception.CompilerException;
import org.ek9lang.core.threads.SharedThreadContext;
import org.ek9lang.core.utils.Holder;

/**
 * MULTI THREADED
 * Now try check that all symbols used have a type. THIS IS MAJOR MILESTONE!
 * Create expression symbols in here; so we can pass the type base and also enable type checking.
 * Do some basic semantic checks in here before developing an IR.
 * Doing too much focus on the type resolution only we need to call this many times when there a
 * circular type loops.
 * The main complexity in here is building the return types for expressions and resolving types
 * for classes and specific methods.
 * So quite a bit of the semantic checking can go on in here. As always fail as early as possible
 * in the phases. But process that phase to the end.
 * Note that CallId is particularly complex to deal with all the ways to handle TextForSomething()
 * because it could be.
 * <pre>
 * A. new instance of class TextForSomething i.e. new TextForSomething()
 * - then we have to check it has a constructor with those arguments.
 * B. new instance of a template type that has not had parameters supplied like item := List()
 * - so can we infer its type from the variable assigned to?
 * C. new instance of a template type that has had parameters supplied like item := List(23)
 * - so we can infer from '23' this is a List of integer
 * D. Just a normal method call
 * - so find the method and check the parameters match.
 * E. call to a Function with a number of parameters.
 * F. call to a variable (C# delegate) that points to a function
 * - with a number of parameters.
 * </pre>
 * And probably more by the time we've finished.
 * This will create new inferred parameterised types as it processes instruction blocks.
 * This is why return types from expressions are essential, only if you just have the return type 'in time' can
 * we work out what the generic type is going to be parameterised with. As the next statements and expressions then
 * may use part of that new object via expressions we have to define then 'just a head' of when they are needed.
 */
public class Ek9Phase3SymbolResolution implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {

  private final boolean useMultiThreading;
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  private static final CompilationPhase thisPhase = CompilationPhase.FULL_RESOLUTION;

  /**
   * Create new instance to finally resolve all symbols, even inferred ones.
   */
  public Ek9Phase3SymbolResolution(final boolean multiThread,
                                   SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                   Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    this.useMultiThreading = multiThread;
    this.listener = listener;
    this.reporter = reporter;
    this.compilableProgramAccess = compilableProgramAccess;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    reporter.log(thisPhase);
    final var result = underTakeTypeSymbolResolutionAndDefinition(workspace);
    return new CompilationPhaseResult(thisPhase, result, compilerFlags.getCompileToPhase() == thisPhase);
  }

  private boolean underTakeTypeSymbolResolutionAndDefinition(Workspace workspace) {
    if (useMultiThreading) {
      defineSymbolsMultiThreaded(workspace);
    } else {
      defineSymbolsSingleThreaded(workspace);
    }
    return !sourceHaveErrors.test(workspace.getSources());
  }

  private void defineSymbolsMultiThreaded(Workspace workspace) {
    workspace.getSources()
        .parallelStream()
        .forEach(this::resolveOrDefineTypeSymbols);
  }

  private void defineSymbolsSingleThreaded(Workspace workspace) {
    workspace.getSources().forEach(this::resolveOrDefineTypeSymbols);
  }

  private void resolveOrDefineTypeSymbols(CompilableSource source) {
    //First get the parsed module for this source file.
    //This has to be done via a mutable holder through a reentrant lock to the program
    var holder = new Holder<ParsedModule>();
    compilableProgramAccess.accept(
        program -> holder.accept(Optional.ofNullable(program.getParsedModuleForCompilableSource(source))));

    if (holder.isEmpty()) {
      throw new CompilerException("Compiler error, the parsed module must be present for " + source.getFileName());
    }

    holder.ifPresent(parsedModule -> {
      ResolveDefineInferredTypeListener phaseListener =
          new ResolveDefineInferredTypeListener(parsedModule);
      ParseTreeWalker walker = new ParseTreeWalker();
      walker.walk(phaseListener, source.getCompilationUnitContext());
      listener.accept(new CompilationEvent(thisPhase, parsedModule, source));
    });
  }
}
