package org.ek9lang.compiler.main;

import java.util.function.Supplier;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Ek9BuiltinLangSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * A simple shared context of a CompilableProgram with the Ek9 language modules already loaded.
 * i.e. org.ek9.lang and other built in modules.
 * Does not employ any phase listeners or verbose compilation reporting.
 */
public class CompilableProgramSuitable implements Supplier<SharedThreadContext<CompilableProgram>> {
  @Override
  public SharedThreadContext<CompilableProgram> get() {
    Ek9LanguageBootStrap bootStrap =
        new Ek9LanguageBootStrap(new Ek9BuiltinLangSupplier(), (phase, compilableSource) -> {
        }, new CompilerReporter(false));

    return bootStrap.get();
  }
}
