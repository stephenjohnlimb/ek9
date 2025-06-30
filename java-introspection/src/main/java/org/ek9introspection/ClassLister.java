package org.ek9introspection;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ek9tooling.Ek9Application;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Component;
import org.ek9tooling.Ek9Constants;
import org.ek9tooling.Ek9ConstrainedType;
import org.ek9tooling.Ek9EnumType;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Package;
import org.ek9tooling.Ek9ParameterisedType;
import org.ek9tooling.Ek9Program;
import org.ek9tooling.Ek9Record;
import org.ek9tooling.Ek9Service;
import org.ek9tooling.Ek9Text;
import org.ek9tooling.Ek9Trait;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

/**
 * Used to get the classes by package name.
 * Then subsequently enables then to be separated by their EK9 construct type.
 */
public class ClassLister {

  /**
   * Set of valid Ek9 constructs.
   */
  private final Set<Class<? extends Annotation>> ek9Constructs = Set.of(
      Ek9Application.class,
      Ek9Class.class,
      Ek9Component.class,
      Ek9Constants.class,
      Ek9Function.class,
      Ek9Package.class,
      Ek9ParameterisedType.class,
      Ek9Program.class,
      Ek9Record.class,
      Ek9Service.class,
      Ek9Text.class,
      Ek9Trait.class,
      Ek9EnumType.class,
      Ek9ConstrainedType.class
  );


  @SuppressWarnings("checkstyle:LambdaParameterName")
  public Map<Class<?>, Map<String, Class<?>>> findByConstruct(final Map<String, Class<?>> allInPackage) {

    final HashMap<Class<?>, Map<String, Class<?>>> rtn = new HashMap<>();
    for (var construct : allInPackage.entrySet()) {
      //Might not be a construct could be the package level (i.e. the module).

      final var possibleConstruct = ek9ConstructFromAnnotation(construct.getValue().getAnnotations());

      possibleConstruct.ifPresent(constructType -> {
        final var constructMap = rtn.computeIfAbsent(constructType, _ -> new HashMap<>());
        constructMap.put(construct.getKey(), construct.getValue());
      });
    }

    return rtn;
  }

  private Optional<Class<?>> ek9ConstructFromAnnotation(Annotation[] annotations) {
    if (annotations == null) {
      return Optional.empty();
    }

    for (var annotation : annotations) {
      final var annotationType = annotation.annotationType();
      if (ek9Constructs.contains(annotationType)) {
        return Optional.of(annotationType);
      }
    }
    return Optional.empty();
  }

  @SuppressWarnings("checkstyle:LambdaParameterName")
  public Map<String, Class<?>> findAllClassesUsingClassLoader(String packageName) {

    Reflections reflections = new Reflections(packageName, Scanners.SubTypes.filterResultsBy(_ -> true));

    final Function<Class<?>, String> classToName = Class::getCanonicalName;

    return reflections.getSubTypesOf(Object.class)
        .stream()
        .filter(this::usableClass)
        .collect(Collectors.toMap(classToName, Function.identity()));

  }

  /**
   * Never process any inner Java classes.
   * Those can never be used via EK9
   */
  private Boolean usableClass(Class<?> cls) {
    final var name = cls.getCanonicalName();
    return name != null && !name.contains("$");

  }

}
