package org.ek9.lang;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * A JSON entity, can be a raw thing like a representation of an integer.
 * Or could be a quoted String, or even an array of other JSON entities. But could also be
 * a structured entity.
 * <p>
 * So quite flexible, but a bit complex, if you try and add incompatible JSON to and existing object
 * the result will be unset.
 * </p>
 */

@SuppressWarnings({"checkstyle:MethodName", "checkstyle:CatchParameterName"})
@Ek9Class("""
    JSON as open""")
public class JSON extends BuiltinType {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

  private final NodeEmpty nodeEmpty = new NodeEmpty();
  private final NodeLength nodeLength = new NodeLength();
  private final NodeAdd nodeAdd = new NodeAdd();
  private final NodeMerge nodeMerge = new NodeMerge();
  private final NodeReplace nodeReplace = new NodeReplace();
  private final NodeContains nodeContains = new NodeContains();
  private final NodeIterator nodeIterator = new NodeIterator();
  private final NodeRead nodeRead = new NodeRead();

  JsonNode jsonNode;

  @Ek9Constructor("""
      JSON() as pure""")
  public JSON() {
    unSet();
    this.jsonNode = null;
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Boolean""")
  public JSON(Boolean arg0) {
    this();
    if (isValid(arg0)) {
      set();
      this.jsonNode = nodeFactory.booleanNode(arg0.state);
    }
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as String""")
  public JSON(String arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as JSON""")
  public JSON(JSON arg0) {
    this();
    if (isValid(arg0)) {
      assign(arg0.jsonNode.deepCopy());
    }
  }

  @Ek9Constructor("""
      JSON() as pure
        ->
          name as String
          value as JSON""")
  public JSON(String name, JSON value) {
    if (isValid(name)) {
      super.set();
      ObjectNode objectNode = nodeFactory.objectNode();
      if (isValid(value)) {
        objectNode.set(name.state, value.jsonNode.deepCopy());
      } else {
        // Map unset EK9 value to JSON null
        objectNode.set(name.state, nodeFactory.nullNode());
      }
      this.jsonNode = objectNode;
    }
    // If name is unset, entire JSON remains unset
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Integer""")
  public JSON(Integer arg0) {
    if (isValid(arg0)) {
      set();
      this.jsonNode = nodeFactory.numberNode(arg0.state);
    }
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Float""")
  public JSON(Float arg0) {
    if (isValid(arg0)) {
      set();
      this.jsonNode = nodeFactory.numberNode(arg0.state);
    }
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Character""")
  public JSON(Character arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Date""")
  public JSON(Date arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as DateTime""")
  public JSON(DateTime arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Time""")
  public JSON(Time arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Millisecond""")
  public JSON(Millisecond arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Duration""")
  public JSON(Duration arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Money""")
  public JSON(Money arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Locale""")
  public JSON(Locale arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Dimension""")
  public JSON(Dimension arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Resolution""")
  public JSON(Resolution arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Colour""")
  public JSON(Colour arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Bits""")
  public JSON(Bits arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Path""")
  public JSON(Path arg0) {
    toTextNodeIfValid(arg0);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as RegEx""")
  public JSON(RegEx arg0) {
    toTextNodeIfValid(arg0);
  }

  /**
   * Is this an array JSON?.
   */
  @Ek9Method("""
      arrayNature() as pure
        <- rtn as Boolean?""")
  public Boolean arrayNature() {
    if (hasValidJson()) {
      return Boolean._of(jsonNode.isArray());
    }
    return new Boolean();
  }

  /**
   * Is this a structured object JSON.
   */
  @Ek9Method("""
      objectNature() as pure
        <- rtn as Boolean?""")
  public Boolean objectNature() {
    if (hasValidJson()) {
      return Boolean._of(jsonNode.isObject());
    }
    return new Boolean();
  }

  /**
   * Is this a value, ie not an array nor a structured object, it is just a value.
   */
  @Ek9Method("""
      valueNature() as pure
        <- rtn as Boolean?""")
  public Boolean valueNature() {
    if (hasValidJson()) {
      return Boolean._of(jsonNode.isValueNode());
    }
    return new Boolean();
  }

  /**
   * If an array then how many items in the array.
   *
   * @return Integer of number of items or unset if not an array.
   */
  @Ek9Method("""
      arrayLength() as pure
        <- rtn as Integer?""")
  @SuppressWarnings("checkstyle:CatchParameterName")
  public Integer arrayLength() {
    if (hasValidJson() && jsonNode.isArray()) {
      return Integer._of(jsonNode.size());
    }
    return new Integer();
  }

  @Ek9Method("""
      get() as pure
        -> index as Integer
        <- rtn as JSON?""")
  public JSON get(Integer index) {
    if (hasValidJson() && isValid(index) && jsonNode.isArray()) {
      final JsonNode element = jsonNode.get((int) index.state);
      if (element != null) {
        return JSON._of(element);
      }
    }
    return _new();
  }

  /**
   * Access a property from within the JSON.
   * Now this may return unset JSON if it is not present
   *
   * @param property The name of the property to access within a JSON object.
   * @return The JSON of that property.
   */
  @Ek9Method("""
      get() as pure
        -> property as String
        <- rtn as JSON?""")
  @SuppressWarnings("checkstyle:CatchParameterName")
  public JSON get(String property) {
    if (hasValidJson() && isValid(property) && jsonNode.isObject()) {
      JsonNode element = jsonNode.get(property.state);
      if (element != null) {
        return JSON._of(element);
      }
    }

    return _new();
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(Any arg) {
    if (arg instanceof JSON asJson) {
      return _eq(asJson);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as JSON
        <- rtn as Boolean?""")
  public Boolean _eq(JSON arg) {
    if (!canProcess(arg)) {
      return new Boolean();
    }
    return Boolean._of(jsonNode.equals(arg.jsonNode));
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as JSON
        <- rtn as Boolean?""")
  public Boolean _neq(JSON arg) {
    return _eq(arg)._negate();
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    if (!hasValidJson()) {
      return new Boolean();
    }

    return Boolean._of(nodeEmpty.test(jsonNode));
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    if (!hasValidJson()) {
      return new Integer();
    }
    return Integer._of(nodeLength.applyAsInt(jsonNode));
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as JSON
        <- rtn as Integer?""")
  public Integer _cmp(JSON arg) {
    if (!canProcess(arg)) {
      return new Integer();
    }

    // For JSON comparison, compare string representations
    java.lang.String thisStr = jsonNode.toString();
    java.lang.String argStr = arg.jsonNode.toString();
    return Integer._of(thisStr.compareTo(argStr));
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof JSON asJson) {
      return _cmp(asJson);
    }
    return new Integer();
  }

  /**
   * Remember this creates a new JSON object with the current value plus the new value (if possible).
   * It does not mutate this JSON object.
   * Only meaningful if this JSON object is an array or an object structure.
   * i.e. a [] or a {}.
   * But not if it is just a value or a name value pair.
   * With an [] you can add a value or an object, but not a name/value or another [] directly.
   * With an {} you can add a nameValue pair, but not just a value or just and object or just a []
   */

  @Ek9Operator("""
      operator + as pure
        -> arg as JSON
        <- rtn as JSON?""")
  public JSON _add(JSON arg) {
    if (canProcess(arg)) {
      return JSON._of(nodeAdd.apply(jsonNode, arg.jsonNode));
    }
    return _new();
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg as JSON
        <- rtn as Integer?""")
  public Integer _fuzzy(JSON value) {
    // For JSON, fuzzy comparison is same as regular comparison
    return _cmp(value);
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    if (!hasValidJson()) {
      return new Integer();
    }
    return Integer._of(jsonNode.hashCode());
  }

  @Ek9Operator("""
      operator contains as pure
        -> arg as JSON
        <- rtn as Boolean?""")
  public Boolean _contains(JSON value) {
    if (canProcess(value)) {
      return Boolean._of(nodeContains.test(jsonNode, value.jsonNode));
    }
    return new Boolean();
  }

  /**
   * Create an iterator over this JSON based on its nature.
   * - Array: iterates over elements as JSON objects
   * - Object: iterates over key-value pairs as named JSON objects
   * - Value: iterates over single JSON value
   * - Unset: empty iterator
   */
  @Ek9Method("""
      iterator() as pure
        <- rtn as Iterator of JSON?""")
  public _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C iterator() {
    if (!hasValidJson()) {
      // Return unset iterator when there's nothing to iterate over
      return _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C._of();
    }

    final var baseIterator = nodeIterator.apply(this);
    return _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C._of(baseIterator);
  }

  @Ek9Operator("""
      operator :~:
        -> arg as JSON""")
  public void _merge(JSON arg) {
    if (!isValid(arg)) {
      // If argument is invalid, no change
      return;
    }

    if (!hasValidJson()) {
      // If this is unset, just copy the argument
      assign(arg.jsonNode.deepCopy());
      return;
    }

    //This function may return the same jsonNode or a new one with the current json node inside it.
    jsonNode = nodeMerge.apply(jsonNode, arg.jsonNode);

  }

  @Ek9Operator("""
      operator :^:
        -> arg as JSON""")
  public void _replace(JSON arg) {
    if (hasValidJson() && isValid(arg)) {
      assign(nodeReplace.apply(jsonNode, arg.jsonNode));
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as JSON""")
  public void _pipe(JSON arg) {
    _merge(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as JSON""")
  public void _copy(JSON arg) {
    if (isValid(arg)) {
      // Copy with deep copy of the argument's JsonNode
      assign(arg.jsonNode.deepCopy());
    } else {
      // If argument is invalid, become unset
      assign(null);
    }
  }

  @Ek9Method("""
      clear()
        <- rtn as JSON?""")
  public JSON clear() {
    super.unSet();
    this.jsonNode = null;
    return this;
  }

  @Ek9Operator("""
      operator #^ as pure
        <- rtn as String?""")
  public String _promote() {
    return _string();
  }

  @Override
  @Ek9Operator("""
      operator $$ as pure
        <- rtn as JSON?""")
  public JSON _json() {
    //Seems a bit pointless - but when other code just calls $$ _json via
    //Any then we need this type to return itself.
    return this;
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (!hasValidJson()) {
      return new String();
    }
    try {
      return String._of(objectMapper.writeValueAsString(jsonNode));
    } catch (JsonProcessingException _) {
      return new String();
    }
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  @Ek9Method("""
      prettyPrint() as pure
        <- rtn as String?""")
  public String prettyPrint() {
    if (!hasValidJson()) {
      return new String();
    }

    try {
      return String._of(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
    } catch (JsonProcessingException _) {
      return new String();
    }
  }

  /**
   * Factory method to create a new JSON array.
   */
  @Ek9Method("""
      array() as pure
        <- rtn as JSON?""")
  public JSON array() {
    return JSON._of(nodeFactory.arrayNode());
  }

  /**
   * Factory method to create a new JSON object.
   */
  @Ek9Method("""
      object() as pure
        <- rtn as JSON?""")
  public JSON object() {
    return JSON._of(nodeFactory.objectNode());
  }

  @Ek9Method("""
      parse() as pure
        -> jsonText as String
        <- rtn as Result of (JSON, String)?""")
  public _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 parse(String jsonText) {
    final var result = new _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456();

    if (!isValid(jsonText)) {
      return result.asError(String._of("Invalid jsonText: jsonText is null or unset"));
    }

    try {
      final var parsedNode = objectMapper.readTree(jsonText.state);
      if (parsedNode != null) {
        return result.asOk(JSON._of(parsedNode));
      }
      return result.asError(String._of("Unable to parse jsonText"));
    } catch (JsonProcessingException ex) {
      return result.asError(String._of("Invalid jsonText: " + ex.getMessage()));
    }
  }

  /**
   * Use JSON Path to locate part of the json structure.
   * Returns Result of (JSON, String) for explicit error handling.
   */
  @Ek9Method("""
      read() as pure
        -> arg as Path
        <- rtn as Result of (JSON, String)?""")
  public _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 read(Path path) {
    final var result = new _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456();

    if (!hasValidJson()) {
      return result.asError(String._of("Invalid JSON: JSON object is not set or contains invalid data"));
    }

    if (!isValid(path)) {
      return result.asError(String._of("Invalid Path: Path is null or unset"));
    }

    return nodeRead.apply(jsonNode, path);
  }

  //Start of Utility methods.

  private boolean hasValidJson() {
    return isSet && jsonNode != null;
  }

  @Override
  protected JSON _new() {
    return new JSON();
  }

  private void assign(JsonNode value) {
    if (value == null) {
      super.unSet();
      this.jsonNode = null;
    } else {
      super.set();
      this.jsonNode = value;
    }
  }

  @Override
  public java.lang.String toString() {
    return _string().state;
  }

  private void toTextNodeIfValid(Any any) {
    if (isValid(any)) {
      set();
      this.jsonNode = nodeFactory.textNode(any._string().state);
    } else {
      unSet();
    }
  }

  public static JSON _of(java.lang.String value) {
    JSON result = new JSON();
    final var parsedJsonNode = parse(value);
    if (parsedJsonNode != null) {
      result.jsonNode = parsedJsonNode;
      result.set();
    }
    return result;
  }

  public static JSON _of(String value) {
    return new JSON(value);
  }

  public static JSON _of(JsonNode jsonNode) {
    JSON rtn = new JSON();
    rtn.assign(jsonNode);
    return rtn;
  }

  public static JSON _of(java.util.List<JSON> list) {
    JSON rtn = new JSON();
    final var jsonNode = nodeFactory.arrayNode();
    final var nodes = list.stream().map(json -> json.jsonNode).toList();
    jsonNode.addAll(nodes);
    rtn.assign(jsonNode);
    return rtn;
  }

  private static JsonNode parse(java.lang.String value) {
    if (value != null) {
      try {
        return objectMapper.readTree(value);
      } catch (JsonProcessingException _) {
        //Ignore not parsable.
      }
    }
    return null;
  }
}
