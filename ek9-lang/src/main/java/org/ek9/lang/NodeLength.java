package org.ek9.lang;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * depending on the nature of the json node, this function will work out the length.
 */
final class NodeLength implements java.util.function.ToIntFunction<JsonNode> {

  @Override
  public int applyAsInt(final JsonNode jsonNode) {
    if (jsonNode.isArray() || jsonNode.isObject()) {
      return jsonNode.size();
    } else if (jsonNode.isTextual()) {
      return jsonNode.asText().length();
    }
    return 1;
  }
}
