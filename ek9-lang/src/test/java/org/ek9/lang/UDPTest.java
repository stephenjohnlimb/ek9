package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

/**
 * Comprehensive test suite for UDP class implementation.
 * Tests socket creation, packet sending/receiving, and resource management.
 * Needs to be single threaded as we are accessing ports and also hold the testUDP as an instance variable.
 * Each of the tests may mutate this.
 */
@Execution(SAME_THREAD)
class UDPTest extends Common {

  private UDP testUdp;

  @AfterEach
  void tearDown() {
    if (testUdp != null) {
      testUdp._close();
      testUdp = null;
    }
  }

  @Test
  void testDefaultConstructor() {
    final var udp = new UDP();
    assertNotNull(udp);

    // Default constructor should create unset UDP
    assertUnset.accept(udp);
    assertNotNull(udp._string());
    assertTrue(udp._string().state.contains("UDP{}"));
    assertEquals("", udp.lastErrorMessage().state);
  }

  @Test
  void testConstructorWithNullProperties() {
    final var udp = new UDP(null);
    assertNotNull(udp);

    // Null properties should result in unset UDP
    assertUnset.accept(udp);
    assertTrue(udp.lastErrorMessage().state.contains("Port is required"));
  }

  @Test
  void testConstructorWithUnsetProperties() {
    final var properties = new NetworkProperties();
    final var udp = new UDP(properties);
    assertNotNull(udp);

    // Unset properties (no port) should result in unset UDP
    assertUnset.accept(udp);
    assertTrue(udp.lastErrorMessage().state.contains("Port is required"));
  }

  @Test
  void testConstructorWithPortOnly() {
    final var properties = new NetworkProperties(Integer._of(0)); // Use port 0 for auto-assign
    testUdp = new UDP(properties);
    assertNotNull(testUdp);

    // Should be set when port is provided
    assertSet.accept(testUdp);
    assertTrue(testUdp._string().state.contains("UDP{"));
    assertTrue(testUdp._string().state.contains("bound to"));
    assertEquals("", testUdp.lastErrorMessage().state);
  }

  @Test
  void testConstructorWithLocalOnlyFlag() {
    final var properties = new NetworkProperties(Integer._of(0), Boolean._of(true));
    testUdp = new UDP(properties);
    assertNotNull(testUdp);

    // Should be set and bound to loopback only
    assertSet.accept(testUdp);
    assertTrue(testUdp._string().state.contains("bound to"));
  }

  @Test
  void testConstructorWithTimeout() {
    final var properties = new NetworkProperties(Integer._of(0));
    properties.timeout = Millisecond._of(1000);
    testUdp = new UDP(properties);
    assertNotNull(testUdp);

    assertSet.accept(testUdp);
    assertTrue(testUdp._string().state.contains("bound to"));
  }

  @Test
  void testConstructorWithPacketSize() {
    final var properties = new NetworkProperties(Integer._of(0));
    properties.packetSize = Integer._of(2048);
    testUdp = new UDP(properties);
    assertNotNull(testUdp);

    assertSet.accept(testUdp);
    assertTrue(testUdp._string().state.contains("bound to"));
  }

  @Test
  void testSendPacketValid() {
    // Create UDP socket
    final var properties = new NetworkProperties(INT_0);
    testUdp = new UDP(properties);
    assertSet.accept(testUdp);

    // Create valid packet
    final var packetProperties = new NetworkProperties(STR_LOCALHOST, INT_12345);
    final var packet = new UDPPacket(packetProperties, STR_TEST_MESSAGE);

    // Send should not throw
    assertDoesNotThrow(() -> testUdp.send(packet));
    assertEquals("", testUdp.lastErrorMessage().state);
  }

  @Test
  void testSendPacketInvalidSocket() {
    final var udp = new UDP(); // Unset UDP
    assertUnset.accept(udp);
    final var packet = new UDPPacket();
    assertUnset.accept(packet);

    udp.send(packet);
    assertTrue(udp.lastErrorMessage().state.contains("UDP socket is not available"));
  }

  @Test
  void testSendPacketNull() {
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);

