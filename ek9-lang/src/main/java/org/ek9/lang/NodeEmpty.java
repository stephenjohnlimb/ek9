package org.ek9.lang;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Just checks if the different types of JsonNode are empty.
 *
 */
final class NodeEmpty implements java.util.function.Predicate<JsonNode> {
  @Override
  public boolean test(final JsonNode jsonNode) {
    if (jsonNode.isArray() || jsonNode.isObject()) {
      return jsonNode.isEmpty();
    } else if (jsonNode.isTextual()) {
      return jsonNode.asText().isEmpty();
    } else {
      return jsonNode.isNull();
    }
  }
}
