package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Property;
import org.ek9tooling.Ek9Record;

/**
 * EK9 Record representing a UDP packet with network properties and content.
 * Used for UDP network communications.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Record("""
    UDPPacket""")
public class UDPPacket extends BuiltinType {

  @Ek9Property("properties as NetworkProperties: NetworkProperties()")
  public NetworkProperties properties = new NetworkProperties();

  @Ek9Property("content as String: String()")
  public String content = new String();

  @Ek9Constructor("""
      UDPPacket() as pure""")
  public UDPPacket() {
    //Default constructor - properties and content are unset by default
    unSet();
  }

  @Ek9Constructor("""
      UDPPacket() as pure
        ->
          properties as NetworkProperties""")
  public UDPPacket(final NetworkProperties properties) {
    if (isValid(properties)) {
      this.properties._copy(properties);
      set();
    }
  }

  @Ek9Constructor("""
      UDPPacket() as pure
        ->
          properties as NetworkProperties
          content as String""")
  public UDPPacket(final NetworkProperties properties, final String content) {
    if (isValid(properties)) {
      this.properties._copy(properties);
    }
    if (isValid(content)) {
      this.content._copy(content);
    }
    // UDPPacket is set if any property is set
    if (this.properties._isSet().state || this.content._isSet().state) {
      set();
    }
  }

  /**
   * Copy constructor.
   */
  @Ek9Constructor("""
      UDPPacket() as pure
        ->
          source as UDPPacket""")
  public UDPPacket(final UDPPacket source) {
    if (isValid(source)) {
      this.properties._copy(source.properties);
      this.content._copy(source.content);
      set();
    }
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(final Any arg) {
    if (arg instanceof UDPPacket other) {
      return _eq(other);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator == as pure
        -> arg as UDPPacket
        <- rtn as Boolean?""")
  public Boolean _eq(final UDPPacket arg) {
    if (!isValid(arg)) {
      return new Boolean();
    }

    final var propertiesEqual = this.properties._eq(arg.properties);
    final var contentEqual = this.content._eq(arg.content);

    if (!propertiesEqual._isSet().state || !contentEqual._isSet().state) {
      return new Boolean();
    }

    return Boolean._of(propertiesEqual.state && contentEqual.state);
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as UDPPacket
        <- rtn as Boolean?""")
  public Boolean _neq(final UDPPacket arg) {
    return _eq(arg)._negate();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as UDPPacket
        <- rtn as Integer?""")
  public Integer _cmp(final UDPPacket arg) {
    if (!isValid(arg)) {
      return new Integer();
    }

    // First compare by properties
    final var propertiesCompare = this.properties._cmp(arg.properties);
    if (!propertiesCompare._isSet().state || propertiesCompare.state != 0) {
      return propertiesCompare;
    }

    // If properties are equal, compare by content
    return this.content._cmp(arg.content);
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(final Any arg) {
    if (arg instanceof UDPPacket other) {
      return _cmp(other);
    }
    return new Integer();
  }


  @Ek9Operator("""
      operator :~:
        -> arg as UDPPacket""")
  public void _merge(final UDPPacket arg) {
    if (isValid(arg)) {
      // Merge properties and content from source UDPPacket
      if (arg.properties._isSet().state) {
        this.properties._merge(arg.properties);
      }
      if (arg.content._isSet().state) {
        this.content._copy(arg.content);
      }
      set();
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as UDPPacket""")
  public void _replace(final UDPPacket arg) {
    if (arg != null) {
      this.properties._copy(arg.properties);
      this.content._copy(arg.content);
      if (arg._isSet().state) {
        set();
      } else {
        unSet();
      }
    }
  }

  @Ek9Operator("""
      operator :=:
        -> arg as UDPPacket""")
  public void _copy(final UDPPacket arg) {
    _replace(arg);
  }

  @Ek9Operator("""
      operator #^ as pure
        <- rtn as String?""")
  public String _promote() {
    return _string();
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    if (!isSet) {
      return new Integer();
    }

    final var propertiesHash = this.properties._hashcode();
    final var contentHash = this.content._hashcode();

    if (!propertiesHash._isSet().state || !contentHash._isSet().state) {
      return new Integer();
    }

    return Integer._of(java.util.Objects.hash(propertiesHash.state, contentHash.state));
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (!isSet) {
      return new String();
    }

    final var propertiesStr = this.properties._string();
    final var contentStr = this.content._string();

    final var propertiesDisplay = propertiesStr._isSet().state ? propertiesStr.state : "unset";
    final var contentDisplay = contentStr._isSet().state ? contentStr.state : "unset";

    return String._of("UDPPacket{properties=" + propertiesDisplay + ", content=\"" + contentDisplay + "\"}");
  }

}