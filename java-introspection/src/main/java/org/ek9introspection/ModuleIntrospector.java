package org.ek9introspection;

import java.io.PrintStream;
import java.util.Map;
import java.util.function.Function;
import org.ek9tooling.Ek9Module;

/**
 * Checks if an EK9 Module can be exported from a Map of Java classes that have been loaded.
 * If it can then the printStream will contain the module declaration and can be added to with other
 * introspected constructs. If not then the printStream will remain empty.
 */
class ModuleIntrospector extends Introspector implements Function<Map<String, Class<?>>, Ek9InterfaceOrError> {

  private final String moduleName;

  ModuleIntrospector(final PrintStream printStream, final String moduleName) {
    super(printStream);
    this.moduleName = moduleName;
  }

  @Override
  public Ek9InterfaceOrError apply(final Map<String, Class<?>> classes) {

    if (classes.containsKey(moduleName)) {
      //Then this maybe a valid package we can use for reflection to extract the Ek9
      //Types and declarations.
      final var ek9ModuleDetails = classes.get(moduleName);
      final var moduleAnnotation = ek9ModuleDetails.getAnnotationsByType(Ek9Module.class);
      if (moduleAnnotation.length != 1) {
        return new Ek9InterfaceOrError(null, "Not usable as an extern jar for EK9: Expecting Ek9Module");
      }
      if (moduleAnnotation[0].value().startsWith("defines extern module")) {
        //Now we can just get the value out of the annotation.
        //i.e. it will be "defines extern module org.ek9.lang" or whatever package/module.
        printStream.println("""
            #!ek9
            <?-
              Reverse engineered from Java package.
              Do not manually edit.
            -?>""");
        printStream.println(moduleAnnotation[0].value());
      } else {
        return new Ek9InterfaceOrError(null,
            "Not usable as an extern jar for EK9: expecting 'defines extern module {module-name}'");
      }

    } else {
      final var errorMessage = String.format("Not usable as an extern jar for EK9: Cannot find %s", moduleName);
      return new Ek9InterfaceOrError(null, errorMessage);
    }

    return new Ek9InterfaceOrError(printStream.toString(), null);
  }
}
