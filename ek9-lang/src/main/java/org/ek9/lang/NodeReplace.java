package org.ek9.lang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Replace a node to the current node, but the implementation varies based upon the first node nature.
 * That nature being array object or value.
 */
final class NodeReplace implements java.util.function.BinaryOperator<JsonNode> {

  @Override
  public JsonNode apply(final JsonNode jsonNode, final JsonNode jsonNode2) {

    // Both are valid, perform replace based on types
    if (jsonNode.isObject() && jsonNode2.isObject()) {
      // Object + Object: only replace properties if key already exists
      ObjectNode thisObject = (ObjectNode) jsonNode;
      ObjectNode argObject = (ObjectNode) jsonNode2;

      for (final java.util.Map.Entry<java.lang.String, JsonNode> field : argObject.properties()) {
        java.lang.String key = field.getKey();
        JsonNode value = field.getValue();

        // Only replace if key already exists
        if (thisObject.has(key)) {
          thisObject.set(key, value.deepCopy());
        }
      }
      return jsonNode;
    }
    // For non-object types, do full replacement (existing behavior)
    return jsonNode2;

  }
}