    testUdp.send(null);
    assertTrue(testUdp.lastErrorMessage().state.contains("Invalid packet"));
  }

  @Test
  void testSendPacketMissingDestination() {
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);

    // Packet with port but no host (missing destination host)
    final var packetProperties = new NetworkProperties(INT_12345); // Port only, no host
    final var packet = new UDPPacket(packetProperties, String._of("test"));
    testUdp.send(packet);
    assertTrue(testUdp.lastErrorMessage().state.contains("destination"));
  }

  @Test
  void testPipeOperator() {
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);
    assertSet.accept(testUdp);

    final var packetProperties = new NetworkProperties(STR_LOCALHOST, INT_12345);
    final var packet = new UDPPacket(packetProperties, String._of("Pipe test"));

    // Test pipe operator
    assertDoesNotThrow(() -> testUdp._pipe(packet));
    assertEquals("", testUdp.lastErrorMessage().state);
  }

  @Test
  void testHasNextInitialState() {
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);

    // Initially should have no packets
    assertFalse(testUdp.hasNext().state);
  }

  @Test
  void testHasNextUnsetSocket() {
    final var udp = new UDP();

    // Unset socket should return false
    assertFalse(udp.hasNext().state);
  }

  @Test
  void testNextWithoutPackets() {
    final var properties = new NetworkProperties(Integer._of(0));
    assertNotNull(properties);
    testUdp = new UDP(properties);

    // Should return unset packet when no packets available
    final var packet = testUdp.next();
    assertUnset.accept(packet);
  }

  @Test
  void testNextUnsetSocket() {
    final var udp = new UDP();

    final var packet = udp.next();
    assertUnset.accept(packet);
    assertTrue(udp.lastErrorMessage().state.contains("UDP socket is not available"));
  }

  @Test
  void testReceiveTimeout() {
    final var properties = new NetworkProperties(Integer._of(0));
    assertNotNull(properties);

    properties.timeout = Millisecond._of(100); // Short timeout
    testUdp = new UDP(properties);

    // Should return unset packet after timeout
    final var packet = testUdp.receive();
    assertUnset.accept(packet);
  }

  @Test
  void testReceiveUnsetSocket() {
    final var udp = new UDP();

    final var packet = udp.receive();
    assertUnset.accept(packet);
    assertTrue(udp.lastErrorMessage().state.contains("UDP socket is not available"));
  }

  @Test
  void testSendAndReceiveLoopback() throws Exception {
    // Create UDP socket for sending and receiving
    final var properties = new NetworkProperties(Integer._of(0));

    assertEquals(Integer._of(0), properties.port);
    testUdp = new UDP(properties);
    assertSet.accept(testUdp);
    assertNotEquals(Integer._of(0), properties.port);
    // Get the actual port assigned
    final var actualPort = Integer._of(getActualPort(testUdp));

    // Create packet to send to ourselves
    final var packetProperties = new NetworkProperties(STR_LOCALHOST, actualPort);
    final var content = String._of("Loopback test message");
    final var packet = new UDPPacket(packetProperties, content);

    // Send packet
    testUdp.send(packet);
    assertEquals("", testUdp.lastErrorMessage().state);

    // Wait for packet to be received (up to 2 seconds)
    boolean packetReceived = false;
    for (int i = 0; i < 20; i++) {
      if (testUdp.hasNext().state) {
        packetReceived = true;
        break;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    // Should have packet available
    assertTrue(packetReceived, "Packet should have been received within timeout");

    // Receive packet
    final var receivedPacket = testUdp.next();
    assertSet.accept(receivedPacket);
    assertSet.accept(receivedPacket.content);
    assertEquals("Loopback test message", receivedPacket.content.state);
  }

  @Test
  void testConcurrentSendReceive() throws Exception {
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);
    assertSet.accept(testUdp);

    final var actualPort = Integer._of(getActualPort(testUdp));
    final var latch = new CountDownLatch(5);

    // Send 5 packets concurrently
    for (int i = 0; i < 5; i++) {
      final int messageNumber = i;
      Thread.ofVirtual().start(() -> {
        try {
          final var packetProperties = new NetworkProperties(STR_LOCALHOST, actualPort);
          final var content = String._of("Message " + messageNumber);
          final var packet = new UDPPacket(packetProperties, content);
          testUdp.send(packet);
          latch.countDown();
        } catch (Exception _) {
          // Ignore for test
        }
      });
    }

    // Wait for all sends to complete
    try {
      assertTrue(latch.await(5, TimeUnit.SECONDS));
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }

    // Wait for packets to be received (up to 3 seconds)
    int receivedCount = 0;
    for (int attempt = 0; attempt < 30 && receivedCount == 0; attempt++) {
      while (testUdp.hasNext().state && receivedCount < 5) {
        final var packet = testUdp.next();
        if (packet._isSet().state) {
          receivedCount++;
        }
      }
      if (receivedCount == 0) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException _) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
    assertTrue(receivedCount > 0, "Should have received at least one packet");
  }

  @Test
  void testCloseOperation() {
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);
    assertSet.accept(testUdp);

    // Close should work without error
    testUdp._close();
    assertUnset.accept(testUdp);

    // Operations after close should fail gracefully
    assertFalse(testUdp.hasNext().state);
    assertUnset.accept(testUdp.next());
    assertUnset.accept(testUdp.receive());
  }

  @Test
  void testMultipleCloseOperations() {
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);

    // Multiple closes should not cause issues
    assertDoesNotThrow(() -> {
      testUdp._close();
      testUdp._close();
      testUdp._close();
    });
  }

  @Test
  void testStringRepresentation() {
    // Test unset UDP string
    final var unsetUdp = new UDP();
    assertTrue(unsetUdp._string().state.contains("UDP{}"));

    // Test set UDP string
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);
    assertSet.accept(testUdp);

    final var stringRep = testUdp._string().state;
    assertTrue(stringRep.contains("UDP{"));
    assertTrue(stringRep.contains("bound to"));
    assertTrue(stringRep.contains("queue:"));
    assertTrue(stringRep.contains("NetworkProperties"));
  }

  @Test
  void testLastErrorMessage() {
    final var udp = new UDP();

    // Should start with empty error
    assertEquals("", udp.lastErrorMessage().state);

    // Error should be set after invalid operation
    udp.send(null);
    assertFalse(udp.lastErrorMessage().state.isEmpty());
  }

  @Test
  void testIsSetOperator() {
    // Unset UDP
    final var unsetUdp = new UDP();
    assertNotNull(unsetUdp);

    assertUnset.accept(unsetUdp);

    // Set UDP
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);
    assertSet.accept(testUdp);

    // After close should be unset
    testUdp._close();
    assertUnset.accept(testUdp);
  }

  @Test
  void testResourceCleanup() {
    final var properties = new NetworkProperties(Integer._of(0));
    assertNotNull(properties);
    testUdp = new UDP(properties);
    assertSet.accept(testUdp);

    // Start receiving to create background thread
    testUdp.hasNext();

    // Close should clean up all resources
    testUdp._close();
    assertUnset.accept(testUdp);

    // Give time for cleanup
    try {
      Thread.sleep(50);
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void testErrorHandling() {
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);

    // Test various error conditions
    testUdp.send(null);
    assertTrue(testUdp.lastErrorMessage().state.contains("Invalid packet"));

    final var invalidPacketProperties = new NetworkProperties(INT_12345); // Port only, no host
    final var invalidPacket = new UDPPacket(invalidPacketProperties, String._of("test"));
    testUdp.send(invalidPacket);
    assertTrue(testUdp.lastErrorMessage().state.contains("destination"));

    // Clear error by successful operation
    final var validPacket = new UDPPacket(
        new NetworkProperties(STR_LOCALHOST, INT_12345),
        String._of("Test")
    );
    testUdp.send(validPacket);
    assertEquals("", testUdp.lastErrorMessage().state);
  }

  @Test
  void testPacketQueueBehavior() throws Exception {
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);
    final var actualPort = Integer._of(getActualPort(testUdp));

    // Send multiple packets quickly
    for (int i = 0; i < 3; i++) {
      final var packet = new UDPPacket(
          new NetworkProperties(STR_LOCALHOST, actualPort),
          String._of("Message " + i)
      );
      testUdp.send(packet);
    }

    // Wait for packets to be received (up to 2 seconds)
    int packetCount = 0;
    for (int attempt = 0; attempt < 20 && packetCount == 0; attempt++) {
      while (testUdp.hasNext().state && packetCount < 3) {
        final var packet = testUdp.next();
        if (packet._isSet().state) {
          packetCount++;
        }
      }
      if (packetCount == 0) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException _) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
    assertTrue(packetCount > 0, "Should have received at least one packet");
  }

  @Test
  void testNullSafety() {
    final var properties = new NetworkProperties(Integer._of(0));
    testUdp = new UDP(properties);

    // All methods should handle null gracefully
    assertDoesNotThrow(() -> testUdp.send(null));
    assertDoesNotThrow(() -> testUdp._pipe(null));

    // Constructors should handle null
    assertDoesNotThrow(() -> new UDP(null));
  }

  @Test
  void testEdgeCasesAndBoundaryConditions() {
    // Test with very small timeout
    final var smallTimeoutProps = new NetworkProperties(Integer._of(0));
    assertNotNull(smallTimeoutProps);
    smallTimeoutProps.timeout = Millisecond._of(1);
    final var smallTimeoutUdp = new UDP(smallTimeoutProps);
    assertSet.accept(smallTimeoutUdp);
    smallTimeoutUdp._close();

    // Test with large packet size
    final var largePacketProps = new NetworkProperties(Integer._of(0));
    largePacketProps.packetSize = Integer._of(65507); // Max UDP packet size
    final var largePacketUdp = new UDP(largePacketProps);
    assertSet.accept(largePacketUdp);
    largePacketUdp._close();

    // Test with zero packet size (should use default)
    final var zeroPacketProps = new NetworkProperties(Integer._of(0));
    zeroPacketProps.packetSize = Integer._of(0);
    final var zeroPacketUdp = new UDP(zeroPacketProps);
    assertSet.accept(zeroPacketUdp);
    zeroPacketUdp._close();
  }

  // Helper method to extract actual port from UDP string representation
  private int getActualPort(UDP udp) {
    final var stringRep = udp._string().state;
    final var boundIndex = stringRep.indexOf("bound to ");
    if (boundIndex != -1) {
      final var portStart = boundIndex + "bound to ".length();
      final var portEnd = stringRep.indexOf(",", portStart);
      if (portEnd != -1) {
        return java.lang.Integer.parseInt(stringRep.substring(portStart, portEnd));
      }
    }
    return 0;
  }
}