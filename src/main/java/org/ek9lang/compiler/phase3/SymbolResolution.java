package org.ek9lang.compiler.phase3;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilableSourceHasErrors;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.SharedThreadContext;

/**
 * <p>
 * MULTI THREADED
 * Now try check that all symbols used have a type. THIS IS MAJOR MILESTONE!
 * Create expression symbols in here; so we can pass the type base and also enable type checking.
 * Do some basic semantic checks in here before developing an IR.
 * </p>
 * <p>
 * Doing too much focus on the type resolution only we need to call this many times when there a
 * circular type loops.
 * The main complexity in here is building the return types for expressions and resolving types
 * for classes and specific methods.
 * So quite a bit of the semantic checking can go on in here. As always fail as early as possible
 * in the phases. But process that phase to the end.
 * Note that CallId is particularly complex to deal with all the ways to handle TextForSomething()
 * because it could be.
 * </p>
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
 * <p>
 * And probably more by the time we've finished.
 * This will create new inferred parameterised types as it processes instruction blocks.
 * This is why return types from expressions are essential, only if you just have the return type 'in time' can
 * we work out what the generic type is going to be parameterised with. As the next statements and expressions then
 * may use part of that new object via expressions we have to define then 'just a head' of when they are needed.
 * </p>
 */
public final class SymbolResolution extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.FULL_RESOLUTION;
  private final boolean useMultiThreading;
  private final CompilableSourceHasErrors sourceHasErrors = new CompilableSourceHasErrors();

  /**
   * Create new instance to finally resolve all symbols, even inferred ones.
   */
  public SymbolResolution(final boolean multiThread,
                          final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                          final Consumer<CompilationEvent> listener,
                          final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);
    this.useMultiThreading = multiThread;

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    return underTakeTypeSymbolResolutionAndDefinition(workspace);
  }

  private boolean underTakeTypeSymbolResolutionAndDefinition(final Workspace workspace) {

    if (useMultiThreading) {
      defineSymbolsMultiThreaded(workspace);
    } else {
      defineSymbolsSingleThreaded(workspace);
    }

    return !sourceHasErrors.test(workspace.getSources());
  }

  private void defineSymbolsMultiThreaded(final Workspace workspace) {

    workspace.getSources()
        .parallelStream()
        .forEach(this::resolveOrDefineTypeSymbols);

  }

  private void defineSymbolsSingleThreaded(final Workspace workspace) {

    workspace.getSources().forEach(this::resolveOrDefineTypeSymbols);

  }

  private void resolveOrDefineTypeSymbols(final CompilableSource source) {

    //First get the parsed module for this source file.
    //This has to be done via a mutable holder through a reentrant lock to the program
    final var holder = new AtomicReference<ParsedModule>();

    compilableProgramAccess.accept(
        program -> holder.set(program.getParsedModuleForCompilableSource(source))
    );

    if (holder.get() == null) {
      throw new CompilerException("Compiler error, the parsed module must be present for " + source.getFileName());
    } else {
      final var parsedModule = holder.get();
      final var phaseListener = new ResolveDefineInferredTypeListener(parsedModule);
      final var walker = new ParseTreeWalker();

      walker.walk(phaseListener, source.getCompilationUnitContext());
      listener.accept(new CompilationEvent(thisPhase, parsedModule, source));
    }

  }
}
