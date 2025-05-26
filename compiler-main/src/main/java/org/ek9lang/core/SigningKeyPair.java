package org.ek9lang.core;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
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

  private SigningKeyPair(final PublicKey pub) {

    this();
    this.pub = pub;

  }

  private SigningKeyPair(final PrivateKey pvt) {

    this();
    this.pvt = pvt;

  }

  private SigningKeyPair() {

    cipher = getRsaCipher();

  }

  /**
   * Create a signing key pair from the private and public parts of a PKI key pair.
   */
  public SigningKeyPair(final String privateBase64, final String publicBase64) {

    pvt = privateFromBase64(privateBase64);
    pub = publicFromBase64(publicBase64);
    cipher = getRsaCipher();

  }

  private static KeyPairGenerator getRsaKeyPairGenerator() {

    final Processor<KeyPairGenerator> processor = () -> KeyPairGenerator.getInstance("RSA");

    return new ExceptionConverter<KeyPairGenerator>().apply(processor);
  }

  /**
   * Create a new signing key pair of a particular key size.
   */
  public static SigningKeyPair generate(final int keySize) {

    final var rtn = new SigningKeyPair();
    final var kpg = getRsaKeyPairGenerator();
    kpg.initialize(keySize);
    final var kp = kpg.generateKeyPair();

    rtn.pub = kp.getPublic();
    rtn.pvt = kp.getPrivate();

    return rtn;
  }

  public static SigningKeyPair of(final File privateKeyFile, final File publicKeyFile) {

    return new SigningKeyPair(asBase64(privateKeyFile), asBase64(publicKeyFile));
  }

  public static SigningKeyPair ofPublic(final File publicKeyFile) {

    return ofPublic(asBase64(publicKeyFile));
  }

  public static SigningKeyPair ofPublic(final String publicBase64) {

    return new SigningKeyPair(publicFromBase64(publicBase64));
  }

  public static SigningKeyPair ofPrivate(final File privateKeyFile) {

    return ofPrivate(asBase64(privateKeyFile));
  }

  public static SigningKeyPair ofPrivate(final String privateBase64) {

    return new SigningKeyPair(privateFromBase64(privateBase64));
  }

  private static String asBase64(final File keyFile) {

    final Processor<String> processor = () -> {
      try (final var fis = new FileInputStream(keyFile)) {
        return new String(fis.readAllBytes(), StandardCharsets.UTF_8);
      }
    };

    return new ExceptionConverter<String>().apply(processor);
  }

  private static PublicKey publicFromBase64(final String publicBase64) {

    final Processor<PublicKey> processor = () -> {
      final var publicKeyPem = publicBase64
          .replace("\\n", "")
          .replace("-----BEGIN PUBLIC KEY-----", "")
          .replace(System.lineSeparator(), "")
          .replace("-----END PUBLIC KEY-----", "");

      final var encoded = Base64.getDecoder().decode(publicKeyPem);
      final var keyFactory = KeyFactory.getInstance("RSA");
      final var keySpec = new X509EncodedKeySpec(encoded);

      return keyFactory.generatePublic(keySpec);
    };

    return new ExceptionConverter<PublicKey>().apply(processor);
  }

  private static PrivateKey privateFromBase64(final String privateBase64) {
    final Processor<PrivateKey> processor = () -> {
      final var privateKeyPem = privateBase64
          .replace("\\n", "")
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace(System.lineSeparator(), "")
          .replace("-----END PRIVATE KEY-----", "");

      final var encoded = Base64.getDecoder().decode(privateKeyPem);
      final var keyFactory = KeyFactory.getInstance("RSA");
      final var keySpec = new PKCS8EncodedKeySpec(encoded);

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

  public String encryptWithPublicKey(final String data) {

    return encrypt(data, this.pub);
  }

  public String encryptWithPrivateKey(final String data) {

    return encrypt(data, this.pvt);
  }

  public String decryptWithPublicKey(final String data) {

    return decrypt(data, this.pub);
  }

  public String decryptWithPrivateKey(final String data) {

    return decrypt(data, this.pvt);
  }

  private byte[] encrypt(final byte[] data, final Key key) {

    final var rtn = applyCipher(Cipher.ENCRYPT_MODE, data, key);
    if (rtn.length == 0) {
      throw new CompilerException("Encryption failed");
    }

    return rtn;
  }

  /**
   * Accepts a string converts to bytes encrypts and converts to base64.
   */
  private String encrypt(final String data, final Key key) {

    return encoder.encodeToString(encrypt(data.getBytes(StandardCharsets.UTF_8), key));
  }

  private byte[] decrypt(final byte[] data, final Key key) {

    final var rtn = applyCipher(Cipher.DECRYPT_MODE, data, key);
    if (rtn.length == 0) {
      throw new CompilerException("Decryption failed");
    }

    return rtn;
  }

  /**
   * Accepts a base 64 string converts to bytes decrypts and converts back to String.
   */
  private String decrypt(final String data, final Key key) {

    final var decoded = decoder.decode(data.getBytes(StandardCharsets.UTF_8));

    return new String(Objects.requireNonNull(decrypt(decoded, key)), StandardCharsets.UTF_8);
  }


  private byte[] applyCipher(final int encryptDecryptMode, final byte[] data, final Key key) {

    try {
      this.cipher.init(encryptDecryptMode, key);
      return this.cipher.doFinal(data);
    } catch (Exception ex) {
      throw new CompilerException("Unable apply Cipher " + ex.getMessage());
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
        + "-----END PUBLIC KEY-----\n";
  }

  private String to64CharacterLines(final String pemText) {

    final var buffer = new StringBuilder();
    int index = 0;
    while (index < pemText.length()) {
      buffer.append(pemText, index, Math.min(index + 64, pemText.length())).append("\n");
      index += 64;
    }

    return buffer.toString();
  }
}
