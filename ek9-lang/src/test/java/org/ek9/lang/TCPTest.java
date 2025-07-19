package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for TCP class implementation.
 * Tests both server and client modes, threading, and resource management.
 */
class TCPTest extends Common {

  @Test
  void testDefaultConstructor() {
    final var tcp = new TCP();
    assertNotNull(tcp);

    // Default constructor should create unset TCP
    assertFalse.accept(tcp._isSet());
    assertNotNull(tcp._string());
    assertEquals("", tcp.lastErrorMessage().state);
  }

  @Test
  void testConstructorWithNullProperties() {
    final var tcp = new TCP(null);
    assertNotNull(tcp);

    // Null properties should result in unset TCP
    assertFalse.accept(tcp._isSet());
  }

  @Test
  void testConstructorWithUnsetProperties() {
    final var properties = new NetworkProperties();
    final var tcp = new TCP(properties);
    assertNotNull(tcp);

    // Unset properties should result in unset TCP
    assertFalse.accept(tcp._isSet());
  }

  @Test
  void testServerModeConstructionWithPort() {
    final var properties = new NetworkProperties(Integer._of(0)); // Use port 0 for auto-assign
    final var tcp = new TCP(properties);
    assertNotNull(tcp);

    // Should be set for server mode
    assertTrue.accept(tcp._isSet());
    assertTrue(tcp._string().state.contains("server"));

    tcp._close();
  }

  @Test
  void testServerModeConstructionWithBacklog() {
    final var properties = new NetworkProperties();
    properties.port = Integer._of(0);
    properties.backlog = Integer._of(5);

    final var tcp = new TCP(properties);
    assertNotNull(tcp);

    // Should be set for server mode
    assertTrue.accept(tcp._isSet());
    assertTrue(tcp._string().state.contains("server"));

    tcp._close();
  }

  @Test
  void testServerModeConstructionWithMaxConcurrent() {
    final var properties = new NetworkProperties();
    properties.port = Integer._of(0);
    properties.maxConcurrent = Integer._of(3);

    final var tcp = new TCP(properties);
    assertNotNull(tcp);

    // Should be set for server mode
    assertTrue.accept(tcp._isSet());
    assertTrue(tcp._string().state.contains("server"));

    tcp._close();
  }

  @Test
  void testClientModeConstruction() {
    final var properties = new NetworkProperties(String._of("localhost"), Integer._of(8085));
    final var tcp = new TCP(properties);
    assertNotNull(tcp);

    // Should be set for client mode
    assertTrue.accept(tcp._isSet());
    assertTrue(tcp._string().state.contains("client"));
  }

  @Test
  void testClientModeInvalidProperties() {
    // Missing port
    final var properties1 = new NetworkProperties(String._of("localhost"));
    final var tcp1 = new TCP(properties1);
    assertFalse.accept(tcp1._isSet());
    assertTrue(tcp1.lastErrorMessage().state.contains("port"));

    // Missing host  
    final var properties2 = new NetworkProperties();
    properties2.port = Integer._of(8085);
    properties2.host = new String(); // Unset host
    final var tcp2 = new TCP(properties2);
    // Should default to server mode since no host specified
    assertTrue.accept(tcp2._isSet());
    tcp2._close();
  }

  @Test
  void testServerAcceptWithInvalidHandler() {
    final var properties = new NetworkProperties(Integer._of(0));
    final var tcp = new TCP(properties);

    // Test with null handler
    assertFalse.accept(tcp.accept(null));
    assertTrue(tcp.lastErrorMessage().state.contains("Invalid handler"));

    tcp._close();
  }

  @Test
  void testClientConnectInServerMode() {
    final var properties = new NetworkProperties(Integer._of(0));
    final var tcp = new TCP(properties);

    // Attempting to connect in server mode should fail
    final var connection = tcp.connect();
    assertFalse.accept(connection._isSet());
    assertTrue(tcp.lastErrorMessage().state.contains("server mode"));

    tcp._close();
  }

  @Test
  void testServerAcceptInClientMode() {
    final var properties = new NetworkProperties(String._of("localhost"), Integer._of(8085));
    final var tcp = new TCP(properties);

    final var handler = new TCPHandler();
    assertFalse.accept(tcp.accept(handler));
    assertTrue(tcp.lastErrorMessage().state.contains("client mode"));
  }

  @Test
  void testServerModeLocalOnly() {
    final var properties = new NetworkProperties();
    properties.port = Integer._of(0);
    properties.localOnly = Boolean._of(true);

    final var tcp = new TCP(properties);
    assertTrue.accept(tcp._isSet());
    assertTrue(tcp._string().state.contains("localOnly: true"));

    tcp._close();
  }

  @Test
  void testClose() {
    final var properties = new NetworkProperties(Integer._of(0));
    assertNotNull(properties);
    final var tcp = new TCP(properties);

    assertTrue.accept(tcp._isSet());

    tcp._close();

    // Should be false after close (was constructed then closed)
    assertFalse.accept(tcp._isSet());
  }

  @Test
  void testMultipleClose() {
    final var properties = new NetworkProperties(Integer._of(0));
    final var tcp = new TCP(properties);

    // Multiple closes should be safe
    assertDoesNotThrow(tcp::_close);
    assertDoesNotThrow(tcp::_close);
    assertDoesNotThrow(tcp::_close);

    assertFalse.accept(tcp._isSet());
  }

