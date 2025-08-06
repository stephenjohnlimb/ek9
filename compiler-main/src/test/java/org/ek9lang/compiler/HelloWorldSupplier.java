package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Supplier;
import org.ek9lang.compiler.support.PathToSourceFromName;

/**
 * Just loads the hello world ek9 source code and supplies it as Compilable Source.
 */
public class HelloWorldSupplier implements Supplier<CompilableSource> {

  @Override
  public CompilableSource get() {
    var basePath = new PathToSourceFromName().apply("/examples/basics");
    var fullPath = new PathToSourceFromName().apply("/examples/basics/HelloWorld.ek9");
    var helloWorldSource = new CompilableSource(basePath, fullPath);
    assertNotNull(helloWorldSource, "Expecting source to be available");
    return helloWorldSource;
  }
}
