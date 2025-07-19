package org.ek9.lang;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * TCP class for EK9 - represents a TCP server/client networking interface.
 * <p>
 * This class provides TCP networking functionality including server socket
 * creation, connection establishment, and connection acceptance.
 * </p>
 * <p>
 * The implementation uses Java Socket and ServerSocket for underlying
 * TCP operations while providing EK9-compatible interfaces.
 * Uses virtual threads for scalable concurrent connection handling.
 * </p>
 * <p>
 * Can represent both a Server socket and a client socket (not at the same time).
 * </p>
 * <p>
 * But to reduce the surface area of what you need to know, this single TCP socket
 * implementation can be constructed as a server socket and then you may use 'accept'
 * to handle incoming connections.
 * </p>
 * <p>
 * Or you can construct it as a client socket to make connections to servers and then use
 * 'connect' to get the connection and the input and output objects for communication.
 * </p>
 * <p>
 * Below is an outline of the way to use TCP in server mode.
 * basically if the tcp server could start with the network properties it will be 'set'.
 * i.e. the '?' operator will report true. Then you can use your 'handler' via 'accept'.
 * </p>
 * <p>
 * At this point the code will block in the accept method, using your 'handler' as clients
 * connect to your server. Only when the service is 'triggered' to exit (by some means - signals, UDP, message)
 * will the accept method complete.
 * </p>
 * <p>Example TCP as server:</p>
 * <pre>
 *   stdout &lt;- Stdout()
 *   stderr &lt;- Stderr()
 *   tcpServer &lt;- TCP(NetworkProperties(4446, 5, 4, true))
 *   handler &lt;- ...
 *   if tcpServer?
 *     if tcpServer.accept(handler)
 *       stdout.println("TCP Server completed and shutdown")
 *     else
 *       stderr.println(`Was not able to service requests gracefully [${tcpServer}]`)
 *    else
 *      stderr.println(`Failed to get TCP server running [${tcpServer}]`)
 * </pre>
 * <p>Example TCP as a client:</p>
 * <pre>
 *   TCPClient1()
 *       stdout &lt;- Stdout()
 *       stderr &lt;- Stderr()
 *
 *       tcpClient &lt;- TCP(NetworkProperties("localhost", 4446))
 *
 *       if tcpClient?
 *         stdout.println(`Will attempt to connect to server ${tcpClient}`)
 *         try
 *           -&gt; connection &lt;- tcpClient.connect()
 *           if connection?
 *             connection.output().println("Short Message")
 *             stdout.println(`Received response [${connection.input().next()}]`)
 *             //End of the try will just close the connection.
 *           else
 *             stderr.println(`Failed to connect [${tcpClient}]`)
 *       else
 *         stderr.println(`Failed to get TCP client running [${tcpClient}]`)
 * </pre>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("TCP")
public class TCP extends BuiltinType {

  private NetworkProperties properties = new NetworkProperties();
  private ServerSocket serverSocket;
  private EK9ThreadPool threadPool;
  private SocketConnection clientConnection;
  private java.lang.String lastError = "";
  private boolean isServer;
  //Can be altered by other threads
  private volatile boolean isListening;

  @Ek9Constructor("TCP() as pure")
  public TCP() {
    // TCP is unset when constructed without properties
    unSet();
  }

  @Ek9Constructor("""
      TCP() as pure
        -> properties as NetworkProperties""")
  public TCP(NetworkProperties properties) {
    this.properties = properties;
    if (isValid(properties)) {
      initializeFromProperties();
    } else {
      unSet();
    }
  }

  @Ek9Method("""
      connect()
        <- rtn as TCPConnection?""")
  public TCPConnection connect() {
    if (isServer || !isSet) {
      lastError = isServer ? "Cannot connect in server mode" : "TCP cannot be used as client in this mode.";

      return new TCPConnection() {
        @Override
        public Boolean _isSet() {
          return Boolean._of(false);
        }
      };
    }

    // Check if already connected
    if (isValid(clientConnection)) {
      return clientConnection;
    }

    try {
      java.lang.String host = properties.host._isSet().state ? properties.host.state : "localhost";
      int port = properties.port._isSet().state ? (int) properties.port.state : 80;

      Socket socket = new Socket(host, port);
      clientConnection = new SocketConnection(socket, properties.timeout);
      return clientConnection;

    } catch (IOException e) {
      lastError = e.getMessage();
      return new TCPConnection() {
        @Override
        public Boolean _isSet() {
          return Boolean._of(false);
        }
      };
    }
  }

  @Ek9Method("""
      accept()
        -> handler as TCPHandler
        <- rtn as Boolean?""")
  public Boolean accept(TCPHandler handler) {
    if (!isServer || !isSet || serverSocket == null) {
      lastError = !isServer ? "Cannot accept in client mode" : "TCP cannot be used as server in this mode.";
      return Boolean._of(false);
    }

    if (!isValid(handler)) {
      lastError = "Invalid handler provided";
      return Boolean._of(false);
    }

    return doAccept(handler);
  }

  @Ek9Method("""
      lastErrorMessage() as pure
        <- rtn as String?""")
  public String lastErrorMessage() {
    return String._of(lastError);
  }

