package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UDPPacketTest extends Common {

  @Test
  void testDefaultConstructor() {
    final var packet = new UDPPacket();
    assertNotNull(packet);

    // All fields should be unset
    assertUnset.accept(packet.properties);
    assertUnset.accept(packet.content);

    // Record should be unset when all fields are unset
    assertUnset.accept(packet);
    assertUnset.accept(packet._hashcode());
  }

  @Test
  void testNetworkPropertiesConstructor() {
    // Test with set NetworkProperties
    final var properties = new NetworkProperties(STR_LOCALHOST, INT_8080);
    final var packet = new UDPPacket(properties);
    assertNotNull(packet);

    assertSet.accept(packet.properties);
    assertUnset.accept(packet.content);
    assertSet.accept(packet); // Record is set because properties is set

    // Test with unset NetworkProperties
    final var unsetPacket = new UDPPacket(new NetworkProperties());
    assertUnset.accept(unsetPacket.properties);
    assertUnset.accept(unsetPacket.content);
    assertUnset.accept(unsetPacket);

    // Test with null NetworkProperties
    assertDoesNotThrow(() -> new UDPPacket((NetworkProperties) null));
    final var nullPacket = new UDPPacket((NetworkProperties) null);
    assertUnset.accept(nullPacket.properties);
    assertUnset.accept(nullPacket.content);
    assertUnset.accept(nullPacket);
  }

  @Test
  void testFullConstructor() {
    // Test with both properties and content set
    final var properties = new NetworkProperties(STR_LOCALHOST, INT_8080);
    final var content = String._of("Hello UDP");
    final var packet = new UDPPacket(properties, content);
    assertNotNull(packet);

    assertSet.accept(packet.properties);
    assertSet.accept(packet.content);
    assertSet.accept(packet);

    // Test with only properties set
    final var propertiesOnlyPacket = new UDPPacket(properties, new String());
    assertSet.accept(propertiesOnlyPacket.properties);
    assertUnset.accept(propertiesOnlyPacket.content);
    assertSet.accept(propertiesOnlyPacket); // Set because properties is set

    // Test with only content set
    final var contentOnlyPacket = new UDPPacket(new NetworkProperties(), content);
    assertUnset.accept(contentOnlyPacket.properties);
    assertSet.accept(contentOnlyPacket.content);
    assertSet.accept(contentOnlyPacket); // Set because content is set

    // Test with both unset
    final var unsetPacket = new UDPPacket(new NetworkProperties(), new String());
    assertUnset.accept(unsetPacket.properties);
    assertUnset.accept(unsetPacket.content);
    assertUnset.accept(unsetPacket);

    // Test with null parameters
    assertDoesNotThrow(() -> new UDPPacket(null, null));
    final var nullPacket = new UDPPacket(null, null);
    assertUnset.accept(nullPacket.properties);
    assertUnset.accept(nullPacket.content);
    assertUnset.accept(nullPacket);
  }

  @Test
  void testCopyConstructor() {
    // Test copying set packet
    final var properties = new NetworkProperties(STR_LOCALHOST, INT_8080);
    final var originalPacket = new UDPPacket(properties, STR_TEST_MESSAGE);
    final var copiedPacket = new UDPPacket(originalPacket);

    assertSet.accept(copiedPacket.properties);
    assertSet.accept(copiedPacket.content);
    assertSet.accept(copiedPacket);
    assertTrue(originalPacket._eq(copiedPacket).state);

    // Test copying unset packet
    final var unsetPacket = new UDPPacket();
    final var copiedUnsetPacket = new UDPPacket(unsetPacket);
    assertUnset.accept(copiedUnsetPacket.properties);
    assertUnset.accept(copiedUnsetPacket.content);
    assertUnset.accept(copiedUnsetPacket);

    // Test copying null packet
    assertDoesNotThrow(() -> new UDPPacket((UDPPacket) null));
    final var nullCopyPacket = new UDPPacket((UDPPacket) null);
    assertUnset.accept(nullCopyPacket);
  }

  @Test
  void testEqualityOperators() {
    final var properties1 = new NetworkProperties(STR_LOCALHOST, INT_8080);
    final var content1 = String._of("Message");
    final var packet1 = new UDPPacket(properties1, content1);

    final var properties2 = new NetworkProperties(STR_LOCALHOST, INT_8080);
    final var content2 = String._of("Message");
    final var packet2 = new UDPPacket(properties2, content2);

    final var properties3 = new NetworkProperties(String._of("remote"), Integer._of(9090));
    final var content3 = String._of("Different");
    final var packet3 = new UDPPacket(properties3, content3);

    // Test equality between UDPPackets
    assertTrue(packet1._eq(packet2).state);
    assertTrue(packet1._eq((Any) packet2).state);
    assertFalse(packet1._eq(packet3).state);

    // Test inequality between UDPPackets
    assertFalse(packet1._neq(packet2).state);
    assertTrue(packet1._neq(packet3).state);

    // Test with unset values
    final var unsetPacket = new UDPPacket();
    assertUnset.accept(packet1._eq(unsetPacket));
    assertUnset.accept(packet1._eq((Any) unsetPacket));
    assertUnset.accept(packet1._neq(unsetPacket));

    // Test equality with non-UDPPacket Any
    final var stringValue = String._of("test");
    assertUnset.accept(packet1._eq(stringValue));
  }

  @Test
  void testComparisonOperators() {
    final var properties1 = new NetworkProperties(STR_LOCALHOST, INT_8080);
    final var content1 = String._of("Message1");
    final var packet1 = new UDPPacket(properties1, content1);

    final var properties2 = new NetworkProperties(STR_LOCALHOST, INT_8080);
    final var content2 = String._of("Message2");
    final var packet2 = new UDPPacket(properties2, content2);

    final var properties3 = new NetworkProperties(String._of("remote"), Integer._of(9090));
    final var packet3 = new UDPPacket(properties3, String._of("Message"));

    // Test comparison with UDPPacket
    assertEquals(0, packet1._cmp(packet1).state); // Same packet
    assertSet.accept(packet1._cmp(packet2)); // Same properties, different content
    assertTrue(packet1._cmp(packet2).state != 0);

    // Test comparison with Any (UDPPacket)
    // UDPPacket comparison: if properties are same, compares content (meaningful ordering)
    assertSet.accept(packet1._cmp((Any) packet2)); // Different content - String comparison works
    assertTrue(packet1._cmp((Any) packet2).state != 0);

    // packet1 vs packet3: different properties -> NetworkProperties comparison returns unset
    assertUnset.accept(packet1._cmp((Any) packet3)); // Different properties

    // Test with unset values
    final var unsetPacket = new UDPPacket();
    assertUnset.accept(packet1._cmp(unsetPacket));
    assertUnset.accept(packet1._cmp((Any) unsetPacket));

    // Test comparison with non-UDPPacket Any type
    final var stringValue = String._of("test");
    assertUnset.accept(packet1._cmp(stringValue));
  }

  @Test
  void testAssignmentOperators() {
    final var originalProperties = new NetworkProperties(STR_LOCALHOST, INT_8080);
    final var originalContent = String._of("Original");
    final var packet = new UDPPacket(originalProperties, originalContent);

    final var newProperties = new NetworkProperties(String._of("remote"), Integer._of(9090));
    final var newContent = String._of("New");
    final var newPacket = new UDPPacket(newProperties, newContent);

    // Test merge operator
    final var mergePacket = new UDPPacket(originalProperties, originalContent);
    mergePacket._merge(newPacket);
    assertSet.accept(mergePacket.properties);
    assertSet.accept(mergePacket.content);
    assertSet.accept(mergePacket);

    // Test replace operator
    final var replacePacket = new UDPPacket(originalProperties, originalContent);
    replacePacket._replace(newPacket);
    assertSet.accept(replacePacket.properties);
    assertSet.accept(replacePacket.content);
    assertSet.accept(replacePacket);
    assertTrue(replacePacket._eq(newPacket).state);

    // Test copy operator
    final var copyPacket = new UDPPacket(originalProperties, originalContent);
    copyPacket._copy(newPacket);
    assertSet.accept(copyPacket.properties);
    assertSet.accept(copyPacket.content);
    assertSet.accept(copyPacket);
    assertTrue(copyPacket._eq(newPacket).state);

    // Test with unset source packet
    final var unsetPacket = new UDPPacket();
    final var targetPacket = new UDPPacket(originalProperties, originalContent);
    targetPacket._replace(unsetPacket);
    assertUnset.accept(targetPacket);

    // Test with null values
    assertDoesNotThrow(() -> packet._merge(null));
    assertDoesNotThrow(() -> packet._replace(null));
    assertDoesNotThrow(() -> packet._copy(null));
  }

  @Test
  void testUnaryOperators() {
    final var properties = new NetworkProperties(STR_LOCALHOST, INT_8080);
    final var packet = new UDPPacket(properties, STR_TEST_MESSAGE);

    // Test isSet operator
    assertTrue(packet._isSet().state);

    final var unsetPacket = new UDPPacket();
    assertFalse(unsetPacket._isSet().state);

    // Test hashCode operator
    assertSet.accept(packet._hashcode());
    assertUnset.accept(unsetPacket._hashcode());

    // Test that equal packets have equal hash codes
    final var packet2 = new UDPPacket(
        new NetworkProperties(STR_LOCALHOST, INT_8080),
        STR_TEST_MESSAGE
    );
    assertEquals(packet._hashcode().state, packet2._hashcode().state);
  }

  @Test
  void testStringOperators() {
    final var properties = new NetworkProperties(STR_LOCALHOST, INT_8080);
    final var content = String._of("Hello UDP");
    final var packet = new UDPPacket(properties, content);

    // Test string operator
    assertSet.accept(packet._string());
    assertTrue(packet._string().state.contains("UDPPacket"));
    assertTrue(packet._string().state.contains("properties="));
    assertTrue(packet._string().state.contains("content="));

    // Test promote operator
    assertSet.accept(packet._promote());
    assertEquals(packet._string().state, packet._promote().state);

    // Test with unset packet
    final var unsetPacket = new UDPPacket();
    assertUnset.accept(unsetPacket._string());
    assertUnset.accept(unsetPacket._promote());

    // Test with partially set packet (only properties)
    final var partialPacket = new UDPPacket(properties);
    assertSet.accept(partialPacket._string());
    assertTrue(partialPacket._string().state.contains("unset"));
  }

  @Test
  void testEdgeCasesAndNullSafety() {
    // Test all methods with unset packet
    final var unsetPacket = new UDPPacket();

    assertUnset.accept(unsetPacket._eq(null));
    assertUnset.accept(unsetPacket._eq((Any) null));
    assertUnset.accept(unsetPacket._neq(null));
    assertUnset.accept(unsetPacket._cmp(null));
    assertUnset.accept(unsetPacket._cmp((Any) null));

    assertDoesNotThrow(() -> unsetPacket._merge(null));
    assertDoesNotThrow(() -> unsetPacket._replace(null));
    assertDoesNotThrow(() -> unsetPacket._copy(null));

    // Test with mixed set/unset properties
    final var setProperties = new NetworkProperties(STR_LOCALHOST);
    final var unsetContent = new String();
    final var mixedPacket = new UDPPacket(setProperties, unsetContent);
    assertSet.accept(mixedPacket);
    assertSet.accept(mixedPacket._string());

    final var unsetProperties = new NetworkProperties();
    final var setContent = String._of("Content");
    final var mixedPacket2 = new UDPPacket(unsetProperties, setContent);
    assertSet.accept(mixedPacket2);
    assertSet.accept(mixedPacket2._string());
  }

  @Test
  void testRecordBehavior() {
    // Test that UDPPacket follows record semantics:
    // - Record is set if ANY property is set
    // - Record is unset if ALL properties are unset

    // All unset - record unset
    final var allUnsetPacket = new UDPPacket(new NetworkProperties(), new String());
    assertNotNull(allUnsetPacket);
    assertUnset.accept(allUnsetPacket);

    // Properties set, content unset - record set
    final var propertiesSetPacket = new UDPPacket(
        new NetworkProperties(STR_LOCALHOST),
        new String()
    );
    assertSet.accept(propertiesSetPacket);

    // Properties unset, content set - record set
    final var contentSetPacket = new UDPPacket(
        new NetworkProperties(),
        String._of("Content")
    );
    assertSet.accept(contentSetPacket);

    // Both set - record set
    final var bothSetPacket = new UDPPacket(
        new NetworkProperties(STR_LOCALHOST),
        String._of("Content")
    );
    assertSet.accept(bothSetPacket);
  }

  @Test
  void testComplexScenarios() {
    // Test chaining operations with UDPPackets
    final var packet1 = new UDPPacket();
    final var sourcePacket = new UDPPacket(
        new NetworkProperties(STR_LOCALHOST, INT_8080),
        String._of("Test")
    );
    packet1._replace(sourcePacket);
    assertSet.accept(packet1);

    final var packet2 = new UDPPacket();
    packet2._copy(packet1);
    assertTrue(packet1._eq(packet2).state);

    // Test comparison consistency for UDPPackets
    final var packetA = new UDPPacket(
        new NetworkProperties(String._of("hostA"), INT_8080),
        String._of("MessageA")
    );
    final var packetB = new UDPPacket(
        new NetworkProperties(String._of("hostB"), INT_8080),
        String._of("MessageB")
    );
    final var packetC = new UDPPacket(
        new NetworkProperties(String._of("hostC"), INT_8080),
        String._of("MessageC")
    );

    // Transitivity test for comparison - only meaningful when NetworkProperties can be compared
    final var cmpAB = packetA._cmp(packetB);
    final var cmpBC = packetB._cmp(packetC);
    final var cmpAC = packetA._cmp(packetC);

    if (cmpAB._isSet().state && cmpBC._isSet().state && cmpAC._isSet().state) {
      // If A < B and B < C, then A < C (transitivity)
      if (cmpAB.state < 0 && cmpBC.state < 0) {
        assertTrue(cmpAC.state < 0);
      }
    }
  }
}