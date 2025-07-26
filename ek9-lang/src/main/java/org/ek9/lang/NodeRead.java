package org.ek9.lang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles JSONPath querying for JSON nodes.
 * Converts EK9 path syntax to JSONPath format and executes queries with proper error handling.
 */
final class NodeRead implements java.util.function.BiFunction<
        JsonNode,
        Path,
        _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 apply(final JsonNode jsonNode,
                                                                                        final Path path) {
    final var result = new _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456();

    try {
      // Use JSON Path library to query the JSON structure
      var pathExpression = path._string().state;
      // Convert EK9 paths ($?.somepath) to JSONPath format ($.somepath)
      if (pathExpression.startsWith("$?")) {
        pathExpression = "$" + pathExpression.substring(2); // Replace "$?" with "$"
      }
      var jsonPathContext = com.jayway.jsonpath.JsonPath.parse(jsonNode.toString());
      Object pathResult = jsonPathContext.read(pathExpression);

      return result.asOk(JSON._of(objectMapper.valueToTree(pathResult)));
    } catch (com.jayway.jsonpath.PathNotFoundException e) {
      // JSONPath throws this for non-existent paths
      return result.asError(String._of("Path not found: " + e.getMessage()));
    } catch (com.jayway.jsonpath.InvalidPathException e) {
      // JSONPath throws this for invalid path syntax
      return result.asError(String._of("Invalid path syntax: " + e.getMessage()));
    } catch (java.lang.Exception e) {
      // Catch any other JSONPath or Jackson exceptions
      return result.asError(String._of("JSON Path query failed: " + e.getMessage()));
    }
  }
}