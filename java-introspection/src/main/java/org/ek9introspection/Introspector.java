package org.ek9introspection;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Just functions as a base for introspection of Java classes wrt EK9 annotations.
 * During introspection enables an extern form of the EK9 code to generated from annotated Java code.
 */
abstract class Introspector {

  private static final String NEW_LINE_FOUR_SPACES = "\n    ";
  private static final String SIX_SPACES = "      ";
  private static final String NEW_LINE_SIX_SPACES = "\n      ";

  private final ValueFromConstruct valueFromConstruct = new ValueFromConstruct();
  private final ValueFromProperty valueFromProperty = new ValueFromProperty();

  protected final PrintStream printStream;

  Introspector(final PrintStream printStream) {
    this.printStream = printStream;

  }

  protected void introspectClass(final PrintStream printStream, final Class<?> cls) {

    final var declaration = valueFromConstruct.apply(cls);

    declaration.ifPresent(constructDeclaration -> {
      final var theConstructDeclaration = formatFormalDeclaration(constructDeclaration);
      printStream.println(theConstructDeclaration);

      final var constructors = Arrays.stream(cls.getConstructors())
          .map(constructor -> constructor.getAnnotationsByType(Ek9Constructor.class))
          .flatMap(Arrays::stream)
          .toList();

      final var methods = Arrays.stream(cls.getDeclaredMethods())
          .map(method -> method.getAnnotationsByType(Ek9Method.class))
          .flatMap(Arrays::stream).toList();

      final var operators = Arrays.stream(cls.getDeclaredMethods())
          .map(operator -> operator.getAnnotationsByType(Ek9Operator.class))
          .flatMap(Arrays::stream).toList();

      //Do sort the fields, methods and operators before outputting.
      Arrays.stream(cls.getDeclaredFields())
          .map(valueFromProperty)
          .flatMap(Optional::stream)
          .sorted()
          .map(this::formatDeclaration)
          .forEach(printStream::println);

      constructors.forEach(constructor -> printStream.println(formatDeclaration(constructor.value())));

      methods.stream()
          .sorted(Comparator.comparing(Ek9Method::value))
          .forEach(method -> printStream.println(formatDeclaration(method.value())));

      operators.stream()
          .sorted(Comparator.comparing(Ek9Operator::value))
          .forEach(method -> printStream.println(formatDeclaration(method.value())));

    });
  }

  protected String formatFormalDeclaration(final String value) {

    final var formatted = value.replace("\n", NEW_LINE_FOUR_SPACES);
    return NEW_LINE_FOUR_SPACES + formatted;

  }

  protected String formatDeclaration(final String value) {

    final var formatted = value.replace("\n", NEW_LINE_SIX_SPACES);
    return SIX_SPACES + formatted + "\n";

  }

}
