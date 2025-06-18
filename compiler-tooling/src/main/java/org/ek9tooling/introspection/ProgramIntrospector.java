package org.ek9tooling.introspection;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Program;

/**
 * Introspects EK9 Programs as defined as Java classes.
 */
class ProgramIntrospector extends Introspector implements Consumer<Map<Class<?>, Map<String, Class<?>>>> {

  ProgramIntrospector(final PrintStream printStream) {
    super(printStream);
  }

  @Override
  public void accept(final Map<Class<?>, Map<String, Class<?>>> constructs) {

    final var programs = constructs.get(Ek9Program.class);
    if (programs == null || programs.isEmpty()) {
      return;
    }

    printStream.printf("%n  defines program%n");

    programs
        .keySet()
        .stream()
        .sorted()
        .forEach(key -> introspectProgramClass(printStream, programs.get(key)));
  }

  private void introspectProgramClass(final PrintStream printStream, final Class<?> cls) {

    Arrays.stream(cls.getConstructors())
        .map(constructor -> constructor.getAnnotationsByType(Ek9Constructor.class))
        .flatMap(Arrays::stream)
        .forEach(constructor -> printStream.println(formatFormalDeclaration(constructor.value())));

  }

}
