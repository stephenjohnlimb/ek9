package org.ek9.lang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Adds a node to the current node, but the implementation varies based upon the first node nature.
 * That nature being array object or value.
 */
final class NodeAdd implements java.util.function.BinaryOperator<JsonNode> {

  private static final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

  @Override
  public JsonNode apply(final JsonNode jsonNode, final JsonNode jsonNode2) {

    // Array + anything: add to array
    if (jsonNode.isArray()) {
      ArrayNode newArray = jsonNode.deepCopy();
      newArray.add(jsonNode2);
      return newArray;
    }

    if (jsonNode.isObject() && jsonNode2.isObject()) {
      // Object + Object: merge objects
      ObjectNode newObject = jsonNode.deepCopy();
      newObject.setAll((ObjectNode) jsonNode2);
      return newObject;
    }

    if (jsonNode.isObject()) {
      // Object + named value (if arg has name): add property
      ObjectNode newObject = jsonNode.deepCopy();
      // For now, add as indexed property
      newObject.set("value_" + newObject.size(), jsonNode2);
      return newObject;
    }

    // Value + Value: create array
    ArrayNode newArray = nodeFactory.arrayNode();
    newArray.add(jsonNode);
    newArray.add(jsonNode2);
    return newArray;

  }
}
