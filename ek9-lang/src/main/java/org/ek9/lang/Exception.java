package org.ek9.lang;


import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * The EK9 exception for use in controlled way with try/catch.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Exception as open""")
public class Exception extends RuntimeException implements Any {

  boolean isSet = true;

  String reason = new String();

  /**
   * You can use an exit code if you wish.
   * If the exception makes it all the way back to
   * the 'main' in a CLI application this will be used
   * as the exit code of the application.
   */
  private final Integer exitCode = new Integer();

  private Exception(java.lang.String value) {
    if (value != null) {
      this.reason = String._of(value);
    }
  }

  private Exception(RuntimeException rex) {
    super(rex);
  }

  @Ek9Constructor("""
      Exception() as pure""")
  public Exception() {
    isSet = false;
  }

  @Ek9Constructor("""
      Exception() as pure
        -> cause as Exception""")
  public Exception(Exception cause) {
    super(cause);
  }

  @Ek9Constructor("""
      Exception() as pure
        ->
          cause as Exception
          exitCode as Integer""")
  public Exception(Exception cause, Integer exitCode) {
    this(cause);
    this.exitCode._copy(exitCode);
  }

  @Ek9Constructor("""
      Exception() as pure
        -> reason as String""")
  public Exception(String reason) {
    this.reason = reason;
  }

  @Ek9Constructor("""
      Exception() as pure
        ->
          reason as String
          exitCode as Integer""")
  public Exception(String reason, Integer exitCode) {
    this(reason);
    this.exitCode._copy(exitCode);
  }

  @Ek9Constructor("""
      Exception() as pure
        ->
          reason as Exception
          cause as Exception""")
  public Exception(String reason, Exception cause) {
    super(cause);
    this.reason = reason;
  }

  @Ek9Constructor("""
      Exception() as pure
        ->
          reason as Exception
          cause as Exception
          exitCode as Integer""")
  public Exception(String reason, Exception cause, Integer exitCode) {
    super(cause);
    this.reason = reason;
    this.exitCode._copy(exitCode);
  }

  @Ek9Method("""
      exitCode() as pure
        <- rtn as Integer?""")
  public Integer exitCode() {
    return exitCode;
  }

  @Ek9Method("""
      reason() as pure
        <- rtn as String?""")
  public String reason() {
    return reason;
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      return String._of(toString());
    }
    return new String();
  }

  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  @Override
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  //Start of Utility methods

  @Override
  public java.lang.String toString() {
    if (isSet) {
      StringBuilder builder = new StringBuilder();
      if (reason.isSet) {
        builder.append(reason.state);
      }

      final var underlyingMessage = getMessage();
      if (underlyingMessage != null) {
        if (!builder.isEmpty()) {
          builder.append(": ");
        }
        builder.append("Root Cause: ");
        builder.append(underlyingMessage);
      }

      if (exitCode.isSet) {
        if (!builder.isEmpty()) {
          builder.append(": ");
        }
        builder.append("Exit Code: ");
        builder.append(exitCode.state);
      }
      return "Exception: " + builder;
    }
    return "";
  }


  public static Exception _of(java.lang.String reason) {
    return new Exception(reason);
  }

  public static Exception _of(RuntimeException rex) {
    return new Exception(rex);
  }
}
