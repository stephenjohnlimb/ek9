package org.ek9lang.compiler.internals;

import java.net.URL;
import java.util.List;
import java.util.function.Supplier;

/**
 * Just loads the ek9 language builtin ek9 lang source code and supplies it as Compilable Source.
 */
public class Ek9BuiltinLangSupplier implements Supplier<List<CompilableSource>> {

  @Override
  public List<CompilableSource> get() {
    final var resourceName = "/builtin/org/ek9/lang/builtin.ek9";
    URL url = Ek9BuiltinLangSupplier.class.getResource(resourceName);
    //Because this is shipped in with the compiler, we can't just open as a file.
    //So we provide the inputStream directly
    return List.of(new CompilableSource(url.getPath(), Ek9BuiltinLangSupplier.class.getResourceAsStream(resourceName)));
  }
}
