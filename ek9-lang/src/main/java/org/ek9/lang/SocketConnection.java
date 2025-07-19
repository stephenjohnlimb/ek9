package org.ek9.lang;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import org.ek9tooling.Ek9Operator;

/**
 * Internal Java implementation of TCPConnection that wraps a Java Socket.
 * <p>
 * This class is not exposed to EK9 language - it's an internal implementation utility
 * that implements the TCPConnection trait. Provides StringInput and StringOutput
 * interfaces for network communication.
 * </p>
 * <p>
 * Follows the pattern established by Levenshtein.java - pure Java implementation
 * without EK9 annotations that can be used by EK9-exposed classes.
 * </p>
 */
public class SocketConnection implements TCPConnection {

  private final Socket socket;
  private final Scanner scanner;
  private final PrintWriter printWriter;
  private final StringInput stringInput;
  private final StringOutput stringOutput;
  private boolean isSet = true;
  private boolean closed = false;

  /**
   * Creates a SocketConnection wrapping the provided Socket.
   *
   * @param socket  The connected socket to wrap
   * @param timeout The read timeout in milliseconds (can be unset)
   * @throws IOException if there's an error setting up I/O streams
   */
  public SocketConnection(Socket socket, Millisecond timeout) throws IOException {
    if (socket == null || !socket.isConnected()) {
      throw new IllegalArgumentException("Socket must be connected");
    }

    this.socket = socket;

    // Set timeout if provided and valid
    if (isValid(timeout)) {
      try {
        int timeoutMs = (int) timeout._prefix().state;
        socket.setSoTimeout(timeoutMs);
      } catch (Exception e) {
        // If timeout setting fails, continue without timeout
        System.err.println("Warning: Failed to set socket timeout: " + e.getMessage());
      }
    }

    // Set up I/O streams
    this.scanner = new Scanner(new InputStreamReader(socket.getInputStream()));
    this.printWriter = new PrintWriter(socket.getOutputStream(), true);

    // Create StringInput and StringOutput implementations
    this.stringInput = new SocketStringInput();
    this.stringOutput = new SocketStringOutput();
  }

  @Override
  public StringInput input() {
    return stringInput;
  }

  @Override
  public StringOutput output() {
    return stringOutput;
  }

  @Override
  public Boolean _isSet() {
    return Boolean._of(isSet && !closed && socket.isConnected());
  }

  @Override
  public String _string() {
    if (_isSet().state) {
      return String._of("SocketConnection{"
          + socket.getRemoteSocketAddress()
          + " -> "
          + socket.getLocalSocketAddress()
          + "}");
    }
    return new String(); // Unset string
  }

  /**
   * Closes the socket connection and all associated resources.
   */
  @SuppressWarnings("checkstyle:CatchParameterName")
  @Override
  @Ek9Operator("""
      override operator close as pure""")
  public void _close() {
    if (!closed) {
      closed = true;
      isSet = false;

      try {
        if (scanner != null) {
          scanner.close();
        }
      } catch (Exception _) {
        // Ignore scanner close errors
      }

      try {
        if (printWriter != null) {
          printWriter.close();
        }
      } catch (Exception _) {
        // Ignore print writer close errors
      }

      try {
        if (socket != null && !socket.isClosed()) {
          socket.close();
        }
      } catch (IOException _) {
        // Ignore socket close errors
      }
    }
  }

  /**
   * Internal StringInput implementation that reads from the socket's Scanner.
   */
  private class SocketStringInput implements StringInput {

    @SuppressWarnings("checkstyle:CatchParameterName")
    @Override
    public String next() {
      if (closed || !socket.isConnected()) {
        return new String(); // Unset string
      }

      try {
        if (scanner.hasNext()) {
          java.lang.String line = scanner.nextLine();
          return String._of(line);
        }
      } catch (Exception _) {
        // Error reading - close connection and return unset
        SocketConnection.this._close();
        return new String();
      }

      return new String(); // No more input
    }

    @SuppressWarnings("checkstyle:CatchParameterName")
    @Override
    public Boolean hasNext() {
      if (closed || !socket.isConnected()) {
        return Boolean._of(false);
      }

      try {
        return Boolean._of(scanner.hasNext());
      } catch (Exception _) {
        // Error checking for input - assume no more
        return Boolean._of(false);
      }
    }

    @Override
    public void _close() {
      SocketConnection.this._close();
    }

    @Override
    public Boolean _isSet() {
      return Boolean._of(!closed && socket.isConnected());
    }
  }

  /**
   * Internal StringOutput implementation that writes to the socket's PrintWriter.
   */
  private class SocketStringOutput implements StringOutput {

    @SuppressWarnings("checkstyle:CatchParameterName")
    @Override
    public void println(String arg0) {
      if (!closed && socket.isConnected() && canProcess(arg0)) {
        try {
          printWriter.println(arg0.state);
          printWriter.flush();
        } catch (Exception _) {
          // Error writing - close connection
          SocketConnection.this._close();
        }
      }
    }

    @SuppressWarnings("checkstyle:CatchParameterName")
    @Override
    public void print(String arg0) {
      if (!closed && socket.isConnected() && canProcess(arg0)) {
        try {
          printWriter.print(arg0.state);
          printWriter.flush();
        } catch (Exception _) {
          // Error writing - close connection
          SocketConnection.this._close();
        }
      }
    }

    @Override
    public void _close() {
      SocketConnection.this._close();
    }

    @Override
    public Boolean _isSet() {
      return Boolean._of(!closed && socket.isConnected());
    }
  }
}