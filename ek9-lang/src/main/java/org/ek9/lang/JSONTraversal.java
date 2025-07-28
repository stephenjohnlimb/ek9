package org.ek9.lang;

import java.util.function.Consumer;

/**
 * Just deals with traversing various and any JSON structure until it gets to a value node.
 * Then when it gets to a value not is triggers _string on that and calls the configured consumer.
 * The consumer can then decide what to do with that string value.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
final class JSONTraversal implements java.util.function.BiConsumer<JSON, java.util.function.Consumer<String>> {
  @Override
  public void accept(final JSON json, final Consumer<String> stringConsumer) {
    if (json != null && json.isSet) {
      processJson(json, stringConsumer);
    }
  }

  private void processJson(final JSON json, final Consumer<String> stringConsumer) {
    if (json.valueNature().state) {
      //We can just get it as a String, because we've found a value, so call the consumer.
      //but ensure its not quoted.
      final var trimmed = json._string().trim(Character._of('"'));
      stringConsumer.accept(trimmed);
    } else if (json.objectNature().state) {
      processObjectNature(json, stringConsumer);
    } else if (json.arrayNature().state) {
      processArrayNature(json, stringConsumer);
    }
  }

  private void processObjectNature(final JSON json, final Consumer<String> stringConsumer) {
    if (json._len().state == 1) {
      //Only one property so we need to just process the value of that property
      processJson(JSON._of(json.jsonNode.values().next()), stringConsumer);
      return;
    }
    final var iter = json.iterator();
    while (iter.hasNext().state) {
      processJson(iter.next(), stringConsumer);
    }
  }

  private void processArrayNature(final JSON json, final Consumer<String> stringConsumer) {
    final var iter = json.iterator();
    while (iter.hasNext().state) {
      //recursive call to get right down to a value, that we might be able to convert.
      processJson(iter.next(), stringConsumer);
    }
  }
}
