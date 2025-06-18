package org.ek9tooling.introspection;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import org.ek9tooling.Ek9Package;

class PackageIntrospector extends Introspector implements Consumer<Map<String, Class<?>>> {

  PackageIntrospector(final PrintStream printStream) {
    super(printStream);
  }

  @Override
  public void accept(final Map<String, Class<?>> packageClasses) {
    exportPackage(packageClasses);
  }

  private void exportPackage(final Map<String, Class<?>> packageClasses) {

    if (packageClasses == null || packageClasses.size() != 1) {
      return;
    }

    printStream.printf("%n  defines package%n");

    //There should only be one.
    packageClasses.values().stream().findFirst().ifPresent(cls -> {
      final var annotation = cls.getAnnotationsByType(Ek9Package.class);
      Arrays.stream(annotation)
          .findFirst()
          .ifPresent(anno -> printStream.printf("%s%n", formatFormalDeclaration(anno.value())));
    });
  }
}
