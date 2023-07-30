package org.ek9lang.compiler.main;

import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.Ek9BuiltinLangSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.SharedThreadContext;

/**
 * A simple shared context of a CompilableProgram with the Ek9 language modules already loaded.
 * i.e. org.ek9.lang and other built in modules.
 * Does not employ any phase listeners or verbose compilation reporting.
 */
public class CompilableProgramSuitable implements Supplier<SharedThreadContext<CompilableProgram>> {
  @Override
  public SharedThreadContext<CompilableProgram> get() {
    Ek9LanguageBootStrap bootStrap =
        new Ek9LanguageBootStrap(new Ek9BuiltinLangSupplier(), compilationEvent -> {
          var source = compilationEvent.source();
          var phase = compilationEvent.phase();
          if (!source.getErrorListener().isErrorFree()) {
            System.err.println("Errors  : " + phase + ", source: " + source.getFileName());
            source.getErrorListener().getErrors().forEachRemaining(System.err::println);
          }
          if (source.getErrorListener().hasDirectiveErrors()) {
            System.err.println("Directiv: " + phase + ", source: " + source.getFileName());
            source.getErrorListener().getDirectiveErrors().forEachRemaining(System.err::println);
          }
        }, new CompilerReporter(false));

    return bootStrap.get();
  }
}
