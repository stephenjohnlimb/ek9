package org.ek9lang.compiler.common;

import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.DeSerializer;
import org.ek9lang.compiler.Ek9BuiltinLangSupplier;
import org.ek9lang.compiler.Ek9LanguageBootStrap;
import org.ek9lang.compiler.Serializer;
import org.ek9lang.core.SharedThreadContext;

/**
 * A simple shared context of a CompilableProgram with the Ek9 language modules already loaded.
 * i.e. org.ek9.lang and other built in modules.
 * Does not employ any phase listeners or verbose compilation reporting.
 * Similar to InitialCompilableProgramSupplier (but in a different context of use).
 */
public class CompilableProgramSupplier implements Supplier<SharedThreadContext<CompilableProgram>> {

  private static byte[] serialisedBuiltinEk9Symbols;

  @Override
  public SharedThreadContext<CompilableProgram> get() {
    return getCompilableProgram();
  }

  /**
   * Makes the compiler if a serialized version is not available.
   * Otherwise, it just deserialized the byte serialized version.
   *
   * @return a compilable program.
   */
  private static synchronized SharedThreadContext<CompilableProgram> getCompilableProgram() {

    synchronized (CompilableProgramSupplier.class) {
      if (serialisedBuiltinEk9Symbols == null) {

        final var builtinEk9Symbols = getCompilableProgramSharedThreadContext();
        var serializer = new Serializer();
        serialisedBuiltinEk9Symbols = serializer.apply(builtinEk9Symbols);
        return builtinEk9Symbols;
      }
    }

    //Outside the thread lock, we can take the block of bytes and deserialize.
    //OK now lets try and reload it.
    var deserializer = new DeSerializer();
    return deserializer.apply(serialisedBuiltinEk9Symbols);
  }

  private static SharedThreadContext<CompilableProgram> getCompilableProgramSharedThreadContext() {
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
        }, new CompilerReporter(false, true));

    return bootStrap.get();
  }

}
