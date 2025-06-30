package org.ek9introspection;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import org.ek9tooling.Ek9Constant;

/**
 * Searches for class annotated as Ek9Constants and introspects to produce the ek9 interface definition.
 */
class ConstantsIntrospector extends Introspector implements Consumer<Map<String, Class<?>>> {
  public ConstantsIntrospector(final PrintStream printStream) {
    super(printStream);
  }

  @Override
  public void accept(final Map<String, Class<?>> constantsClasses) {


    //If there are none of either then don't output anything.
    if (constantsClasses == null || constantsClasses.isEmpty()) {
      return;
    }

    printStream.printf("%n  defines constant%n");

    constantsClasses
        .values()
        .stream()
        .map(Class::getDeclaredFields)
        .flatMap(Arrays::stream)
        .filter(field -> field.isAnnotationPresent(Ek9Constant.class))
        .filter(field -> java.lang.reflect.Modifier.isStatic(field.getModifiers()))
        .map(field -> field.getAnnotationsByType(Ek9Constant.class))
        .flatMap(Arrays::stream)
        .map(Ek9Constant::value)
        .sorted()
        .forEach(declaration -> printStream.printf(formatFormalDeclaration(declaration)));

    printStream.printf("%n");
  }
}
