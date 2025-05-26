package org.ek9lang.core;

/**
 * Converts bytes to Hexadecimal and back.
 */
public final class Hex {
  private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

  private Hex() {
    //Just to stop instantiation.
  }

  /**
   * Converts bytes to hex string.
   */
  public static String toString(final byte[] bytes) {
    final var hexChars = new char[bytes.length * 2];

    for (int j = 0; j < bytes.length; j++) {
      final int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }

    return new String(hexChars);
  }

  /**
   * Converts hex string back into bytes.
   */
  public static byte[] toByteArray(final String s) {
    final var len = s.length();
    final var data = new byte[len / 2];

    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
          + Character.digit(s.charAt(i + 1), 16));
    }

    return data;
  }
}
