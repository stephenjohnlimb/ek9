package org.ek9lang.compiler.common;

import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.Ek9BuiltinLangSupplier;
import org.ek9lang.compiler.Ek9LanguageBootStrap;
import org.ek9lang.core.SharedThreadContext;

/**
 * A simple shared context of a CompilableProgram with the Ek9 language modules already loaded.
 * i.e. org.ek9.lang and other built in modules.
 * Does not employ any phase listeners or verbose compilation reporting.
 */
public class CompilableProgramSuitable implements Supplier<SharedThreadContext<CompilableProgram>> {
  //TODO consider caching/cloning the CompilableProgram that just has ek9 basics in it.
  //TODO then rather than parse all the EK9 source over and over for tests just copy the data structure.
  //TODO for tests this gets called 100 times, so we're parsing the same build in ek9 source 100 times!
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

    var before = System.currentTimeMillis();
    var rtn = bootStrap.get();
    var after = System.currentTimeMillis();

    System.err.println("Bootstrap duration " + (after - before) + " ms");
    return rtn;
  }
}
