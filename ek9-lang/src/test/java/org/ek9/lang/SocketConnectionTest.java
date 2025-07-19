package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Test the internal SocketConnection class.
 * This tests the Java implementation that supports EK9 TCP functionality.
 */
class SocketConnectionTest extends Common {

  private Socket testSocket;

  @AfterEach
  void cleanup() {
    if (testSocket != null && !testSocket.isClosed()) {
      try {
        testSocket.close();
      } catch (IOException _) {
        // Ignore
      }
    }
  }

  @Test
  void testConstructionWithInvalidSocket() throws IOException {
    assertThrows(Exception.class, () ->
        new SocketConnection(null, new Millisecond()));

    // Test with unconnected socket
    try (Socket unconnectedSocket = new Socket()) {
      assertThrows(Exception.class, () ->
          new SocketConnection(unconnectedSocket, new Millisecond()));
    }
  }

  @Test
  void testConstructionWithValidSocket() throws java.lang.Exception {
    try (ServerSocket server = new ServerSocket(0)) {
      // Connect in background thread
      CountDownLatch connectionMade = new CountDownLatch(1);
      Thread clientThread = new Thread(() -> {
        try {
          testSocket = new Socket("localhost", server.getLocalPort());
          connectionMade.countDown();
          Thread.sleep(100); // Keep connection open briefly
        } catch (java.lang.Exception e) {
          fail("Unable to open socket: " + e.getMessage());
        }
      });
      clientThread.start();

      // Accept connection
      Socket serverSocket = server.accept();
      assertTrue(connectionMade.await(1, TimeUnit.SECONDS));

      // Test construction
      assertDoesNotThrow(() -> {
        SocketConnection connection = new SocketConnection(serverSocket, new Millisecond());
        assertNotNull(connection);
        assertSet.accept(connection._isSet());

        // Test methods don't throw
        assertNotNull(connection.input());
        assertNotNull(connection.output());
        assertNotNull(connection._string());

        connection._close();
      });

      clientThread.join(1000);
    }
  }

  @Test
  void testBasicInputOutput() throws java.lang.Exception {
    // Create a pair of connected sockets using pipes
    TestSocketPair socketPair = createTestSocketPair();

    try {
      SocketConnection connection = new SocketConnection(socketPair.socket1, new Millisecond());

      // Test output
      StringOutput output = connection.output();
      assertSet.accept(output._isSet());

      // Write some data
      assertDoesNotThrow(() -> output.println(String._of("Hello World")));
      assertDoesNotThrow(() -> output.print(String._of("Test")));

      connection._close();
      assertFalse.accept(connection._isSet());

    } finally {
      socketPair.close();
    }
  }

  @Test
  void testConnectionString() throws java.lang.Exception {
    TestSocketPair socketPair = createTestSocketPair();

    try {
      SocketConnection connection = new SocketConnection(socketPair.socket1, new Millisecond());

      String connectionString = connection._string();
      assertSet.accept(connectionString);
      assertTrue(connectionString.state.contains("SocketConnection"));

      connection._close();

      // After close, string should be unset
      assertUnset.accept(connection._string());

    } finally {
      socketPair.close();
    }
  }

  @Test
  void testTimeoutSetting() throws java.lang.Exception {
    TestSocketPair socketPair = createTestSocketPair();

    try {
      Millisecond timeout = Millisecond._of(1000);
      SocketConnection connection = new SocketConnection(socketPair.socket1, timeout);

      assertSet.accept(connection._isSet());

      // Timeout setting shouldn't affect basic functionality
      assertNotNull(connection.input());
      assertNotNull(connection.output());

      connection._close();

    } finally {
      socketPair.close();
    }
  }

  @Test
  void testTimeoutWithUnsetMillisecond() throws java.lang.Exception {
    TestSocketPair socketPair = createTestSocketPair();

    try {
      Millisecond unsetTimeout = new Millisecond(); // Unset timeout

      // Should not throw even with unset timeout
      assertDoesNotThrow(() -> {
        SocketConnection connection = new SocketConnection(socketPair.socket1, unsetTimeout);
        assertSet.accept(connection._isSet());
        connection._close();
      });

    } finally {
      socketPair.close();
    }
  }

