package org.ek9lang.compiler.internals;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Supplier;
import org.ek9lang.compiler.testsupport.PathToSourceFromName;

/**
 * Just loads the ek9 language builtin ek9 source code and supplies it as Compilable Source.
 */
public class Ek9BuiltinSupplier implements Supplier<CompilableSource> {

  @Override
  public CompilableSource get() {
    var fullPath = new PathToSourceFromName().apply("/builtin/org/ek9/lang/builtin.ek9");
    var helloWorldSource = new CompilableSource(fullPath);
    assertNotNull(helloWorldSource, "Expecting source to be available");
    return helloWorldSource;
  }
}
