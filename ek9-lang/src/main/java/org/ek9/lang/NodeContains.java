package org.ek9.lang;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test if the first node contains the second node, but the implementation varies based upon the first node nature.
 * That nature being array object or value.
 */
final class NodeContains implements java.util.function.BiPredicate<JsonNode, JsonNode> {

  @Override
  public boolean test(final JsonNode jsonNode, final JsonNode jsonNode2) {

    if (jsonNode.isArray()) {
      for (JsonNode element : jsonNode) {
        if (element.equals(jsonNode2)) {
          return true;
        }
      }
      return false;
    }

    if (jsonNode.isObject()) {
      return jsonNode.has(jsonNode2.asText());
    }

    return jsonNode.equals(jsonNode2);

  }
}
