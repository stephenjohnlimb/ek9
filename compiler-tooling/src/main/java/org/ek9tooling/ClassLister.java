package org.ek9tooling;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Used to get the classes by package name.
 * Then subsequently enables then to be separated by their EK9 construct type.
 */
public class ClassLister {


  @SuppressWarnings("checkstyle:LambdaParameterName")
  public Map<String, Map<String, Class<?>>> findByConstruct(final Map<String, Class<?>> allInPackage) {

    final HashMap<String, Map<String, Class<?>>> rtn = new HashMap<>();
    for (var construct : allInPackage.entrySet()) {
      //Might not be a construct could be the package level (i.e. the module).
      final var possibleConstruct = construct.getValue().getAnnotationsByType(Ek9Construct.class);
      final var isConstruct = possibleConstruct.length == 1;
      if (isConstruct) {
        final var constructType = possibleConstruct[0].value();
        final var constructMap = rtn.computeIfAbsent(constructType, _ -> new HashMap<>());
        constructMap.put(construct.getKey(), construct.getValue());
      }
    }

    return rtn;
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
          .collect(Collectors.toMap(classToName, Function.identity()));
    }
    return new HashMap<>();
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
