package org.ek9lang.core;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;

/**
 * Just a wrapper around java public private key processing.
 * Can hold either public key and private key, just one or none.
 */
public final class SigningKeyPair {
  private final Base64.Encoder encoder = Base64.getEncoder();
  private final Base64.Decoder decoder = Base64.getDecoder();
  private final Cipher cipher;
  private PublicKey pub;
  private PrivateKey pvt;

  private SigningKeyPair(PublicKey pub) {
    this();
    this.pub = pub;
  }

  private SigningKeyPair(PrivateKey pvt) {
    this();
    this.pvt = pvt;
  }

  private SigningKeyPair() {
    cipher = getRsaCipher();
  }

  /**
   * Create a signing key pair from the private and public parts of a PKI key pair.
   */
  public SigningKeyPair(String privateBase64, String publicBase64) {
    pvt = privateFromBase64(privateBase64);
    pub = publicFromBase64(publicBase64);
    cipher = getRsaCipher();
  }

  private static KeyPairGenerator getRsaKeyPairGenerator() {
    Processor<KeyPairGenerator> processor = () -> KeyPairGenerator.getInstance("RSA");
    return new ExceptionConverter<KeyPairGenerator>().apply(processor);
  }

  /**
   * Create a new signing key pair of a particular key size.
   */
  public static SigningKeyPair generate(int keySize) {
    SigningKeyPair rtn = new SigningKeyPair();
    KeyPairGenerator kpg = getRsaKeyPairGenerator();
    kpg.initialize(keySize);
    KeyPair kp = kpg.generateKeyPair();
    rtn.pub = kp.getPublic();
    rtn.pvt = kp.getPrivate();

    return rtn;
  }

  public static SigningKeyPair of(File privateKeyFile, File publicKeyFile) {
    return new SigningKeyPair(asBase64(privateKeyFile), asBase64(publicKeyFile));
  }

  public static SigningKeyPair ofPublic(File publicKeyFile) {
    return ofPublic(asBase64(publicKeyFile));
  }

  public static SigningKeyPair ofPublic(String publicBase64) {
    return new SigningKeyPair(publicFromBase64(publicBase64));
  }

  public static SigningKeyPair ofPrivate(File privateKeyFile) {
    return ofPrivate(asBase64(privateKeyFile));
  }

  public static SigningKeyPair ofPrivate(String privateBase64) {
    return new SigningKeyPair(privateFromBase64(privateBase64));
  }

  private static String asBase64(File keyFile) {
    Processor<String> processor = () -> {
      try (FileInputStream fis = new FileInputStream(keyFile)) {
        return new String(fis.readAllBytes(), StandardCharsets.UTF_8);
      }
    };
    return new ExceptionConverter<String>().apply(processor);
  }

  private static PublicKey publicFromBase64(String publicBase64) {
    Processor<PublicKey> processor = () -> {
      String publicKeyPem = publicBase64
          .replace("\\n", "")
          .replace("-----BEGIN PUBLIC KEY-----", "")
          .replace(System.lineSeparator(), "")
          .replace("-----END PUBLIC KEY-----", "");

      byte[] encoded = Base64.getDecoder().decode(publicKeyPem);

      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
      return keyFactory.generatePublic(keySpec);
    };
    return new ExceptionConverter<PublicKey>().apply(processor);
  }

  private static PrivateKey privateFromBase64(String privateBase64) {
    Processor<PrivateKey> processor = () -> {
      String privateKeyPem = privateBase64
          .replace("\\n", "")
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace(System.lineSeparator(), "")
          .replace("-----END PRIVATE KEY-----", "");

      byte[] encoded = Base64.getDecoder().decode(privateKeyPem);

      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
      return keyFactory.generatePrivate(keySpec);
    };

    return new ExceptionConverter<PrivateKey>().apply(processor);
  }

  @SuppressWarnings("java:S5542")
  private Cipher getRsaCipher() {
    Processor<Cipher> processor = () -> Cipher.getInstance("RSA");
    return new ExceptionConverter<Cipher>().apply(processor);
  }

  public boolean isPublic() {
    return pub != null;
  }

  public boolean isPrivate() {
    return pvt != null;
  }

  public String encryptWithPublicKey(String data) {
    return encrypt(data, this.pub);
  }

  public String encryptWithPrivateKey(String data) {
    return encrypt(data, this.pvt);
  }

  public String decryptWithPublicKey(String data) {
    return decrypt(data, this.pub);
  }

  public String decryptWithPrivateKey(String data) {
    return decrypt(data, this.pvt);
  }

  private byte[] encrypt(byte[] data, Key key) {
    var rtn = applyCipher(Cipher.ENCRYPT_MODE, data, key);
    if (rtn.length == 0) {
      throw new CompilerException("Encryption failed");
    }
    return rtn;
  }

  /**
   * Accepts a string converts to bytes encrypts and converts to base64.
   */
  private String encrypt(String data, Key key) {
    return encoder.encodeToString(encrypt(data.getBytes(StandardCharsets.UTF_8), key));
  }

  private byte[] decrypt(byte[] data, Key key) {
    var rtn = applyCipher(Cipher.DECRYPT_MODE, data, key);
    if (rtn.length == 0) {
      throw new CompilerException("Decryption failed");
    }
    return rtn;
  }

  /**
   * Accepts a base 64 string converts to bytes decrypts and converts back to String.
   */
  private String decrypt(String data, Key key) {
    byte[] decoded = decoder.decode(data.getBytes(StandardCharsets.UTF_8));
    return new String(Objects.requireNonNull(decrypt(decoded, key)), StandardCharsets.UTF_8);
  }


  private byte[] applyCipher(int encryptDecryptMode, byte[] data, Key key) {
    try {
      this.cipher.init(encryptDecryptMode, key);
      return this.cipher.doFinal(data);
    } catch (Exception ex) {
      Logger.error("Unable apply Cipher " + ex.getMessage());
      return new byte[0];
    }
  }

  /**
   * Access the private key of the signing key pair.
   */
  public String getPrivateKeyInBase64() {
    return "-----BEGIN PRIVATE KEY-----\n"
        + to64CharacterLines(encoder.encodeToString(pvt.getEncoded()))
        + "-----END PRIVATE KEY-----\n";
  }

  /**
   * Access the public key of the signing key pair.
   */
  public String getPublicKeyInBase64() {
    return "-----BEGIN PUBLIC KEY-----\n"
        + to64CharacterLines(encoder.encodeToString(pub.getEncoded()))
        +
        "-----END PUBLIC KEY-----\n";
  }

  private String to64CharacterLines(String pemText) {
    StringBuilder buffer = new StringBuilder();
    int index = 0;
    while (index < pemText.length()) {
      buffer.append(pemText, index, Math.min(index + 64, pemText.length())).append("\n");
      index += 64;
    }
    return buffer.toString();
  }
}
