package org.ek9.lang;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * UDP class for EK9 - represents a UDP networking interface for datagram communications.
 * <p>
 * This class provides UDP networking functionality including datagram socket
 * creation, packet sending, and packet receiving operations.
 * </p>
 * <p>
 * The implementation uses Java DatagramSocket for underlying UDP operations
 * while providing EK9-compatible interfaces.
 * </p>
 * <p>
 * UDP requires a port to be specified in NetworkProperties for socket creation.
 * If no port is provided, the UDP instance will be unset and unusable.
 * </p>
 * <p>Example UDP Server usage:</p>
 * <pre>
 *   stdout &lt;- Stdout()
 *   udpConnection &lt;- UDP(NetworkProperties(port: 4445))
 *
 *   while udpConnection?
 *     packet &lt;- udpConnection.receive()
 *     if packet?
 *       if packet.content == "end"
 *         endPacket &lt;- UDPPacket(packet.properties, "OK will stop Server")
 *         udpConnection.send(endPacket)
 *         udpConnection.close()
 *       else
 *         udpConnection.send(packet)
 *
 *   stdout.println("UDP Server Complete")
 * </pre>
 * <p>Example UDP Client usage:</p>
 * <pre>
 *   stdout &lt;- Stdout()
 *
 *   try
 *     -&gt; client &lt;- UDP(NetworkProperties(timeout: 250ms))
 *
 *     packet &lt;- UDPPacket(NetworkProperties("localhost", 4445), "A Test")
 *     client.send(packet)
 *     response &lt;- client.receive()
 *     if response?
 *       stdout.println(`Received [${response}]`)
 *
 *   stdout.println("UDP Client Complete")
 * </pre>
 * <p>Example using pipe operator for sending:</p>
 * <pre>
 *   try
 *     -&gt; udpConnection &lt;- UDP(NetworkProperties(250ms))
 *     cat ["A Test", "Middle Item", "end"] | map stringToPacket &gt; udpConnection
 *
 *   Stdout().println("UDP Client Complete")
 * </pre>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("UDP")
public class UDP extends BuiltinType {

  private static final int DEFAULT_BUFFER_SIZE = 1024;
  private static final int DEFAULT_TIMEOUT_MS = 5000;

  private NetworkProperties properties = new NetworkProperties();
  private DatagramSocket socket;
  private java.lang.String lastError = "";
  private final BlockingQueue<UDPPacket> receivedPackets = new LinkedBlockingQueue<>();
  private volatile boolean isReceiving = false;
  private Thread receiveThread;

  @Ek9Constructor("""
      <?-
        Creates an unset UDP connection, useful when connections should not be provided.
      -?>
      UDP() as pure""")
  public UDP() {
    // UDP is unset when constructed without properties
    unSet();
  }

  @Ek9Constructor("""
      <?-
        Use specific network properties to set the destination, port and timeouts.
      -?>
      UDP() as pure
        -> properties as NetworkProperties""")
  public UDP(NetworkProperties properties) {
    this.properties = properties;
    if (isValid(properties) && properties.port._isSet().state) {
      initializeSocket();
    } else {
      lastError = "Port is required for UDP socket creation";
      unSet();
    }
  }

  @Ek9Method("""
      <?-
        Set the appropriate NetworkProperties on the UDPPacket to specific the destination.
        It is possible to broadcast by using 255.255.255.255 or other specific CIDRs.
        Also set the content for the actual message you wish to send.
      -?>
      send()
        -> packet as UDPPacket""")
  public void send(UDPPacket packet) {
    if (!isSet || socket == null) {
      lastError = "UDP socket is not available for sending";
      return;
    }

    if (!isValid(packet)) {
      lastError = "Invalid packet provided";
      return;
    }

    if (!packet.properties._isSet().state || !packet.properties.host._isSet().state
        || !packet.properties.port._isSet().state) {
      lastError = "Packet must have destination host and port";
      return;
    }

    try {
      java.lang.String host = packet.properties.host.state;
      int port = (int) packet.properties.port.state;
      java.lang.String content = packet.content._isSet().state ? packet.content.state : "";

      byte[] data = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      InetAddress address = InetAddress.getByName(host);
      DatagramPacket datagramPacket = new DatagramPacket(data, data.length, address, port);

      socket.send(datagramPacket);
      lastError = "";
    } catch (IOException e) {
      lastError = "Failed to send packet: " + e.getMessage();
    }
  }

  @Ek9Method("""
      <?-
        Check to see if the internal buffer of message is empty or has packets to supply.
      -?>
      hasNext() as pure
        <- rtn as Boolean?""")
  public Boolean hasNext() {
    if (!isSet || socket == null) {
      return Boolean._of(false);
    }

    ensureReceiving();
    return Boolean._of(!receivedPackets.isEmpty());
  }