  @Test
  void testInputOutputAfterClose() throws java.lang.Exception {
    TestSocketPair socketPair = createTestSocketPair();

    try {
      SocketConnection connection = new SocketConnection(socketPair.socket1, new Millisecond());

      StringInput input = connection.input();
      StringOutput output = connection.output();

      // Should work initially
      assertSet.accept(input._isSet());
      assertSet.accept(output._isSet());

      // Close connection
      connection._close();

      // Should be false after close (was constructed then closed)
      assertFalse.accept(connection._isSet());
      assertFalse.accept(input._isSet());
      assertFalse.accept(output._isSet());

      // Operations should be no-ops after close
      assertDoesNotThrow(() -> output.println(String._of("Should be ignored")));
      assertFalse.accept(input.hasNext());
      assertUnset.accept(input.next());

    } finally {
      socketPair.close();
    }
  }

  @Test
  void testMultipleClose() throws java.lang.Exception {
    TestSocketPair socketPair = createTestSocketPair();

    try {
      SocketConnection connection = new SocketConnection(socketPair.socket1, new Millisecond());

      // Multiple closes should be safe
      assertDoesNotThrow(connection::_close);
      assertDoesNotThrow(connection::_close);
      assertDoesNotThrow(connection::_close);

      assertFalse.accept(connection._isSet());

    } finally {
      socketPair.close();
    }
  }

  @Test
  void testInputCloseCallsConnectionClose() throws java.lang.Exception {
    TestSocketPair socketPair = createTestSocketPair();
    assertNotNull(socketPair);

    try {
      SocketConnection connection = new SocketConnection(socketPair.socket1, new Millisecond());
      StringInput input = connection.input();

      assertTrue.accept(connection._isSet());

      input._close();

      // Connection should be closed
      assertFalse.accept(connection._isSet());

    } finally {
      socketPair.close();
    }
  }

  @Test
  void testOutputCloseCallsConnectionClose() throws java.lang.Exception {
    TestSocketPair socketPair = createTestSocketPair();
    assertNotNull(socketPair);
    try {
      SocketConnection connection = new SocketConnection(socketPair.socket1, new Millisecond());
      StringOutput output = connection.output();

      assertSet.accept(connection._isSet());

      output._close();

      // Connection should be closed
      assertFalse.accept(connection._isSet());

    } finally {
      socketPair.close();
    }
  }

  /**
   * Helper class to create a pair of connected test sockets.
   */
  private static class TestSocketPair {
    final Socket socket1;
    final Socket socket2;
    final ServerSocket server;

    TestSocketPair(Socket socket1, Socket socket2, ServerSocket server) {
      this.socket1 = socket1;
      this.socket2 = socket2;
      this.server = server;
    }

    void close() {
      try {
        if (socket1 != null) {
          socket1.close();
        }
      } catch (IOException _) {
        // Ignore
      }
      try {
        if (socket2 != null) {
          socket2.close();
        }
      } catch (IOException _) {
        // Ignore
      }
      try {
        if (server != null) {
          server.close();
        }
      } catch (IOException _) {
        // Ignore
      }
    }
  }

  /**
   * Creates a pair of connected sockets for testing.
   */
  private TestSocketPair createTestSocketPair() throws IOException {
    ServerSocket server = new ServerSocket(0);

    // Connect in background
    final Socket[] clientSocket = new Socket[1];
    Thread connector = new Thread(() -> {
      try {
        //Need to hang on to this socket so cannot use try with.
        clientSocket[0] = new Socket("localhost", server.getLocalPort());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    connector.start();

    Socket serverSocket = server.accept();

    try {
      connector.join(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Failed to connect sockets", e);
    }

    return new TestSocketPair(serverSocket, clientSocket[0], server);
  }
}