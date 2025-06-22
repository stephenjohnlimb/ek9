package org.ek9tooling;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

  public Map<String, Class<?>> findAllClassesUsingClassLoader(String packageName) {
    InputStream stream = ClassLoader.getSystemClassLoader()
        .getResourceAsStream(packageName.replaceAll("[.]", "/"));

    final Function<Class<?>, String> classToName = Class::getCanonicalName;

    if (stream != null) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      return reader.lines()
          .filter(line -> line.endsWith(".class"))
          .map(line -> getClass(line, packageName))
          .filter(this::usableClass)
          .collect(Collectors.toMap(classToName, Function.identity()));
    }
    return new HashMap<>();
  }

  /**
   * Never process any inner Java classes.
   * Those can never be used via EK9
   */
  private Boolean usableClass(Class<?> cls) {
    final var name = cls.getCanonicalName();

    return name != null && !name.contains("$");

  }

  @SuppressWarnings({"java:S112", "checkstyle:CatchParameterName"})
  private Class<?> getClass(String className, String packageName) {
    final var name = packageName + "."
        + className.substring(0, className.lastIndexOf('.'));
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException _) {
      throw new RuntimeException("Failed to load class: " + name);
    }
  }

}
