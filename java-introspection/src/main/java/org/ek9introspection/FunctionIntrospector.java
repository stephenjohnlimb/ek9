package org.ek9introspection;

import java.io.PrintStream;
import java.util.Map;
import java.util.function.Consumer;
import org.ek9tooling.Ek9Function;

/**
 * Just deals with Introspecting functions.
 */
class FunctionIntrospector extends Introspector implements Consumer<Map<Class<?>, Map<String, Class<?>>>> {

  private final ValueFromConstruct valueFromConstruct = new ValueFromConstruct();

  FunctionIntrospector(final PrintStream printStream) {
    super(printStream);
  }

  @Override
  public void accept(final Map<Class<?>, Map<String, Class<?>>> constructs) {

    final var allFunctions = constructs.get(Ek9Function.class);
    if (allFunctions == null || allFunctions.isEmpty()) {
      return;
    }

    printStream.printf("%n  defines function%n");

    allFunctions
        .keySet()
        .stream()
        .sorted()
        .forEach(key -> introspectFunction(printStream, allFunctions.get(key)));
  }

  protected void introspectFunction(final PrintStream printStream, final Class<?> cls) {

    final var declaration = valueFromConstruct.apply(cls);

    declaration.ifPresent(constructDeclaration -> {
      final var theConstructDeclaration = formatFormalDeclaration(constructDeclaration);
      printStream.println(theConstructDeclaration);

    });
  }
}