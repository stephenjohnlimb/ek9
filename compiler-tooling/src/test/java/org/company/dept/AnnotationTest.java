package org.company.dept;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import org.ek9tooling.ClassLister;
import org.ek9tooling.Ek9Construct;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Module;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Property;
import org.junit.jupiter.api.Test;

//TODO actually convert into a TEST!
class AnnotationTest {

  private static final String CONSTANT = "constant";
  private static final String PROGRAM = "program";
  private static final String TYPE = "type";
  private static final String FUNCTION = "function";
  private static final String RECORD = "record";
  private static final String TRAIT = "trait";
  private static final String CLASS = "class";
  private static final String TEXT = "text";
  private static final String COMPONENT = "component";
  private static final String APPLICATION = "application";
  private static final String SERVICE = "service";


  @Test
  void testEk9Annotations() {
    final var packageName = "org.company.dept";
    final var packageInfo = "package-info";
    final var ek9ModuleName = packageName + "." + packageInfo;

    final var classLister = new ClassLister();
    final var classes = classLister.findAllClassesUsingClassLoader(packageName);

    final var byConstructType = classLister.findByConstruct(classes);

    if (showModule(ek9ModuleName, classes)) {
      //Now go through the possible construct types in order

      final var programConstructs = byConstructType.get(PROGRAM);
      showProgramConstructs(ek9ModuleName, programConstructs);

      final var traitConstructs = byConstructType.get(TRAIT);
      showConstructs(TRAIT, ek9ModuleName, traitConstructs);

      final var classConstructs = byConstructType.get(CLASS);
      showConstructs(CLASS, ek9ModuleName, classConstructs);

      final var functionConstructs = byConstructType.get(FUNCTION);
      showConstructs(FUNCTION, ek9ModuleName, functionConstructs);

      final var recordConstructs = byConstructType.get(RECORD);
      showConstructs(RECORD, ek9ModuleName, recordConstructs);

      //TODO all the other constructs.
    }
  }

  private static boolean showModule(final String moduleName, final Map<String, Class<?>> classes) {

    if (classes.containsKey(moduleName)) {
      //Then this maybe a valid package we can use for reflection to extract the Ek9
      //Types and declarations.
      final var ek9ModuleDetails = classes.get(moduleName);
      final var moduleAnnotation = ek9ModuleDetails.getAnnotationsByType(Ek9Module.class);
      if (moduleAnnotation.length != 1) {
        System.err.println("Not usable as an extern jar for EK9 expecting Ek9Module");
        return false;
      }
      if (moduleAnnotation[0].value().startsWith("defines extern module")) {
        //Now we can just get the value out of the annotation.
        //i.e. it will be "defines extern module org.ek9.lang" or whatever package/module.
        System.out.println("#!ek9");
        System.out.println(moduleAnnotation[0].value());
      } else {
        System.err.println("Not usable as an extern jar for EK9 expecting 'defines extern module {module-name}'");
        return false;
      }

    } else {
      System.err.println("Not usable as an extern jar for EK9");
      return false;
    }

    return true;
  }

  private void showProgramConstructs(final String moduleName,
                                     final Map<String, Class<?>> classes) {
    if (classes == null || classes.isEmpty()) {
      return;
    }
    System.out.printf("%n  defines program%n");

    classes.keySet().stream().sorted().filter(key -> !key.equals(moduleName))
        .forEach(key -> introspectProgramClass(classes.get(key)));

  }

  private void introspectProgramClass(final Class<?> cls) {

    final var constructs = cls.getAnnotationsByType(Ek9Construct.class);

    if (constructs.length != 1) {
      System.err.println("Expecting one construct in " + cls.getCanonicalName());
      return;
    }

    final var constructors = Arrays.stream(cls.getConstructors())
        .map(constructor -> constructor.getAnnotationsByType(Ek9Constructor.class))
        .flatMap(Arrays::stream)
        .toList();

    constructors.forEach(constructor -> System.out.println(formatFormalDeclaration(constructor.value())));

  }

  private void showConstructs(final String constructType, final String moduleName,
                              final Map<String, Class<?>> classes) {
    if (classes == null || classes.isEmpty()) {
      return;
    }

    System.out.printf("%n  defines %s%n", constructType);


    classes.keySet().stream().sorted().filter(key -> !key.equals(moduleName))
        .forEach(key -> introspectClass(classes.get(key)));
  }

  private void introspectClass(final Class<?> cls) {

    final var constructs = cls.getAnnotationsByType(Ek9Construct.class);

    if (constructs.length != 1) {
      System.err.println("Expecting one construct in " + cls.getCanonicalName());
      return;
    }

    final var constructors = Arrays.stream(cls.getConstructors())
        .map(constructor -> constructor.getAnnotationsByType(Ek9Constructor.class))
        .flatMap(Arrays::stream)
        .toList();

    final var properties = Arrays.stream(cls.getDeclaredFields())
        .map(constructor -> constructor.getAnnotationsByType(Ek9Property.class))
        .flatMap(Arrays::stream).toList();

    final var methods = Arrays.stream(cls.getDeclaredMethods())
        .map(method -> method.getAnnotationsByType(Ek9Method.class))
        .flatMap(Arrays::stream).toList();

    final var operators = Arrays.stream(cls.getDeclaredMethods())
        .map(operator -> operator.getAnnotationsByType(Ek9Operator.class))
        .flatMap(Arrays::stream).toList();

    final var theConstructDeclaration = formatFormalDeclaration(cls.getSimpleName());

    System.out.println(theConstructDeclaration);

    properties.stream()
        .sorted(Comparator.comparing(Ek9Property::value))
        .forEach(method -> System.out.println(formatDeclaration(method.value())));

    constructors.forEach(constructor -> System.out.println(formatDeclaration(constructor.value())));

    methods.stream()
        .sorted(Comparator.comparing(Ek9Method::value))
        .forEach(method -> System.out.println(formatDeclaration(method.value())));

    operators.stream()
        .sorted(Comparator.comparing(Ek9Operator::value))
        .forEach(method -> System.out.println(formatDeclaration(method.value())));

  }

  /**
   * This needs to take into account spacing and new lines embedded in the format
   */
  private String formatFormalDeclaration(final String value) {
    final var formatted = value.replace("\n", "\n    ");
    return "\n    " + formatted;
  }

  private String formatDeclaration(final String value) {
    final var formatted = value.replace("\n", "\n      ");
    return "\n      " + formatted;
  }
}