  @Test
  void testServerClientCommunication() throws Exception, InterruptedException {
    // Start server
    final var serverProperties = new NetworkProperties(Integer._of(0));
    final var server = new TCP(serverProperties);
    assertTrue.accept(server._isSet());

    // Get actual port assigned
    int serverPort = (int) serverProperties.port.state;
    assertTrue(serverPort > 0);

    AtomicBoolean serverCompleted = new AtomicBoolean(false);
    AtomicInteger connectionsReceived = new AtomicInteger(0);
    CountDownLatch serverReady = new CountDownLatch(1);

    // Create handler that echoes input back with "Echo: " prefix
    final var handler = new TCPHandler() {
      @Override
      public void _call(StringInput input, StringOutput output) {
        connectionsReceived.incrementAndGet();
        if (isValid(input) && isValid(output)) {
          while (isTrue(input.hasNext())) {
            final var value = input.next();
            if (isValid(value)) {
              output.println(String._of("Echo: " + value.state));
              if ("shutdown".equals(value.state)) {
                break;
              }
            }
          }
        }
      }

      private boolean isTrue(Boolean value) {
        return value != null && value.isSet && value.state;
      }
    };

    // Start server in background
    Thread serverThread = new Thread(() -> {
      serverReady.countDown();
      boolean result = server.accept(handler).state;
      serverCompleted.set(result);
    });
    serverThread.start();

    // Wait for server to be ready
    assertTrue(serverReady.await(2, TimeUnit.SECONDS));
    Thread.sleep(100); // Give server time to start listening

    try {
      // Test client connection
      final var clientProperties = new NetworkProperties(String._of("localhost"), Integer._of(serverPort));
      final var client = new TCP(clientProperties);
      assertTrue.accept(client._isSet());

      final var connection = client.connect();
      assertTrue.accept(connection._isSet());

      // Send message and receive echo
      connection.output().println(String._of("Hello Server"));
      final var response = connection.input().next();
      assertTrue.accept(response._isSet());
      assertEquals("Echo: Hello Server", response.state);

      // Send shutdown command
      connection.output().println(String._of("shutdown"));

      connection._close();

    } finally {
      // Cleanup
      server._close();
      serverThread.join(2000);
      assertTrue(serverCompleted.get());
      assertTrue(connectionsReceived.get() > 0);
    }
  }

  @Test
  void testClientConnectionFailure() {
    // Try to connect to non-existent server
    final var properties =
        new NetworkProperties(String._of("localhost"), Integer._of(1)); // Port 1 should be unavailable
    assertNotNull(properties);
    final var tcp = new TCP(properties);

    final var connection = tcp.connect();
    assertFalse.accept(connection._isSet());
    assertTrue.accept(tcp.lastErrorMessage()._isSet());
  }

  @Test
  void testConcurrentConnections() throws Exception, InterruptedException {
    // Start server with multiple concurrent connections allowed
    final var serverProperties = new NetworkProperties();
    serverProperties.port = Integer._of(0);
    serverProperties.maxConcurrent = Integer._of(3);
    final var server = new TCP(serverProperties);
    assertTrue.accept(server._isSet());

    int serverPort = (int) serverProperties.port.state;
    AtomicInteger totalConnections = new AtomicInteger(0);
    CountDownLatch serverReady = new CountDownLatch(1);
    CountDownLatch allClientsConnected = new CountDownLatch(3);

    // Handler that counts connections and signals completion
    final var handler = new TCPHandler() {
      @Override
      public void _call(StringInput input, StringOutput output) {
        int connectionId = totalConnections.incrementAndGet();
        allClientsConnected.countDown();

        if (isValid(output)) {
          output.println(String._of("Connection " + connectionId));
        }

        // Wait a bit to simulate processing
        try {
          Thread.sleep(100);
        } catch (InterruptedException _) {
          Thread.currentThread().interrupt();
        }
      }
    };

    // Start server
    Thread serverThread = new Thread(() -> {
      serverReady.countDown();
      server.accept(handler);
    });
    serverThread.start();

    assertTrue(serverReady.await(1, TimeUnit.SECONDS));
    Thread.sleep(100);

    try {
      // Create multiple client connections concurrently
      Thread[] clientThreads = new Thread[3];
      AtomicInteger successfulConnections = new AtomicInteger(0);

      for (int i = 0; i < 3; i++) {
        clientThreads[i] = new Thread(() -> {
          try {
            final var clientProperties = new NetworkProperties(String._of("localhost"), Integer._of(serverPort));
            final var client = new TCP(clientProperties);
            final var connection = client.connect();

            if (connection._isSet().state) {
              successfulConnections.incrementAndGet();
              final var response = connection.input().next();
              assertTrue(response._isSet().state && response.state.startsWith("Connection"));
              connection._close();
            }
          } catch (java.lang.Exception ex) {
            fail("Connection failed:" + ex.getMessage());
          }
        });
        clientThreads[i].start();
      }

      // Wait for all clients to connect
      assertTrue(allClientsConnected.await(3, TimeUnit.SECONDS));

      // Wait for client threads to complete
      for (Thread thread : clientThreads) {
        thread.join(1000);
      }

      assertEquals(3, successfulConnections.get());
      assertEquals(3, totalConnections.get());

    } finally {
      server._close();
      serverThread.join(2000);
    }
  }

  @Test
  void testStringRepresentation() {
    // Unset TCP
    final var unsetTcp = new TCP();
    assertTrue(unsetTcp._string().state.contains("unset"));

    // Server TCP
    final var serverProperties = new NetworkProperties(Integer._of(0));
    final var serverTcp = new TCP(serverProperties);
    String serverString = serverTcp._string();
    assertTrue(serverString.state.contains("server"));
    assertTrue(serverString.state.contains("ready"));
    serverTcp._close();

    // Client TCP
    final var clientProperties = new NetworkProperties(String._of("localhost"), Integer._of(8085));
    final var clientTcp = new TCP(clientProperties);
    String clientString = clientTcp._string();
    assertTrue(clientString.state.contains("client"));
    assertTrue(clientString.state.contains("ready"));
  }
}