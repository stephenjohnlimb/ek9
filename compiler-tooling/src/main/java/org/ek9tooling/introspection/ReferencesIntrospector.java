package org.ek9tooling.introspection;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.ek9tooling.Ek9References;

/**
 * Checks all the classes for the Ek9References annotation.
 * Gathers together all the references orders and deduplicates them.
 */
class ReferencesIntrospector extends Introspector implements Consumer<Map<String, Class<?>>> {

  ReferencesIntrospector(final PrintStream printStream) {
    super(printStream);
  }

  @Override
  public void accept(final Map<String, Class<?>> allInPackage) {
    final var references = findReferences(allInPackage);
    exportReferences(references);
  }

  /**
   * Goes through all the classes in the package.
   * finds those with @Ek9References annotation, pulls out the value,
   * splits based on line breaks (because there can be multiple references.
   * Then trims out any spaces, sorts the list, deduplicates the values.
   */
  private List<String> findReferences(final Map<String, Class<?>> allInPackage) {

    return allInPackage
        .values()
        .stream()
        .map(cls -> cls.getAnnotationsByType(Ek9References.class))
        .flatMap(Arrays::stream)
        .map(Ek9References::value)
        .map(refs -> refs.split("\n"))
        .flatMap(Arrays::stream)
        .map(String::trim)
        .sorted()
        .distinct()
        .toList();
  }

  private void exportReferences(final List<String> references) {

    if (references == null || references.isEmpty()) {
      return;
    }
    printStream.printf("%n  references%n");
    references.forEach(reference -> printStream.printf("    %s%n", reference));

  }

}
