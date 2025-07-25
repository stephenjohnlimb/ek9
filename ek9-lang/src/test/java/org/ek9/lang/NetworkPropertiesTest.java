package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NetworkPropertiesTest extends Common {

  @Test
  void testDefaultConstructor() {
    final var props = new NetworkProperties();
    assertNotNull(props);

    // All fields should be unset
    assertUnset.accept(props.host);
    assertUnset.accept(props.port);
    assertUnset.accept(props.packetSize);
    assertUnset.accept(props.timeout);
    assertUnset.accept(props.backlog);
    assertUnset.accept(props.maxConcurrent);
    assertUnset.accept(props.localOnly);

    // Record should be unset when all fields are unset
    assertUnset.accept(props);
    assertUnset.accept(props._hashcode());
  }

  @Test
  void testSingleParameterConstructors() {
    // Test timeout constructor
    final var timeoutProps = new NetworkProperties(Millisecond._of(5000));
    assertNotNull(timeoutProps);

    assertUnset.accept(timeoutProps.host);
    assertUnset.accept(timeoutProps.port);
    assertUnset.accept(timeoutProps.packetSize);
    assertSet.accept(timeoutProps.timeout);
    assertUnset.accept(timeoutProps.backlog);
    assertUnset.accept(timeoutProps.maxConcurrent);
    assertUnset.accept(timeoutProps.localOnly);
    assertSet.accept(timeoutProps); // Record is set because timeout is set

    // Test host constructor
    final var hostProps = new NetworkProperties(String._of("localhost"));
    assertSet.accept(hostProps.host);
    assertUnset.accept(hostProps.port);
    assertUnset.accept(hostProps.packetSize);
    assertUnset.accept(hostProps.timeout);
    assertUnset.accept(hostProps.backlog);
    assertUnset.accept(hostProps.maxConcurrent);
    assertUnset.accept(hostProps.localOnly);
    assertSet.accept(hostProps); // Record is set because host is set

    // Test port constructor
    final var portProps = new NetworkProperties(Integer._of(8080));
    assertUnset.accept(portProps.host);
    assertSet.accept(portProps.port);
    assertUnset.accept(portProps.packetSize);
    assertUnset.accept(portProps.timeout);
    assertUnset.accept(portProps.backlog);
    assertUnset.accept(portProps.maxConcurrent);
    assertUnset.accept(portProps.localOnly);
    assertSet.accept(portProps); // Record is set because port is set
  }

  @Test
  void testMultiParameterConstructors() {
    // Test host and port constructor
    final var hostPortProps = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    assertNotNull(hostPortProps);

    assertSet.accept(hostPortProps.host);
    assertSet.accept(hostPortProps.port);
    assertUnset.accept(hostPortProps.packetSize);
    assertUnset.accept(hostPortProps.timeout);
    assertUnset.accept(hostPortProps.backlog);
    assertUnset.accept(hostPortProps.maxConcurrent);
    assertUnset.accept(hostPortProps.localOnly);
    assertSet.accept(hostPortProps);

    // Test port and packetSize constructor
    final var portPacketProps = new NetworkProperties(Integer._of(8080), Integer._of(1024));
    assertUnset.accept(portPacketProps.host);
    assertSet.accept(portPacketProps.port);
    assertSet.accept(portPacketProps.packetSize);
    assertUnset.accept(portPacketProps.timeout);
    assertUnset.accept(portPacketProps.backlog);
    assertUnset.accept(portPacketProps.maxConcurrent);
    assertUnset.accept(portPacketProps.localOnly);
    assertSet.accept(portPacketProps);

    // Test port and localOnly constructor
    final var portLocalProps = new NetworkProperties(Integer._of(8080), Boolean._of(true));
    assertUnset.accept(portLocalProps.host);
    assertSet.accept(portLocalProps.port);
    assertUnset.accept(portLocalProps.packetSize);
    assertUnset.accept(portLocalProps.timeout);
    assertUnset.accept(portLocalProps.backlog);
    assertUnset.accept(portLocalProps.maxConcurrent);
    assertSet.accept(portLocalProps.localOnly);
    assertSet.accept(portLocalProps);
  }

  @Test
  void testComplexConstructors() {
    // Test 4-parameter constructor
    final var fourParamProps = new NetworkProperties(
        String._of("localhost"),
        Integer._of(8080),
        Integer._of(1024),
        Millisecond._of(5000)
    );
    assertNotNull(fourParamProps);

    assertSet.accept(fourParamProps.host);
    assertSet.accept(fourParamProps.port);
    assertSet.accept(fourParamProps.packetSize);
    assertSet.accept(fourParamProps.timeout);
    assertUnset.accept(fourParamProps.backlog);
    assertUnset.accept(fourParamProps.maxConcurrent);
    assertUnset.accept(fourParamProps.localOnly);
    assertSet.accept(fourParamProps);

    // Test 5-parameter constructor
    final var fiveParamProps = new NetworkProperties(
        Integer._of(8080),
        Millisecond._of(5000),
        Integer._of(50),
        Integer._of(100),
        Boolean._of(false)
    );
    assertUnset.accept(fiveParamProps.host);
    assertSet.accept(fiveParamProps.port);
    assertUnset.accept(fiveParamProps.packetSize);
    assertSet.accept(fiveParamProps.timeout);
    assertSet.accept(fiveParamProps.backlog);
    assertSet.accept(fiveParamProps.maxConcurrent);
    assertSet.accept(fiveParamProps.localOnly);
    assertSet.accept(fiveParamProps);
  }

  @Test
  void testConstructorsWithUnsetValues() {
    // Test constructor with unset values
    final var props = new NetworkProperties(new String(), Integer._of(8080));
    assertNotNull(props);

    assertUnset.accept(props.host); // Should remain unset
    assertSet.accept(props.port); // Should be set
    assertSet.accept(props); // Record should be set because port is set
  }

  @Test
  void testStringRepresentation() {
    // Test unset record
    final var unsetProps = new NetworkProperties();
    assertEquals("NetworkProperties{}", unsetProps._string().state);

    // Test single field
    final var singleProps = new NetworkProperties(String._of("localhost"));
    assertEquals("NetworkProperties{host: localhost}", singleProps._string().state);

    // Test multiple fields
    final var multiProps = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    assertEquals("NetworkProperties{host: localhost, port: 8080}", multiProps._string().state);

    // Test all fields
    final var allProps = new NetworkProperties();
    allProps.host = String._of("localhost");
    allProps.port = Integer._of(8080);
    allProps.packetSize = Integer._of(1024);
    allProps.timeout = Millisecond._of(5000);
    allProps.backlog = Integer._of(50);
    allProps.maxConcurrent = Integer._of(100);
    allProps.localOnly = Boolean._of(true);

    assertEquals(
        "NetworkProperties{host: localhost, port: 8080, packetSize: 1024, timeout: 5000ms, backlog: 50, maxConcurrent: 100, localOnly: true}",
        allProps._string().state);
  }

  @Test
  void testHashCode() {
    // Test unset record
    final var unsetProps = new NetworkProperties();
    assertUnset.accept(unsetProps._hashcode());

    // Test set record
    final var setProps = new NetworkProperties(String._of("localhost"));
    assertSet.accept(setProps._hashcode());

    // Test same values produce same hashcode
    final var props1 = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    final var props2 = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    assertEquals(props1._hashcode().state, props2._hashcode().state);
  }

  @Test
  void testEquality() {
    // Test equality with same values
    final var props1 = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    final var props2 = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    assertNotNull(props1);
    assertNotNull(props2);

    assertTrue.accept(props1._eq(props2));
    assertTrue.accept(props2._eq(props1));

    // Test equality with different values
    final var props3 = new NetworkProperties(String._of("localhost"), Integer._of(8081));
    assertFalse.accept(props1._eq(props3));
    assertFalse.accept(props3._eq(props1));

    // Test equality with unset values
    final var unsetProps1 = new NetworkProperties();
    final var unsetProps2 = new NetworkProperties();
    assertUnset.accept(unsetProps1);
    assertUnset.accept(unsetProps2);
    assertUnset.accept(unsetProps1._eq(unsetProps2));

    // Test equality with mixed set/unset
    final var mixedProps1 = new NetworkProperties(String._of("localhost"));
    final var mixedProps2 = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    assertFalse.accept(mixedProps1._eq(mixedProps2));

    assertUnset.accept(unsetProps1._eq(mixedProps1));
    assertUnset.accept(mixedProps1._eq(unsetProps1));
  }

  @Test
  void testMergeOperator() {
    // Test merge with empty target
    final var target = new NetworkProperties();
    final var source = new NetworkProperties(String._of("localhost"), Integer._of(8080));

    target._merge(source);

    assertEquals("localhost", target.host.state);
    assertEquals(8080, target.port.state);
    assertUnset.accept(target.packetSize);
    assertUnset.accept(target.timeout);
    assertUnset.accept(target.backlog);
    assertUnset.accept(target.maxConcurrent);
    assertUnset.accept(target.localOnly);

    // Test merge doesn't overwrite existing values
    final var existingTarget = new NetworkProperties(String._of("existing"));
    final var newSource = new NetworkProperties(String._of("new"), Integer._of(9000));

    existingTarget._merge(newSource);

    assertEquals("existing", existingTarget.host.state); // Should not be overwritten
    assertEquals(9000, existingTarget.port.state); // Should be set
  }

  @Test
  void testReplaceOperator() {
    // Test replace delegates to merge
    final var target = new NetworkProperties(String._of("existing"));
    final var source = new NetworkProperties(String._of("new"), Integer._of(9000));

    target._replace(source);

    assertEquals("existing", target.host.state); // Should not be overwritten
    assertEquals(9000, target.port.state); // Should be set
  }

  @Test
  void testCopyOperator() {
    // Test full copy
    final var target = new NetworkProperties(String._of("existing"));
    final var source = new NetworkProperties(String._of("new"), Integer._of(9000));

    target._copy(source);

    assertEquals("new", target.host.state); // Should be overwritten
    assertEquals(9000, target.port.state); // Should be set
    assertUnset.accept(target.packetSize); // Should be unset (copied unset value)
  }

  @Test
  void testRecordSetLogic() {
    // Test that record is unset only when ALL fields are unset
    final var props = new NetworkProperties();
    assertNotNull(props);
    assertUnset.accept(props);

    // Setting any field should make record set
    props.host = String._of("localhost");
    assertSet.accept(props);

    // Unsetting the field should make record unset again
    props.host = new String();
    assertUnset.accept(props);

    // Setting a different field should make record set
    props.port = Integer._of(8080);
    assertSet.accept(props);
  }

  @Test
  void testFieldAccess() {
    // Test that all fields are public and accessible
    final var props = new NetworkProperties();

    // Direct field access should work
    props.host = String._of("test");
    props.port = Integer._of(8080);
    props.packetSize = Integer._of(1024);
    props.timeout = Millisecond._of(5000);
    props.backlog = Integer._of(50);
    props.maxConcurrent = Integer._of(100);
    props.localOnly = Boolean._of(true);

    // All fields should be accessible and set
    assertEquals("test", props.host.state);
    assertEquals(8080, props.port.state);
    assertEquals(1024, props.packetSize.state);
    assertEquals(5000, props.timeout.state);
    assertEquals(50, props.backlog.state);
    assertEquals(100, props.maxConcurrent.state);
    assertTrue(props.localOnly.state);
  }

  @Test
  void testNullAndInvalidInputs() {
    // Test constructors with null inputs
    final var nullProps = new NetworkProperties((String) null);
    assertUnset.accept(nullProps.host);
    assertUnset.accept(nullProps); // Record should be unset

    // Test merge with null
    final var props = new NetworkProperties(String._of("test"));
    props._merge(null);
    assertEquals("test", props.host.state); // Should remain unchanged

    // Test copy with null
    props._copy(null);
    assertEquals("test", props.host.state); // Should remain unchanged
  }

  @Test
  void testMissingThreeParameterConstructors() {
    // Test 3-parameter constructor: (String host, Integer port, Millisecond timeout)
    final var hostPortTimeoutProps = new NetworkProperties(
        String._of("localhost"),
        Integer._of(8080),
        Millisecond._of(5000)
    );
    assertNotNull(hostPortTimeoutProps);
    assertSet.accept(hostPortTimeoutProps.host);
    assertSet.accept(hostPortTimeoutProps.port);
    assertUnset.accept(hostPortTimeoutProps.packetSize);
    assertSet.accept(hostPortTimeoutProps.timeout);
    assertUnset.accept(hostPortTimeoutProps.backlog);
    assertUnset.accept(hostPortTimeoutProps.maxConcurrent);
    assertUnset.accept(hostPortTimeoutProps.localOnly);
    assertSet.accept(hostPortTimeoutProps);

    // Test 3-parameter constructor: (String host, Integer port, Integer packetSize)
    final var hostPortPacketProps = new NetworkProperties(
        String._of("localhost"),
        Integer._of(8080),
        Integer._of(1024)
    );
    assertNotNull(hostPortPacketProps);
    assertSet.accept(hostPortPacketProps.host);
    assertSet.accept(hostPortPacketProps.port);
    assertSet.accept(hostPortPacketProps.packetSize);
    assertUnset.accept(hostPortPacketProps.timeout);
    assertUnset.accept(hostPortPacketProps.backlog);
    assertUnset.accept(hostPortPacketProps.maxConcurrent);
    assertUnset.accept(hostPortPacketProps.localOnly);
    assertSet.accept(hostPortPacketProps);
  }

  @Test
  void testMissingFourParameterConstructor() {
    // Test 4-parameter constructor: (Integer port, Integer backlog, Integer maxConcurrent, Boolean localOnly)
    final var portBacklogProps = new NetworkProperties(
        Integer._of(8080),
        Integer._of(50),
        Integer._of(100),
        Boolean._of(true)
    );
    assertNotNull(portBacklogProps);
    assertUnset.accept(portBacklogProps.host);
    assertSet.accept(portBacklogProps.port);
    assertUnset.accept(portBacklogProps.packetSize);
    assertUnset.accept(portBacklogProps.timeout);
    assertSet.accept(portBacklogProps.backlog);
    assertSet.accept(portBacklogProps.maxConcurrent);
    assertSet.accept(portBacklogProps.localOnly);
    assertSet.accept(portBacklogProps);
  }

  @Test
  void testConstructorsWithAllNullParameters() {
    // Test single parameter constructors with null
    final var nullTimeoutProps = new NetworkProperties((Millisecond) null);
    assertNotNull(nullTimeoutProps);

    assertUnset.accept(nullTimeoutProps.timeout);
    assertUnset.accept(nullTimeoutProps);

    final var nullHostProps = new NetworkProperties((String) null);
    assertUnset.accept(nullHostProps.host);
    assertUnset.accept(nullHostProps);

    final var nullPortProps = new NetworkProperties((Integer) null);
    assertUnset.accept(nullPortProps.port);
    assertUnset.accept(nullPortProps);

    // Test multi-parameter constructors with all null
    final var nullHostPortProps = new NetworkProperties((String) null, null);
    assertUnset.accept(nullHostPortProps.host);
    assertUnset.accept(nullHostPortProps.port);
    assertUnset.accept(nullHostPortProps);

    final var nullPortPacketProps = new NetworkProperties((Integer) null, (Integer) null);
    assertUnset.accept(nullPortPacketProps.port);
    assertUnset.accept(nullPortPacketProps.packetSize);
    assertUnset.accept(nullPortPacketProps);
  }

  @Test
  void testConstructorsWithAllUnsetParameters() {
    // Test single parameter constructors with unset values
    final var unsetTimeoutProps = new NetworkProperties(new Millisecond());
    assertNotNull(unsetTimeoutProps);

    assertUnset.accept(unsetTimeoutProps.timeout);
    assertUnset.accept(unsetTimeoutProps);

    final var unsetHostProps = new NetworkProperties(new String());
    assertUnset.accept(unsetHostProps.host);
    assertUnset.accept(unsetHostProps);

    final var unsetPortProps = new NetworkProperties(new Integer());
    assertUnset.accept(unsetPortProps.port);
    assertUnset.accept(unsetPortProps);

    // Test multi-parameter constructors with all unset
    final var unsetHostPortProps = new NetworkProperties(new String(), new Integer());
    assertUnset.accept(unsetHostPortProps.host);
    assertUnset.accept(unsetHostPortProps.port);
    assertUnset.accept(unsetHostPortProps);

    final var unsetPortPacketProps = new NetworkProperties(new Integer(), new Integer());
    assertUnset.accept(unsetPortPacketProps.port);
    assertUnset.accept(unsetPortPacketProps.packetSize);
    assertUnset.accept(unsetPortPacketProps);
  }

  @Test
  void testConstructorsWithMixedNullUnsetValid() {
    // Test mixed null, unset, and valid parameters
    final var mixedProps1 = new NetworkProperties(
        null,        // null
        Integer._of(8080),    // valid
        new Integer()         // unset
    );
    assertNotNull(mixedProps1);

    assertUnset.accept(mixedProps1.host);
    assertSet.accept(mixedProps1.port);
    assertUnset.accept(mixedProps1.packetSize);
    assertSet.accept(mixedProps1); // Should be set because port is valid

    final var mixedProps2 = new NetworkProperties(
        String._of("localhost"),  // valid
        null,           // null
        Integer._of(1024),        // valid
        new Millisecond()         // unset
    );
    assertSet.accept(mixedProps2.host);
    assertUnset.accept(mixedProps2.port);
    assertSet.accept(mixedProps2.packetSize);
    assertUnset.accept(mixedProps2.timeout);
    assertSet.accept(mixedProps2); // Should be set because host and packetSize are valid
  }

  @Test
  void testInequalityOperator() {
    // Test inequality operator
    final var props1 = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    assertNotNull(props1);
    final var props2 = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    final var props3 = new NetworkProperties(String._of("localhost"), Integer._of(8081));

    // Same values should be not unequal
    assertFalse.accept(props1._neq(props2));
    assertFalse.accept(props2._neq(props1));

    // Different values should be unequal
    assertTrue.accept(props1._neq(props3));
    assertTrue.accept(props3._neq(props1));

    // Unset records
    final var unsetProps1 = new NetworkProperties();
    final var unsetProps2 = new NetworkProperties();
    assertUnset.accept(unsetProps1._neq(unsetProps2)); // Both unset
    assertUnset.accept(unsetProps1._neq(props1)); // One unset
  }

  @Test
  void testComparisonOperators() {
    // Test comparison operators
    final var props1 = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    final var props2 = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    final var props3 = new NetworkProperties(String._of("localhost"), Integer._of(8081));

    // Same values should compare as equal (0)
    assertEquals(0, props1._cmp(props2).state);
    assertEquals(0, props2._cmp(props1).state);

    // Different values should return unset (no meaningful ordering)
    assertUnset.accept(props1._cmp(props3));
    assertUnset.accept(props3._cmp(props1));

    // Unset records
    final var unsetProps = new NetworkProperties();
    assertUnset.accept(unsetProps._cmp(props1));
    assertUnset.accept(props1._cmp(unsetProps));
    assertUnset.accept(props1._cmp(String._of("Just a String")));
  }

  @Test
  void testEqualityWithAnyParameter() {
    // Test equality with Any parameter
    final var props1 = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    assertNotNull(props1);
    final var props2 = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    final var props3 = new NetworkProperties(String._of("localhost"), Integer._of(8081));

    // Test with NetworkProperties cast to Any
    Any anyProps2 = props2;
    Any anyProps3 = props3;
    assertTrue.accept(props1._eq(anyProps2));
    assertFalse.accept(props1._eq(anyProps3));

    // Test with non-NetworkProperties Any
    Any stringAny = String._of("test");
    assertUnset.accept(props1._eq(stringAny));

    // Test with null Any
    assertUnset.accept(props1._eq((Any) null));
  }

  @Test
  void testOperatorsWithUnsetRecords() {
    // Test operators with unset records
    final var unsetProps1 = new NetworkProperties();
    assertNotNull(unsetProps1);
    final var unsetProps2 = new NetworkProperties();
    final var setProps = new NetworkProperties(String._of("localhost"));

    // Unset records should not be equal to anything
    assertUnset.accept(unsetProps1._eq(unsetProps2));
    assertUnset.accept(unsetProps1._eq(setProps));
    assertUnset.accept(setProps._eq(unsetProps1));

    // Unset records should not have meaningful comparison
    assertUnset.accept(unsetProps1._cmp(unsetProps2));
    assertUnset.accept(unsetProps1._cmp(setProps));
    assertUnset.accept(setProps._cmp(unsetProps1));

    // Unset records should have unset hashcode
    assertUnset.accept(unsetProps1._hashcode());
    assertUnset.accept(unsetProps2._hashcode());
    assertSet.accept(setProps._hashcode());
  }

  @Test
  void testMergeWithUnsetSource() {
    // Test merge with unset source record
    final var target = new NetworkProperties(String._of("existing"));
    final var unsetSource = new NetworkProperties();

    target._merge(unsetSource);

    // Target should remain unchanged
    assertEquals("existing", target.host.state);
    assertUnset.accept(target.port);
    assertUnset.accept(target.packetSize);
    assertUnset.accept(target.timeout);
    assertUnset.accept(target.backlog);
    assertUnset.accept(target.maxConcurrent);
    assertUnset.accept(target.localOnly);
    assertSet.accept(target);
  }

  @Test
  void testCopyWithUnsetSource() {
    // Test copy with unset source record
    final var target = new NetworkProperties(String._of("existing"));
    assertNotNull(target);
    final var unsetSource = new NetworkProperties();

    target._copy(unsetSource);

    // Target should become unset (copy all unset values)
    assertUnset.accept(target.host);
    assertUnset.accept(target.port);
    assertUnset.accept(target.packetSize);
    assertUnset.accept(target.timeout);
    assertUnset.accept(target.backlog);
    assertUnset.accept(target.maxConcurrent);
    assertUnset.accept(target.localOnly);
    assertUnset.accept(target);
  }

  @Test
  void testReplaceWithUnsetSource() {
    // Test replace with unset source record (should delegate to merge)
    final var target = new NetworkProperties(String._of("existing"));
    final var unsetSource = new NetworkProperties();

    target._replace(unsetSource);

    // Target should remain unchanged (same as merge)
    assertEquals("existing", target.host.state);
    assertUnset.accept(target.port);
    assertUnset.accept(target.packetSize);
    assertUnset.accept(target.timeout);
    assertUnset.accept(target.backlog);
    assertUnset.accept(target.maxConcurrent);
    assertUnset.accept(target.localOnly);
    assertSet.accept(target);
  }

  @Test
  void testFieldEqualityEdgeCases() {
    // Test field equality with different set/unset combinations
    final var props1 = new NetworkProperties();
    assertNotNull(props1);
    final var props2 = new NetworkProperties();
    final var props3 = new NetworkProperties(String._of("localhost"));
    final var props4 = new NetworkProperties(String._of("localhost"));
    final var props5 = new NetworkProperties(String._of("different"));

    // Both fields unset should return unset Boolean
    assertUnset.accept(props1._eq(props2));

    // One field set, one unset should return unset Boolean
    assertUnset.accept(props1._eq(props3));
    assertUnset.accept(props3._eq(props1));

    // Both fields set with same values should be equal
    assertTrue.accept(props3._eq(props4));

    // Both fields set with different values should not be equal
    assertFalse.accept(props3._eq(props5));
  }

  @Test
  void testJsonOperator() {
    // Test unset NetworkProperties returns unset JSON
    final var unset = new NetworkProperties();
    assertUnset.accept(unset._json());

    // Test single property creates proper JSON object
    final var singleProps = new NetworkProperties(String._of("localhost"));
    final var json1 = singleProps._json();
    assertSet.accept(json1);
    
    final var expectedJson1 = """
        {
          "host" : "localhost"
        }""";
    assertEquals(expectedJson1, json1.prettyPrint().state);

    // Test multiple properties create proper JSON with all fields
    final var multiProps = new NetworkProperties(String._of("localhost"), Integer._of(8080));
    final var json2 = multiProps._json();
    assertSet.accept(json2);
    
    final var expectedJson2 = """
        {
          "host" : "localhost",
          "port" : 8080
        }""";
    assertEquals(expectedJson2, json2.prettyPrint().state);

    // Test all properties set
    final var allProps = new NetworkProperties();
    allProps.host = String._of("localhost");
    allProps.port = Integer._of(8080);
    allProps.packetSize = Integer._of(1024);
    allProps.timeout = Millisecond._of(5000);
    allProps.backlog = Integer._of(50);
    allProps.maxConcurrent = Integer._of(100);
    allProps.localOnly = Boolean._of(true);

    final var json3 = allProps._json();
    assertSet.accept(json3);
    
    final var expectedJson3 = """
        {
          "host" : "localhost",
          "port" : 8080,
          "packetSize" : 1024,
          "timeout" : "5000ms",
          "backlog" : 50,
          "maxConcurrent" : 100,
          "localOnly" : true
        }""";
    assertEquals(expectedJson3, json3.prettyPrint().state);

    // Test mixed set/unset properties only include set ones
    final var mixedProps = new NetworkProperties();
    mixedProps.host = String._of("example.com");
    mixedProps.port = Integer._of(9090);
    // packetSize, timeout, backlog, maxConcurrent, localOnly remain unset
    
    final var json4 = mixedProps._json();
    assertSet.accept(json4);
    
    final var expectedJson4 = """
        {
          "host" : "example.com",
          "port" : 9090
        }""";
    assertEquals(expectedJson4, json4.prettyPrint().state);

    // Verify JSON structure and property existence
    assertTrue(json4.objectNature().state); // Verify it's an object
    assertSet.accept(json4.get(String._of("host"))); // Verify host property exists
    assertSet.accept(json4.get(String._of("port"))); // Verify port property exists
    assertUnset.accept(json4.get(String._of("packetSize"))); // Verify packetSize property doesn't exist
    assertUnset.accept(json4.get(String._of("timeout"))); // Verify timeout property doesn't exist
    assertUnset.accept(json4.get(String._of("backlog"))); // Verify backlog property doesn't exist
    assertUnset.accept(json4.get(String._of("maxConcurrent"))); // Verify maxConcurrent property doesn't exist
    assertUnset.accept(json4.get(String._of("localOnly"))); // Verify localOnly property doesn't exist

    // Test that JSON can be parsed back and contains correct values
    assertEquals("example.com", json4.get(String._of("host"))._string().state.replaceAll("\"", ""));
    assertEquals("9090", json4.get(String._of("port"))._string().state);
  }
}