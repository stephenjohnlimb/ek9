package org.ek9.lang;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import java.io.StringReader;
import java.io.StringWriter;
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
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    JSON as open""")
public class JSON extends BuiltinType {
  //We use tri-state here.
  //But is array is set and true then this is an array '[]'.
  //Else if it is set and false it is a structure '{}'
  //If it is unset then it is just a value.
  Boolean natureOf = new Boolean();

  //We can support the actual value alone or a key value pair.
  java.lang.String name = null;
  java.lang.String state = "";

  @Ek9Constructor("""
      JSON() as pure""")
  public JSON() {
    super.unSet();
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as JSON""")
  public JSON(JSON value) {
    unSet();
    assign(value);
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as Boolean""")
  public JSON(Boolean type) {
    unSet();
    natureOf._copy(type);
    if (type.isSet) {
      //If true then the type is an array, else it is an object.
      if (type.state) {
        assign("[]");
      } else {
        assign("{}");
      }
    }
  }

  @Ek9Constructor("""
      JSON() as pure
        -> arg0 as String""")
  public JSON(String fromValue) {
    unSet();
    if (fromValue.isSet) {
      String trimmed = fromValue.trim();
      if (trimmed.state.startsWith("[") && trimmed.state.endsWith("]")) {
        natureOf._copy(Boolean._of(true));
      }
      if (trimmed.state.startsWith("{") && trimmed.state.endsWith("}")) {
        natureOf._copy(Boolean._of(false));
      }
      assign(trimmed.state);
    }
  }

  @Ek9Constructor("""
      JSON() as pure
        ->
          keyName as String
          value as JSON""")
  public JSON(String keyName, JSON value) {
    unSet();
    assign(value);
    if (keyName.isSet) {
      assignName(keyName.state);
    }
  }


  protected JSON _new() {
    return new JSON();
  }


  /**
   * Is this an array JSON?.
   */
  @Ek9Method("""
      arrayNature() as pure
        <- rtn as Boolean?""")
  public Boolean arrayNature() {
    return Boolean._of(natureOf.isSet && natureOf.state);
  }

  /**
   * Is this a structured object JSON.
   */
  @Ek9Method("""
      objectNature() as pure
        <- rtn as Boolean?""")
  public Boolean objectNature() {
    return Boolean._of(natureOf.isSet && !natureOf.state);
  }

  /**
   * Is this a value, ie not an array nor a structured object; may or may not be named.
   */
  @Ek9Method("""
      valueNature() as pure
        <- rtn as Boolean?""")
  public Boolean valueNature() {
    return Boolean._of(!natureOf.isSet);
  }

  /**
   * Is this a named - sort of property JSON with a value.
   */
  @Ek9Method("""
      named() as pure
        <- rtn as Boolean?""")
  public Boolean named() {
    if (name != null) {
      return Boolean._of(true);
    }
    return Boolean._of(false);
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
    if (natureOf.isSet && natureOf.state) {
      try (JsonReader jr = Json.createReader(new StringReader(this.state))) {
        JsonArray jsonArray = jr.readArray();
        return Integer._of(jsonArray.size());
      } catch (RuntimeException _) {
        //ignore
      }
    }
    return new Integer();
  }


  @Ek9Method("""
      get() as pure
        -> index as Integer
        <- rtn as JSON?""")
  @SuppressWarnings("checkstyle:CatchParameterName")
  public JSON get(Integer index) {
    if (index.isSet && natureOf.isSet && natureOf.state) {
      try (JsonReader jr = Json.createReader(new StringReader(this.state))) {
        JsonArray jsonArray = jr.readArray();
        JsonValue value = jsonArray.get((int) index.state);
        if (value != null) {
          return JSON._of(value.toString());
        }
      } catch (RuntimeException _) {
        //ignore
      }
    }
    return new JSON();
  }

  /**
   * If a named object you will receive the name else unset.
   */
  @Ek9Method("""
      name() as pure
        <- rtn as String?""")
  public String name() {
    if (this.isSet && this.name != null) {
      return String._of("\"" + this.name + "\"");
    }
    return new String();
  }

  @Ek9Method("""
      value() as pure
        <- rtn as JSON?""")
  public JSON value() {
    if (this.isSet && !natureOf.isSet) {
      return JSON._of(this.state);
    }
    return new JSON();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as JSON
        <- rtn as Boolean?""")
  public Boolean _eq(JSON value) {
    if (this.isSet && value != null && value.isSet) {
      return Boolean._of(this.state.equals(value.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as JSON
        <- rtn as Boolean?""")
  public Boolean _neq(JSON value) {
    if (this.isSet && value != null && value.isSet) {
      return Boolean._of(!this.state.equals(value.state));
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    if (isSet) {
      java.lang.String fullyTrimmed = this.state.replaceAll("\\s", "");
      if (!natureOf.isSet) {
        return Boolean._of(fullyTrimmed.isEmpty() || fullyTrimmed.equals("\"\"")); //ie nothing
      }
      return Boolean._of(fullyTrimmed.length() == 2); //ie it is '[]' or '{}'
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    if (isSet) {
      return Integer._of(this.state.length());
    }
    Integer rtn = Integer._of(0);
    rtn.unSet();
    return rtn;
  }

  public int compare(java.lang.String to) {
    return this.state.compareTo(to);
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as JSON
        <- rtn as Integer?""")
  public Integer _cmp(JSON to) {
    if (this.isSet && to != null && to.isSet) {
      return Integer._of(compare(to.state));
    }
    return new Integer();
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
  public JSON _add(JSON value) {
    if (isSet && natureOf.isSet) {
      //So if array or structure we can add a json value on to the end
      //You cannot add a named property type non-object/array or just value into an array
      if (natureOf.state && value.named().state) {
        return new JSON();
      }
      //conversely, you cannot add a non-named JSON into a JSON object
      if (!natureOf.state && !value.named().state) {
        return new JSON();
      }
      java.lang.String asJson = printToJsonString(false).state;
      java.lang.String truncated = asJson.substring(0, asJson.length() - 1);
      StringBuilder buffer = new StringBuilder(truncated);
      if (truncated.length() > 1) {
        buffer.append(',');
      }

      buffer.append(value.printToJsonString(false).state);

      if (natureOf.isSet && natureOf.state) {
        buffer.append(']');
      } else {
        buffer.append('}');
      }
      return JSON._of(buffer.toString());
    }

    return new JSON(value);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as JSON""")
  public void _copy(JSON value) {
    assign(value);
  }

  @Ek9Method("""
      clear()
        <- rtn as JSON?""")
  public JSON clear() {
    name = null;
    state = "";
    super.unSet();
    return this;
  }

  @Ek9Operator("""
      operator #^ as pure
        <- rtn as String?""")
  public String _promote() {
    if (isSet) {
      return String._of(getName() + this.state);
    }
    return String._of(getName() + "null");
  }

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  @Override
  public String _string() {
    return _promote();
  }


  //TODO add in access to property keys as  List of String.

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
    if (this.isSet && property.isSet) {
      java.lang.String trimmed = this.state.trim();
      if (trimmed.startsWith("{")) {
        try (JsonReader jr = Json.createReader(new StringReader(this.state))) {
          JsonObject jsonStructure = jr.readObject();
          JsonValue value = jsonStructure.get(property.state);
          if (value != null) {
            return new JSON(property, JSON._of(value.toString()));
          }
        } catch (RuntimeException _) {
          //ignore.
        }
      }
    }
    return new JSON();
  }

  @Ek9Method("""
      prettyPrint() as pure
        <- rtn as String?""")
  public String prettyPrint() {
    return printToJsonString(true);
  }

  private java.lang.String getName() {
    if (name != null) {
      return "\"" + this.name + "\":";
    }
    return "";
  }

  //TODO add in iterator.

  //TODO add in conversion to list of JSON.

  //TODO add in pipe based processing

  //Start of Utility methods.

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj) && obj instanceof JSON) {
      if (isSet) {
        return toString().equals(obj.toString());
      }
      return true;
    }
    return false;
  }

  private String printToJsonString(boolean pretty) {
    if (this.isSet) {
      java.lang.String trimmed = this.state.trim();
      if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
        java.util.Map<java.lang.String, java.lang.Boolean> config = new java.util.HashMap<>();
        if (pretty) {
          config.put(JsonGenerator.PRETTY_PRINTING, true);
        }

        try (JsonReader jr = Json.createReader(new StringReader(this.state))) {
          JsonWriterFactory jwf = Json.createWriterFactory(config);
          StringWriter sw = new StringWriter();
          try (JsonWriter jsonWriter = jwf.createWriter(sw)) {

            if (trimmed.startsWith("{")) {
              jsonWriter.write(jr.readObject());
            } else {
              jsonWriter.write(jr.readArray());
            }
            return String._of(getName() + sw.toString());
          } catch (RuntimeException _) {
            //ignore.
          }
        }
      } else {
        return String._of(getName() + trimmed);
      }
    }
    return String._of(getName() + "null");
  }

  @Override
  public java.lang.String toString() {
    if (this.isSet) {
      return getName() + this.state;
    }
    return getName();
  }

  private void assign(java.lang.String value) {
    java.lang.String before = state;
    boolean beforeIsValid = isSet;
    state = value;
    set();
    if (!validateConstraints().state) {
      state = before;
      isSet = beforeIsValid;
      throw new RuntimeException("Constraint violation can't change " + state + " to " + value);
    }

  }

  //Consider moving these methods to a Mutable version of String.
  private void assign(JSON value) {
    if (isValid(value)) {
      assign(value.state);
    } else {
      super.unSet();
    }
    assignName(value.name);
  }

  private void assignName(java.lang.String value) {
    this.name = value;
  }

  public static JSON _of(java.lang.String value) {
    return new JSON(String._of(value));
  }

  public static JSON _of(JSON value) {
    JSON rtn = new JSON();
    rtn.assign(value);
    return rtn;
  }

  public static JSON _of(String value) {
    JSON rtn = new JSON();
    //If just null comes in not \"null\" which is actually a String then we leave unset.

    if (value.isSet && value.state.isEmpty()) {
      rtn.assign(value.state);
    }

    return rtn;
  }
}
