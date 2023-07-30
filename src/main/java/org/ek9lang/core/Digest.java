package org.ek9lang.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Wraps the SHA 256 digest and the resulting byte array into objects,
 * so we can deal with check objects rather than raw bytes.
 */
public final class Digest {

  private Digest() {
    //Just to stop instantiation.
  }

  /**
   * Access a sha1 message digest.
   */
  public static MessageDigest getSha256() {
    Processor<MessageDigest> processor = () -> MessageDigest.getInstance("SHA-256");
    return new ExceptionConverter<MessageDigest>().apply(processor);
  }

  /**
   * Get checksum of an input.
   */
  public static CheckSum digest(String input) {
    AssertValue.checkNotNull("checksum input cannot be null", input);
    return digest(input.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Checksum of bytes.
   */
  public static CheckSum digest(byte[] input) {
    AssertValue.checkNotNull("checksum input cannot be null", input);
    return new CheckSum(getSha256().digest(input));
  }

  /**
   * Open a file and create a checksum of the contents.
   */
  public static CheckSum digest(File file) {

    Processor<CheckSum> processor = () -> {
      try (InputStream is = new FileInputStream(file)) {
        MessageDigest digest = getSha256();
        byte[] buffer = new byte[4096];
        int amountRead;
        while ((amountRead = is.read(buffer, 0, 4096)) != -1) {
          digest.update(buffer, 0, amountRead);
        }
        return new CheckSum(digest.digest());
      }
    };
    return new ExceptionConverter<CheckSum>().apply(processor);
  }

  /**
   * Checks if the contents of a file when a check sum is calculated are the same as that in the
   * check sum file.
   */
  public static boolean check(File contentsFile, File checkSumFile) {
    CheckSum contentsSha = digest(contentsFile);
    CheckSum providedSha = new CheckSum(checkSumFile);

    return providedSha.equals(contentsSha);
  }

  /**
   * A Checksum.
   */
  public static final class CheckSum {
    private final byte[] theCheckSum;

    /**
     * Load a checksum from a file.
     */
    public CheckSum(File sha256File) {
      this.theCheckSum = this.loadFromFile(sha256File);
    }

    public CheckSum(byte[] checksum) {
      AssertValue.checkNotNull("checksum bytes array cannot be null", checksum);
      this.theCheckSum = checksum;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(theCheckSum);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }

      if (obj instanceof CheckSum cs) {
        return checkBytesSame(theCheckSum, cs.theCheckSum);
      }

      if (obj instanceof byte[] bytes) {
        return checkBytesSame(theCheckSum, bytes);
      }

      return false;
    }

    private boolean checkBytesSame(byte[] checksum1, byte[] checksum2) {
      if (checksum1.length != checksum2.length) {
        return false;
      }
      for (int i = 0; i < checksum1.length; i++) {
        if (checksum1[i] != checksum2[i]) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      return Hex.toString(theCheckSum);
    }

    /**
     * Just save this digest to a file.
     */
    public void saveToFile(final File sha256File) {

      Processor<Void> processor = () -> {
        try (OutputStream output = new FileOutputStream(sha256File)) {
          //Don't include the file name - because it might be very long, and we need to
          //keep what we have to send short because it is going to use PKI to encrypt it.
          String content = this + " *-\n";
          var rtn = content.getBytes();
          output.write(rtn);
          return null;
        }
      };
      new ExceptionConverter<Void>().apply(processor);
    }

    private byte[] loadFromFile(File sha256File) {
      Processor<byte[]> processor = () -> {
        try (InputStream is = new BufferedInputStream(new FileInputStream(sha256File))) {
          String line = new String(is.readAllBytes(), StandardCharsets.UTF_8);
          String firstPart = line.split(" ")[0];
          return Hex.toByteArray(firstPart);
        }
      };
      return run(processor);
    }

    private byte[] run(Processor<byte[]> processor) {
      return new ExceptionConverter<byte[]>().apply(processor);
    }
  }
}
