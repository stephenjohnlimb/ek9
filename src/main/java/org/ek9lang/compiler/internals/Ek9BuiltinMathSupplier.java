package org.ek9lang.compiler.internals;

import java.net.URL;
import java.util.function.Supplier;

/**
 * Just loads the ek9 language builtin ek9 math source code and supplies it as Compilable Source.
 */
public class Ek9BuiltinMathSupplier implements Supplier<CompilableSource> {

  @Override
  public CompilableSource get() {
    URL url = Ek9BuiltinMathSupplier.class.getResource("/builtin/org/ek9/math/builtin.ek9");
    return new CompilableSource(url.getPath());
  }
}
