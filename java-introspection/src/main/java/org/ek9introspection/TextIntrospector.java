package org.ek9introspection;

import static java.util.stream.Collectors.groupingBy;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import org.ek9tooling.Ek9Text;

/**
 * Because EK9 text constructs are annotated with both a name and a language, the
 * constructs of the same language must be gathered together.
 */
class TextIntrospector extends Introspector implements Consumer<Map<Class<?>, Map<String, Class<?>>>> {

  TextIntrospector(final PrintStream printStream) {
    super(printStream);
  }

  @Override
  public void accept(final Map<Class<?>, Map<String, Class<?>>> constructs) {

    final var allTexts = constructs.get(Ek9Text.class);
    if (allTexts == null || allTexts.isEmpty()) {
      return;
    }

    //Now need to split the 'texts' by language
    final var byLanguage = allTexts
        .values()
        .stream()
        .collect(groupingBy(this::textLanguage));

    //Now it's nice to have the languages in order in the output.
    byLanguage.keySet().stream().sorted().forEach(lang -> {
      printStream.printf("%n  defines text for \"%s\"%n", lang);

      final var texts = byLanguage.get(lang);
      texts.forEach(this::introspectClass);
    });

  }

  private String textLanguage(final Class<?> cls) {
    final var text = Arrays.stream(cls.getAnnotationsByType(Ek9Text.class))
        .findFirst()
        .orElseThrow();

    return text.lang();
  }

}
