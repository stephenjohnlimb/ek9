package org.ek9lang.lsp;

import java.util.List;
import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.DeSerializer;
import org.ek9lang.compiler.Ek9BuiltinIntrospectionSupplier;
import org.ek9lang.compiler.Ek9LanguageBootStrap;
import org.ek9lang.compiler.Serializer;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.SharedThreadContext;

/**
 * Uses various components to load the Ek9 builtin types into a CompilableProgram.
 * This will form the basis of the program in the LSP. But this component holds on to the
 * CompilableProgram in its initial form as bytes.
 * This means that the caller can request this over and over again and always get the same starting point.
 * A CompilableProgram with just the Ek9 builtin types.
 */
final class InitialCompilableProgramSupplier implements Supplier<SharedThreadContext<CompilableProgram>> {

  private byte[] serialisedBuiltinEk9Symbols;

  //Use the introspection version to get the EK9 built-in source code.
  private final Supplier<List<CompilableSource>> sourceSupplier = new Ek9BuiltinIntrospectionSupplier();
  private final CompilerReporter reporter = new CompilerReporter(false, false);
  private final Supplier<CompilationPhaseListener> listener = () -> compilationEvent -> {
    var source = compilationEvent.source();
    if (source.getErrorListener().hasErrors()) {
      source.getErrorListener().getErrors().forEachRemaining(reporter::report);
    }
  };

  @Override
  public SharedThreadContext<CompilableProgram> get() {

    //Lazy load the core Ek9 built-in types into a Compilable program
    //Hold that as byte data - so we only introspect the core libraries once.
    //But do this in the thread safe way.
    synchronized (this) {
      if (serialisedBuiltinEk9Symbols == null) {
        final var bootStrap = new Ek9LanguageBootStrap(sourceSupplier, listener.get(), reporter);
        final var builtinEk9Symbols = bootStrap.get();
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

}
