package org.company.dept;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import org.ek9tooling.ClassLister;
import org.ek9tooling.Ek9Construct;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Module;
import org.junit.jupiter.api.Test;

//TODO actually convert into a TEST!
class AnnotationTest {

  private static final String TRAIT = "trait";
  private static final String CLASS = "class";
  private static final String FUNCTION = "function";

  @Test
  void testEk9Annotations() {
    final var packageName = "org.company.dept";
    final var packageInfo = "package-info";
    final var ek9ModuleName = packageName + "." + packageInfo;

    final var classLister = new ClassLister();
    final var classes = classLister.findAllClassesUsingClassLoader(packageName);

    final var byConstruct = classLister.findByConstruct(classes);

    if (showModule(ek9ModuleName, classes)) {
      //Now go through the possible construct types in order

      final var traitConstructs = byConstruct.get(TRAIT);
      showConstructs(TRAIT, ek9ModuleName, traitConstructs);

      final var classConstructs = byConstruct.get(CLASS);
      showConstructs(CLASS, ek9ModuleName, classConstructs);

      final var functionConstructs = byConstruct.get(FUNCTION);
      showConstructs(FUNCTION, ek9ModuleName, functionConstructs);

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

  private void showConstructs(final String constructType, final String moduleName, final Map<String, Class<?>> classes) {
    if(classes == null || classes.isEmpty()) {
      return;
    }

    System.out.printf("%n  defines %s%n", constructType);

    //Now it would be best to have the constructs come out in alphabetic order
    classes.keySet().stream().sorted().filter(key -> ! key.equals(moduleName)).forEach( key -> introspectClass(classes.get(key)));
  }

  private void introspectClass(final Class<?> cls) {
    //So for this class lets see what sort of construct it is.
    //When we come to do this for real, need to sort constructs for grouping to make
    //generated EK9 interface look nicer.
    final var constructs = cls.getAnnotationsByType(Ek9Construct.class);
    if (constructs.length != 1) {
      System.err.println("Expecting one construct in " + cls.getCanonicalName());
    } else {
      final var theConstructDeclaration = formatConstructDeclaration(constructs[0].value());
      System.out.println(theConstructDeclaration);
    }
    Arrays.stream(cls.getConstructors())
        .map(constructor -> constructor.getAnnotationsByType(Ek9Constructor.class))
        .flatMap(Arrays::stream)
        .forEach(constructor -> System.out.println(formatOperationDeclaration(constructor.value())));

    final var methods = Arrays.stream(cls.getDeclaredMethods())
        .map(constructor -> constructor.getAnnotationsByType(Ek9Method.class))
        .flatMap(Arrays::stream).toList();

    //Ensure operators come after the methods.
    methods.stream()
        .filter(method -> ! method.value().contains("operator "))
        .sorted(Comparator.comparing(Ek9Method::value))
        .forEach(method -> System.out.println(formatOperationDeclaration(method.value())));

    methods.stream().filter(method -> method.value().contains("operator "))
        .sorted(Comparator.comparing(Ek9Method::value))
        .forEach(method -> System.out.println(formatOperationDeclaration(method.value())));

  }

  /** This needs to take into account spacing and new lines embedded in the format */
  private String formatConstructDeclaration(final String value) {
    final var formatted = value.replace("\n", "\n    ");
    return "\n    " + formatted;
  }

  /** This needs to take into account spacing and new lines embedded in the format */
  private String formatOperationDeclaration(final String value) {
    final var formatted = value.replace("\n", "\n      ");
    return "\n      " + formatted;
  }
}
