package org.ek9lang.compiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;
import org.ek9introspection.Ek9ExternExtractor;
import org.ek9introspection.ValidEk9Interface;
import org.ek9lang.core.CompilerException;

/**
 * Looks for Java classes that have been annotated with the annotations that allow them to be exposed
 * as 'extern' ek9 components.
 */
public class Ek9BuiltinIntrospectionSupplier implements Supplier<List<CompilableSource>> {


  @Override
  public List<CompilableSource> get() {
    return List.of(new CompilableSource(".", "org-ek9-lang.ek9", getOrgEk9LangDeclarations()));
  }

  private InputStream getOrgEk9LangDeclarations() {

    final var validEk9 = new ValidEk9Interface();
    final var ek9ExternExtractor = new Ek9ExternExtractor();

    final var interfaceOrError = ek9ExternExtractor.apply("org.ek9.lang");
    if (!validEk9.test(interfaceOrError)) {
      throw new CompilerException(interfaceOrError.errorMessage());
    }

    return new ByteArrayInputStream(interfaceOrError.ek9Interface().getBytes());
  }
}
