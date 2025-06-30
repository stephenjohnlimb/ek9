package org.ek9introspection;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.ek9tooling.Ek9ConstrainedType;
import org.ek9tooling.Ek9EnumType;
import org.ek9tooling.Ek9ParameterisedType;

/**
 * Deals with both the constrained types and the enumeration types.
 */
class TypeIntrospector extends Introspector implements Consumer<Map<Class<?>, Map<String, Class<?>>>> {

  private final ValueFromConstruct valueFromConstruct = new ValueFromConstruct();
  private final ValueFromProperty valueFromProperty = new ValueFromProperty();

  TypeIntrospector(final PrintStream printStream) {
    super(printStream);
  }

  @Override
  public void accept(final Map<Class<?>, Map<String, Class<?>>> constructs) {
    final var enums = constructs.get(Ek9EnumType.class);
    final var constrainedTypes = constructs.get(Ek9ConstrainedType.class);
    final var parameterisedTypes = constructs.get(Ek9ParameterisedType.class);

    //If there are none of either then don;t output anything.
    if ((enums == null || enums.isEmpty())
        && (constrainedTypes == null || constrainedTypes.isEmpty())
        && (parameterisedTypes == null || parameterisedTypes.isEmpty())) {
      return;
    }

    printStream.printf("%n  defines type%n");

    if (parameterisedTypes != null) {
      parameterisedTypes.values().stream()
          .map(cls -> cls.getAnnotationsByType(Ek9ParameterisedType.class))
          .flatMap(Arrays::stream)
          .map(Ek9ParameterisedType::value)
          .sorted()
          .forEach(declaration -> printStream.printf(formatFormalDeclaration(declaration)));

      printStream.printf("%n");
    }

    if (enums != null) {
      enums.keySet().stream().sorted()
          .forEach(key -> introspectEnumClass(printStream, enums.get(key)));
    }

    if (constrainedTypes != null) {

      constrainedTypes.keySet().stream().sorted()
          .forEach(key -> introspectClass(printStream, constrainedTypes.get(key)));
    }
  }

  private void introspectEnumClass(final PrintStream printStream, final Class<?> cls) {

    final var declaration = valueFromConstruct.apply(cls);

    declaration.ifPresent(constructDeclaration -> {

      printStream.println(formatFormalDeclaration(constructDeclaration));

      //Do NOT sort, use the order declared as the order in the EK9 enumeration.
      Arrays.stream(cls.getDeclaredFields())
          .map(valueFromProperty)
          .flatMap(Optional::stream)
          .forEach(propertyName -> printStream.printf(formatDeclaration(propertyName)));
    });
  }

}
