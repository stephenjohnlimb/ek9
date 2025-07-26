package org.ek9.lang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Merge a node to the current node, but the implementation varies based upon the first node nature.
 * That nature being array object or value.
 */
final class NodeMerge implements java.util.function.BinaryOperator<JsonNode> {

  private static final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

  @Override
  public JsonNode apply(final JsonNode jsonNode, final JsonNode jsonNode2) {

    // Both are valid, perform merge based on types
    if (jsonNode.isObject() && jsonNode2.isObject()) {
      // Object + Object: only add properties if key is missing
      ObjectNode thisObject = (ObjectNode) jsonNode;
      ObjectNode argObject = (ObjectNode) jsonNode2;

      for (final java.util.Map.Entry<java.lang.String, JsonNode> field : argObject.properties()) {
        java.lang.String key = field.getKey();
        JsonNode value = field.getValue();

        // Only add if key doesn't exist
        if (!thisObject.has(key)) {
          thisObject.set(key, value.deepCopy());
        }
      }
      return jsonNode; //self but mutated.
    }

    if (jsonNode.isArray() && jsonNode2.isArray()) {
      // Array + Array: append elements from argument
      ArrayNode thisArray = (ArrayNode) jsonNode;
      ArrayNode argArray = (ArrayNode) jsonNode2;
      thisArray.addAll(argArray);
      return jsonNode; //again self but mutated
    }

    if (jsonNode.isArray()) {
      // Array + Value: add value to array
      ArrayNode thisArray = (ArrayNode) jsonNode;
      thisArray.add(jsonNode2);
      return jsonNode; //again self but mutated
    }
    // Value + anything: convert to array with both values
    ArrayNode newArray = nodeFactory.arrayNode();
    newArray.add(jsonNode);
    newArray.add(jsonNode2);
    return newArray; //This time we return a new jsonNode to be used.
  }
}
