package org.ek9.lang;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 HMAC type that provides cryptographic hashing functions.
 * This is a stateless utility class - it holds no state and isSet is always true.
 * Provides SHA256 hashing for String and GUID inputs.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    HMAC""")
public class HMAC extends BuiltinType {

  @Ek9Constructor("""
      HMAC() as pure""")
  public HMAC() {
    set(); // Always set since HMAC is stateless
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Method("""
      SHA256() as pure
        -> arg0 as String
        <- rtn as String?""")
  public String SHA256(String arg0) {
    if (!isValid(arg0)) {
      return new String(); // Return unset String for invalid input
    }

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(arg0.state.getBytes(StandardCharsets.UTF_8));
      return String._of(bytesToHex(hash));
    } catch (NoSuchAlgorithmException _) {
      // Should never happen with SHA-256
      return new String(); // Return unset String on error
    }
  }

  @Ek9Method("""
      SHA256() as pure
        -> arg0 as GUID
        <- rtn as String?""")
  public String SHA256(GUID arg0) {
    if (!isValid(arg0)) {
      return new String(); // Return unset String for invalid input
    }

    // Convert GUID to string and hash it
    String guidStr = arg0._string();
    return SHA256(guidStr);
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  /**
   * Converts byte array to hexadecimal string representation.
   */
  private java.lang.String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(java.lang.String.format("%02x", b));
    }
    return result.toString();
  }
}