  @Ek9Method("""
      <?-
        Consumes the next packet from the internal buffer if one is present.
        If one is not present it returns an unset UDPPacket.
      -?>
      next()
        <- packet as UDPPacket?""")
  public UDPPacket next() {
    if (!isSet || socket == null) {
      lastError = "UDP socket is not available";
      return new UDPPacket();
    }

    ensureReceiving();
    UDPPacket packet = receivedPackets.poll();
    return packet != null ? packet : new UDPPacket();
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Method("""
      <?-
        Consumes the next packet from the internal buffer if one is present.
        If one is not present it returns an unset UDPPacket.
        But, in this case it waits for timeout period of time for a new packet to arrive
        rather than just returning if no packets are available.
        If no timeout configuration was supplied in the Network Properties 5 seconds is used.
      -?>
      receive()
        <- packet as UDPPacket?""")
  public UDPPacket receive() {
    if (!isSet || socket == null) {
      lastError = "UDP socket is not available for receiving";
      return new UDPPacket();
    }

    ensureReceiving();

    int timeoutMs = properties.timeout._isSet().state
        ? (int) properties.timeout.state
        : DEFAULT_TIMEOUT_MS;

    try {
      UDPPacket packet = receivedPackets.poll(timeoutMs, TimeUnit.MILLISECONDS);
      return packet != null ? packet : new UDPPacket();
    } catch (InterruptedException _) {
      lastError = "Receive operation was interrupted";
      Thread.currentThread().interrupt();
      return new UDPPacket();
    }
  }

  @Ek9Method("""
      <?-
        If an error occurred, this method will supply the details of that error.
      -?>
      lastErrorMessage() as pure
        <- rtn as String?""")
  public String lastErrorMessage() {
    return String._of(lastError);
  }

  @Ek9Operator("""
      <?-
        Short cut method to send a message, useful for use with pipelines.
      -?>
      operator |
        -> packet as UDPPacket""")
  public void _pipe(UDPPacket packet) {
    send(packet);
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Operator("""
      <?-
        Closes the UDP connection, this will alter the UDP state to 'unset'.
        It will also clear any buffered packets held in memory.
      -?>
      operator close as pure""")
  public void _close() {
    isReceiving = false;

    if (receiveThread != null && receiveThread.isAlive()) {
      receiveThread.interrupt();
      try {
        receiveThread.join(1000); // Wait up to 1 second for thread to finish
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt();
      }
      receiveThread = null;
    }

    if (socket != null && !socket.isClosed()) {
      socket.close();
      socket = null;
    }

    receivedPackets.clear();
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
      return String._of("UDP{}");
    }

    java.lang.String socketInfo = socket != null ? "bound to " + socket.getLocalPort() : "unbound";
    java.lang.String queueInfo = "queue: " + receivedPackets.size();
    java.lang.String errorInfo = (lastError != null && !lastError.isEmpty()) ? ", error: " + lastError : "";

    return String._of("UDP{"
        + socketInfo
        + ", "
        + queueInfo
        + ", "
        + properties._string().state
        + errorInfo + "}");
  }

  // Private utility methods

  @SuppressWarnings("checkstyle:CatchParameterName")
  private void initializeSocket() {
    try {
      int port = (int) properties.port.state;
      boolean localOnly = properties.localOnly._isSet().state && properties.localOnly.state;

      if (localOnly) {
        socket = new DatagramSocket(port, InetAddress.getLoopbackAddress());
      } else {
        socket = new DatagramSocket(port);
      }

      // Set socket timeout for receive operations
      int timeoutMs = properties.timeout._isSet().state
          ? (int) properties.timeout.state
          : DEFAULT_TIMEOUT_MS;
      socket.setSoTimeout(timeoutMs);

      // Update properties with actual assigned port if port 0 was used
      if (port == 0) {
        properties.port = Integer._of(socket.getLocalPort());
      }

      set(); // Mark as successfully initialized
      lastError = "";
    } catch (SocketException e) {
      lastError = "Failed to create UDP socket: " + e.getMessage();
      unSet();
    }
  }

  private void ensureReceiving() {
    if (!isReceiving && isSet && socket != null) {
      isReceiving = true;
      receiveThread = Thread.ofVirtual().start(this::receiveLoop);
    }
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  private void receiveLoop() {
    int bufferSize = properties.packetSize._isSet().state
        ? (int) properties.packetSize.state
        : DEFAULT_BUFFER_SIZE;

    while (isReceiving && socket != null && !socket.isClosed()) {
      try {
        byte[] buffer = new byte[bufferSize];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

        socket.receive(datagramPacket);

        // Create UDPPacket from received data
        java.lang.String host = datagramPacket.getAddress().getHostAddress();
        int port = datagramPacket.getPort();
        java.lang.String content = new java.lang.String(
            datagramPacket.getData(), 0, datagramPacket.getLength(),
            java.nio.charset.StandardCharsets.UTF_8);

        NetworkProperties packetProperties = new NetworkProperties(String._of(host), Integer._of(port));
        UDPPacket udpPacket = new UDPPacket(packetProperties, String._of(content));

        // Add to queue (non-blocking)
        if (!receivedPackets.offer(udpPacket)) {
          lastError = "Packet queue is full, dropping packet";
        }

      } catch (SocketTimeoutException _) {
        // Normal timeout, continue receiving
      } catch (IOException e) {
        if (isReceiving) {
          lastError = "Error receiving packet: " + e.getMessage();
        }
        break;
      }
    }
    isReceiving = false;
  }
}