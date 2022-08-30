package org.ek9lang.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DigestTest {
  private static final String testFileName = "assertTest.txt";
  private static final String sha256FileName = "assertTest.sha256";

  @BeforeEach
  void removeFile() {
    File newFile = new File(System.getProperty("java.io.tmpdir"), testFileName);
    //Remove it if already there
    if (newFile.exists()) {
      newFile.delete();
    }
    File sha256File = new File(System.getProperty("java.io.tmpdir"), sha256FileName);
    if (sha256File.exists()) {
      sha256File.delete();
    }
  }

  @Test
  @SuppressWarnings("java:S5785")
  void testNullCheckSums() {
    Digest.CheckSum cksum = new Digest.CheckSum(new byte[1]);
    assertFalse(cksum.equals(null));
  }

  @Test
  void missingFile() {
    assertThrows(java.lang.RuntimeException.class, () -> {
      File nonSuch = new File(System.getProperty("java.io.tmpdir"), "nonSuch.txt");
      Digest.digest(nonSuch);
    });
  }

  @Test
  void missingChecksumFile() {
    assertThrows(java.lang.RuntimeException.class, () -> {
      File nonSuch = new File(System.getProperty("java.io.tmpdir"), "nonSuch.txt");
      File nonSuchSha256 = new File(System.getProperty("java.io.tmpdir"), "nonSuch.sha256");
      Digest.check(nonSuch, nonSuchSha256);
    });
  }

  @Test
  void quickCheck() {
    //checked against https://xorbin.com/tools/sha256-hash-calculator
    String data = "The quick brown fox";
    Digest.CheckSum ckSum = Digest.digest(data);

    String check = ckSum.toString();
    assertEquals("5cac4f980fedc3d3f1f99b4be3472c9b30d56523e632d151237ec9309048bda9".toUpperCase(),
        check);
  }

  @Test
  void testEquality() {
    String data = "The quick brown fox";
    Digest.CheckSum ckSum1 = Digest.digest(data);
    Digest.CheckSum ckSum2 = Digest.digest(data);
    assertEquals(ckSum1, ckSum2);
    assertEquals(ckSum1, ckSum1);
    assertEquals(ckSum1.hashCode(), ckSum2.hashCode());

  }

  @Test
  @SuppressWarnings("java:S5785")
  void testDigestEmptyFile() throws IOException {
    File newFile = new File(System.getProperty("java.io.tmpdir"), testFileName);
    newFile.createNewFile();

    File sha256File = new File(System.getProperty("java.io.tmpdir"), sha256FileName);

    Digest.CheckSum ckSum1 = Digest.digest(newFile);
    ckSum1.saveToFile(sha256File);

    Digest.check(newFile, sha256File);

    Digest.CheckSum ckSum2 = Digest.digest(newFile);

    assertEquals(ckSum1, ckSum2);

    //There is no content so check sum always the same
    assertEquals("E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
        ckSum1.toString());

    byte[] validBytes =
        Hex.toByteArray("E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855");
    assertTrue(ckSum1.equals(validBytes));

    byte[] inValidBytes =
        Hex.toByteArray("X3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855");
    assertFalse(ckSum1.equals(inValidBytes));

    assertFalse(ckSum1.equals(new byte[0]));


    assertFalse(ckSum1.equals(""));

    if (newFile.exists()) {
      newFile.delete();
    }
  }
}
