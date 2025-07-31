package org.ek9.lang;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Property;
import org.ek9tooling.Ek9Record;

/**
 * Used for various network communications.
 * Holds various settings, that relate to network communications.
 * <p>
 * For records, the internal properties are fully accessible.
 * But for extern modules, it's best to declare then in this form below.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Record("""
    NetworkProperties""")
public class NetworkProperties extends BuiltinType {

  private static final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

  @Ek9Property("host as String: String()")
  public String host = new String();

  @Ek9Property("port as Integer: Integer()")
  public Integer port = new Integer();

  @Ek9Property("packetSize as Integer: Integer()")
  public Integer packetSize = new Integer();

  @Ek9Property("timeout as Millisecond: Millisecond()")
  public Millisecond timeout = new Millisecond();

  @Ek9Property("backlog as Integer: Integer()")
  public Integer backlog = new Integer();

  @Ek9Property("maxConcurrent as Integer: Integer()")
  public Integer maxConcurrent = new Integer();

  @Ek9Property("localOnly as Boolean: Boolean()")
  public Boolean localOnly = new Boolean();

  @Ek9Constructor("""
      NetworkProperties() as pure""")
  public NetworkProperties() {
    //Default Constructor.
  }

  @Ek9Constructor("""
      NetworkProperties() as pure
        ->
          timeout as Millisecond""")
  public NetworkProperties(Millisecond timeout) {
    if (isValid(timeout)) {
      this.timeout = new Millisecond(timeout);
    }
  }

  @Ek9Constructor("""
      NetworkProperties() as pure
        ->
          host as String""")
  public NetworkProperties(String host) {
    if (isValid(host)) {
      this.host = new String(host);
    }
  }

  @Ek9Constructor("""
      NetworkProperties() as pure
        ->
          host as String
          port as Integer""")
  public NetworkProperties(String host, Integer port) {
    if (isValid(host)) {
      this.host = new String(host);
    }
    if (isValid(port)) {
      this.port = new Integer(port);
    }
  }

  @Ek9Constructor("""
      NetworkProperties() as pure
        ->
          port as Integer""")
  public NetworkProperties(Integer port) {
    unSet();
    if (isValid(port)) {
      this.port = new Integer(port);
    }
  }

  @Ek9Constructor("""
      NetworkProperties() as pure
        ->
          port as Integer
          packetSize as Integer""")
  public NetworkProperties(Integer port, Integer packetSize) {
    if (isValid(port)) {
      this.port = new Integer(port);
    }
    if (isValid(packetSize)) {
      this.packetSize = new Integer(packetSize);
    }
  }

  @Ek9Constructor("""
      NetworkProperties() as pure
        ->
          port as Integer
          localOnly as Boolean""")
  public NetworkProperties(Integer port, Boolean localOnly) {
    if (isValid(port)) {
      this.port = new Integer(port);
    }
    if (isValid(localOnly)) {
      this.localOnly = new Boolean(localOnly);
    }
  }

  @Ek9Constructor("""
      NetworkProperties() as pure
        ->
          host as String
          port as Integer
          packetSize as Integer""")
  public NetworkProperties(String host, Integer port, Integer packetSize) {
    if (isValid(host)) {
      this.host = new String(host);
    }
    if (isValid(port)) {
      this.port = new Integer(port);
    }
    if (isValid(packetSize)) {
      this.packetSize = new Integer(packetSize);
    }
  }

  @Ek9Constructor("""
      NetworkProperties() as pure
        ->
          host as String
          port as Integer
          timeout as Millisecond""")
  public NetworkProperties(String host, Integer port, Millisecond timeout) {
    if (isValid(host)) {
      this.host = new String(host);
    }
    if (isValid(port)) {
      this.port = new Integer(port);
    }
    if (isValid(timeout)) {
      this.timeout = new Millisecond(timeout);
    }
  }

  @Ek9Constructor("""
      NetworkProperties() as pure
        ->
          host as String
          port as Integer
          packetSize as Integer
          timeout as Millisecond""")
  public NetworkProperties(String host, Integer port, Integer packetSize, Millisecond timeout) {
    if (isValid(host)) {
      this.host = new String(host);
    }
    if (isValid(port)) {
      this.port = new Integer(port);
    }
    if (isValid(packetSize)) {
      this.packetSize = new Integer(packetSize);
    }
    if (isValid(timeout)) {
      this.timeout = new Millisecond(timeout);
    }
  }

  @Ek9Constructor("""
      NetworkProperties() as pure
        ->
          port as Integer
          backlog as Integer
          maxConcurrent as Integer
          localOnly as Boolean""")
  public NetworkProperties(Integer port, Integer backlog, Integer maxConcurrent, Boolean localOnly) {
    if (isValid(port)) {
      this.port = new Integer(port);
    }
    if (isValid(backlog)) {
      this.backlog = new Integer(backlog);
    }
    if (isValid(maxConcurrent)) {
      this.maxConcurrent = new Integer(maxConcurrent);
    }
    if (isValid(localOnly)) {
      this.localOnly = new Boolean(localOnly);
    }
  }

  @Ek9Constructor("""
      NetworkProperties() as pure
        ->
          port as Integer
          timeout as Millisecond
          backlog as Integer
          maxConcurrent as Integer
          localOnly as Boolean""")
  public NetworkProperties(Integer port, Millisecond timeout, Integer backlog, Integer maxConcurrent,
                           Boolean localOnly) {
    if (isValid(port)) {
      this.port = new Integer(port);
    }
    if (isValid(timeout)) {
      this.timeout = new Millisecond(timeout);
    }
    if (isValid(backlog)) {
      this.backlog = new Integer(backlog);
    }
    if (isValid(maxConcurrent)) {
      this.maxConcurrent = new Integer(maxConcurrent);
    }
    if (isValid(localOnly)) {
      this.localOnly = new Boolean(localOnly);
    }
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(calculateIsSet());
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    StringBuilder builder = new StringBuilder();

    if (host.isSet) {
      builder.append("host: ").append(host._string().state);
    }
    if (port.isSet) {
      if (!builder.isEmpty()) {
        builder.append(", ");
      }
      builder.append("port: ").append(port._string().state);
    }
    if (packetSize.isSet) {
      if (!builder.isEmpty()) {
        builder.append(", ");
      }
      builder.append("packetSize: ").append(packetSize._string().state);
    }
    if (timeout.isSet) {
      if (!builder.isEmpty()) {
        builder.append(", ");
      }
      builder.append("timeout: ").append(timeout._string().state);
    }
    if (backlog.isSet) {
      if (!builder.isEmpty()) {
        builder.append(", ");
      }
      builder.append("backlog: ").append(backlog._string().state);
    }
    if (maxConcurrent.isSet) {
      if (!builder.isEmpty()) {
        builder.append(", ");
      }
      builder.append("maxConcurrent: ").append(maxConcurrent._string().state);

    }
    if (localOnly.isSet) {
      if (!builder.isEmpty()) {
        builder.append(", ");
      }
      builder.append("localOnly: ").append(localOnly._string().state);
    }

    return String._of("NetworkProperties{" + builder + "}");
  }

  @Override
  @Ek9Operator("""
      operator $$ as pure
        <- rtn as JSON?""")
  public JSON _json() {
    if (calculateIsSet()) {
      ObjectNode objectNode = nodeFactory.objectNode();
      
      if (host.isSet) {
        objectNode.put("host", host.state);
      }
      if (port.isSet) {
        objectNode.put("port", port.state);
      }
      if (packetSize.isSet) {
        objectNode.put("packetSize", packetSize.state);
      }
      if (timeout.isSet) {
        objectNode.put("timeout", timeout._string().state);
      }
      if (backlog.isSet) {
        objectNode.put("backlog", backlog.state);
      }
      if (maxConcurrent.isSet) {
        objectNode.put("maxConcurrent", maxConcurrent.state);
      }
      if (localOnly.isSet) {
        objectNode.put("localOnly", localOnly.state);
      }
      
      return JSON._of(objectNode);
    }
    return new JSON();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    if (calculateIsSet()) {
      return Integer._of(_string().state.hashCode());
    }
    return new Integer(); // Unset Integer when no properties are set
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(Any arg) {
    if (arg instanceof NetworkProperties asNetworkProperties) {
      return _eq(asNetworkProperties);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator == as pure
        -> arg as NetworkProperties
        <- rtn as Boolean?""")
  public Boolean _eq(NetworkProperties arg) {
    if (arg != null && calculateIsSet() && arg.calculateIsSet()) {
      return Boolean._of(
          fieldsEqual(host, arg.host)
              && fieldsEqual(port, arg.port)
              && fieldsEqual(packetSize, arg.packetSize)
              && fieldsEqual(timeout, arg.timeout)
              && fieldsEqual(backlog, arg.backlog)
              && fieldsEqual(maxConcurrent, arg.maxConcurrent)
              && fieldsEqual(localOnly, arg.localOnly)
      );
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as NetworkProperties
        <- rtn as Boolean?""")
  public Boolean _neq(NetworkProperties arg) {
    return _eq(arg)._negate();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as NetworkProperties
        <- rtn as Integer?""")
  public Integer _cmp(NetworkProperties arg) {
    final var eq = _eq(arg);
    if (eq.isSet && eq.state) {
      return Integer._of(0);
    }
    // No meaningful ordering for NetworkProperties
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof NetworkProperties asNetworkProperties) {
      return _cmp(asNetworkProperties);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as NetworkProperties""")
  public void _merge(NetworkProperties arg) {
    if (isValid(arg)) {
      if (shouldApply(host, arg.host)) {
        host = new String(arg.host);
      }
      if (shouldApply(port, arg.port)) {
        port = new Integer(arg.port);
      }
      if (shouldApply(packetSize, arg.packetSize)) {
        packetSize = new Integer(arg.packetSize);
      }
      if (shouldApply(timeout, arg.timeout)) {
        timeout = new Millisecond(arg.timeout);
      }
      if (shouldApply(backlog, arg.backlog)) {
        backlog = new Integer(arg.backlog);
      }
      if (shouldApply(maxConcurrent, arg.maxConcurrent)) {
        maxConcurrent = new Integer(arg.maxConcurrent);
      }
      if (shouldApply(localOnly, arg.localOnly)) {
        localOnly = new Boolean(arg.localOnly);
      }
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as NetworkProperties""")
  public void _replace(NetworkProperties arg) {
    _merge(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as NetworkProperties""")
  public void _copy(NetworkProperties arg) {
    if (arg != null) {
      host = new String(arg.host);
      port = new Integer(arg.port);
      packetSize = new Integer(arg.packetSize);
      timeout = new Millisecond(arg.timeout);
      backlog = new Integer(arg.backlog);
      maxConcurrent = new Integer(arg.maxConcurrent);
      localOnly = new Boolean(arg.localOnly);
    }
    // No-op if arg is null - leave current state unchanged
  }

  // Start of Utility methods

  private boolean calculateIsSet() {
    return host.isSet
        || port.isSet
        || packetSize.isSet
        || timeout.isSet
        || backlog.isSet
        || maxConcurrent.isSet
        || localOnly.isSet;
  }

  private boolean isValid(NetworkProperties value) {
    return value != null && value._isSet().state;
  }

  private boolean shouldApply(Any localProperty, Any argProperty) {
    final var localPropertySet = localProperty._isSet();
    return localPropertySet.isSet && !localPropertySet.state && isValid(argProperty);
  }

  private boolean fieldsEqual(Any field1, Any field2) {
    if (field1._isSet().state && field2._isSet().state) {
      final var eq = field1._eq(field2);
      return eq._isSet().state && eq.state;
    }
    return field1._isSet().state == field2._isSet().state;
  }
}