  @Ek9Operator("""
      operator close as pure""")
  public void _close() {
    isListening = false;

    // Close client connection if it exists
    if (clientConnection != null) {
      clientConnection._close();
      clientConnection = null;
    }

    // Close server socket and thread pool if in server mode
    if (isServer) {
      if (threadPool != null) {
        final var anyErrorMessage = threadPool.close();
        if (isValid(anyErrorMessage)) {
          lastError = anyErrorMessage.state;
        }

        threadPool = null;
      }

      if (serverSocket != null && !serverSocket.isClosed()) {
        try {
          serverSocket.close();
        } catch (IOException e) {
          lastError = "Error closing server socket: " + e.getMessage();
        }
        serverSocket = null;
      }
    }

    // Mark as unset after cleanup
    unSet();
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
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (!isSet) {
      return String._of("TCP[unset]");
    }

    java.lang.String mode = isServer ? "server" : "client";
    java.lang.String status = isListening ? "listening" : "ready";
    java.lang.String errorInfo = (lastError != null && !lastError.isEmpty()) ? ", error: " + lastError : "";

    return String._of("TCP{"
        + mode
        + ", "
        + status
        + ", "
        + properties._string().state
        + errorInfo + "}");
  }

  // Utility methods

  private Boolean doAccept(TCPHandler handler) {

    isListening = true;
    boolean noSocketErrors = true;

    try {
      // Accept connections until server is closed
      while (serverSocket != null && !serverSocket.isClosed() && isListening) {
        noSocketErrors = acceptSocket(handler);
        if (!noSocketErrors) {
          break;
        }
      }
    } catch (Exception e) {
      lastError = "Unexpected error in accept loop: " + e.getMessage();
      noSocketErrors = false;
    } finally {
      isListening = false;
    }

    return Boolean._of(noSocketErrors);
  }

  private boolean acceptSocket(TCPHandler handler) {
    try {
      Socket clientSocket = serverSocket.accept();

      // Submit connection handling to thread pool
      ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket, handler, properties.timeout);
      threadPool.submit(connectionHandler);
      return true;
    } catch (SocketException e) {
      // Socket was closed during accept - this is normal during shutdown
      if (isListening) {
        lastError = "Server socket closed unexpectedly: " + e.getMessage();
        return false;
      }
      return true; //this is a normal situation
    } catch (IOException e) {
      lastError = "Error accepting connection: " + e.getMessage();
      return false;
    }
  }


  /**
   * Initialize TCP based on NetworkProperties to determine server vs client mode.
   */
  @SuppressWarnings("checkstyle:CatchParameterName")
  private void initializeFromProperties() {
    try {
      // Determine mode based on properties
      boolean hasServerProperties = properties.backlog._isSet().state || properties.maxConcurrent._isSet().state;
      boolean hasClientProperties = properties.host._isSet().state;

      if (hasServerProperties) {
        initializeServerMode();
      } else if (hasClientProperties) {
        initializeClientMode();
      } else if (properties.port._isSet().state) {
        // Port only - assume server mode with defaults
        initializeServerMode();
      } else {
        lastError =
            "NetworkProperties must specify either server (backlog/maxConcurrent) or client (host) configuration";
        unSet();
      }
    } catch (IOException | Exception e) {
      lastError = "Failed to initialize TCP: " + e.getMessage();
      unSet();
    }
  }

  /**
   * Initialize TCP in server mode.
   */
  @SuppressWarnings("checkstyle:CatchParameterName")
  private void initializeServerMode() throws IOException {
    isServer = true;

    int port = properties.port._isSet().state ? (int) properties.port.state : 8080;
    int backlog = properties.backlog._isSet().state ? (int) properties.backlog.state : 50;
    int maxConcurrent = properties.maxConcurrent._isSet().state ? (int) properties.maxConcurrent.state : 10;
    boolean localOnly = properties.localOnly._isSet().state && properties.localOnly.state;

    // Create server socket
    if (localOnly) {
      serverSocket = new ServerSocket(port, backlog, InetAddress.getLoopbackAddress());
    } else {
      serverSocket = new ServerSocket(port, backlog);
    }

    // Update properties with actual assigned port if port 0 was used
    if (port == 0) {
      // OS assigned an actual port, update NetworkProperties with the real port
      properties.port = Integer._of(serverSocket.getLocalPort());
    }

    // Create thread pool for handling connections
    threadPool = new EK9ThreadPool(maxConcurrent);

    set(); // Mark as successfully initialized
  }

  /**
   * Initialize TCP in client mode.
   */
  private void initializeClientMode() {
    isServer = false;

    // Validate client properties
    if (!properties.host._isSet().state || !properties.port._isSet().state) {
      this.lastError = "Client mode requires both host and port";
    } else {
      set(); // Mark as successfully initialized
    }
  }

  /**
   * Internal class to handle individual client connections in server mode.
   */
  private static class ConnectionHandler implements Callable<Void> {
    private final Socket clientSocket;
    private final TCPHandler handler;
    private final Millisecond timeout;

    public ConnectionHandler(Socket clientSocket, TCPHandler handler, Millisecond timeout) {
      this.clientSocket = clientSocket;
      this.handler = handler;
      this.timeout = timeout;
    }

    @Override
    @SuppressWarnings("checkstyle:CatchParameterName")
    public Void call() {
      try {
        // Create connection wrapper
        SocketConnection connection = new SocketConnection(clientSocket, timeout);

        // Call the handler with input and output streams
        handler._call(connection.input(), connection.output());

      } catch (IOException | Exception _) {
        // Error handling - close connection
      } finally {
        // Always close the client socket
        try {
          if (!clientSocket.isClosed()) {
            clientSocket.close();
          }
        } catch (IOException _) {
          // Ignore close errors
        }
      }
      return null;
    }
  }
}