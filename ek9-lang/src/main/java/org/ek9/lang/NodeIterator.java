package org.ek9.lang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Depending on the nature of the JsonNode this will produce an iterator of any over that node.
 */
final class NodeIterator implements java.util.function.Function<JSON, Iterator> {
  @Override
  public Iterator apply(final JSON json) {


    if (json.jsonNode.isArray()) {
      // Array: iterate over elements as JSON objects
      java.util.Spliterator<JsonNode> spliterator =
          java.util.Spliterators.spliteratorUnknownSize(json.jsonNode.elements(),
              java.util.Spliterator.ORDERED);
      return Iterator._of(java.util.stream.StreamSupport.stream(spliterator, false)
          .map(JSON::_of)
          .map(Any.class::cast)
          .iterator());
    }

    if (json.jsonNode.isObject()) {
      // Object: iterate over key-value pairs as named JSON objects
      ObjectNode objectNode = (ObjectNode) json.jsonNode;
      java.util.Spliterator<java.util.Map.Entry<java.lang.String, JsonNode>> spliterator =
          java.util.Spliterators.spliteratorUnknownSize(objectNode.properties().iterator(),
              java.util.Spliterator.ORDERED);
      return Iterator._of(java.util.stream.StreamSupport.stream(spliterator, false)
          .map(entry -> {
            java.lang.String key = entry.getKey();
            JsonNode valueNode = entry.getValue();
            JSON valueJson = JSON._of(valueNode);
            return (Any) new JSON(String._of(key), valueJson); // Named JSON object
          })
          .iterator());
    }
    // Value: single-element iterator containing this JSON
    return Iterator._of(java.util.List.of((Any) json).iterator());
  }
}
