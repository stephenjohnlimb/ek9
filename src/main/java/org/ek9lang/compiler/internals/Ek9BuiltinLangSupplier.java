package org.ek9lang.compiler.internals;

import java.net.URL;
import java.util.function.Supplier;

/**
 * Just loads the ek9 language builtin ek9 lang source code and supplies it as Compilable Source.
 */
public class Ek9BuiltinLangSupplier implements Supplier<CompilableSource> {

  @Override
  public CompilableSource get() {
    URL url = Ek9BuiltinLangSupplier.class.getResource("/builtin/org/ek9/lang/builtin.ek9");
    return new CompilableSource(url.getPath());
  }
}
