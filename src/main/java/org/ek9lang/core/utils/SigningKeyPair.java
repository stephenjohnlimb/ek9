package org.ek9lang.core.utils;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

/**
 * Just a wrapper around java public private key processing.
 */
public class SigningKeyPair
{
	private final Base64.Encoder encoder = Base64.getEncoder();
	private final Base64.Decoder decoder = Base64.getDecoder();
	private final Cipher cipher;
	private PublicKey pub;
	private PrivateKey pvt;

	public static SigningKeyPair generate(int keySize)
	{
		SigningKeyPair rtn = null;

		try
		{
			rtn = new SigningKeyPair();
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(keySize);
			KeyPair kp = kpg.generateKeyPair();
			rtn.pub = kp.getPublic();
			rtn.pvt = kp.getPrivate();
		}
		catch(NoSuchAlgorithmException e)
		{
			System.err.println("Failed to create public private key pair: " + e.getMessage());
		}

		return rtn;
	}

	public static SigningKeyPair of(File privateKeyFile, File publicKeyFile)
	{
		String privatePemContents;
		String publicPemContents;

		try(FileInputStream fis = new FileInputStream(privateKeyFile))
		{
			privatePemContents = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch(Throwable th)
		{
			System.err.println("Failed to open private key file: " + th.getMessage());
			return null;
		}
		try(FileInputStream fis = new FileInputStream(publicKeyFile))
		{
			publicPemContents = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch(Throwable th)
		{
			System.err.println("Failed to open public key file: " + th.getMessage());
			return null;
		}

		return new SigningKeyPair(privatePemContents, publicPemContents);
	}

	public static SigningKeyPair ofPublic(File publicKeyFile)
	{
		try(FileInputStream fis = new FileInputStream(publicKeyFile))
		{
			return ofPublic(new String(fis.readAllBytes(), StandardCharsets.UTF_8));
		}
		catch(Throwable th)
		{
			System.err.println("Failed to open public key file: " + th.getMessage());
			return null;
		}
	}

	public static SigningKeyPair ofPublic(String publicBase64)
	{
		SigningKeyPair rtn = new SigningKeyPair();
		rtn.pub = publicFromBase64(publicBase64);
		return rtn;
	}

	public static SigningKeyPair ofPrivate(File privateKeyFile)
	{
		try(FileInputStream fis = new FileInputStream(privateKeyFile))
		{
			return ofPrivate(new String(fis.readAllBytes(), StandardCharsets.UTF_8));
		}
		catch(Throwable th)
		{
			System.err.println("Failed to open private key file: " + th.getMessage());
			return null;
		}
	}

	public static SigningKeyPair ofPrivate(String privateBase64)
	{
		SigningKeyPair rtn = new SigningKeyPair();
		rtn.pvt = privateFromBase64(privateBase64);
		return rtn;
	}

	private SigningKeyPair()
	{
		cipher = getRSACipher();
	}

	public SigningKeyPair(String privateBase64, String publicBase64)
	{
		pvt = privateFromBase64(privateBase64);
		pub = publicFromBase64(publicBase64);
		cipher = getRSACipher();
	}

	private Cipher getRSACipher()
	{
		try
		{
			return Cipher.getInstance("RSA");
		}
		catch(Throwable th)
		{
			System.err.println("Unable to get RSA Cipher");
		}
		return null;
	}

	private static PublicKey publicFromBase64(String publicBase64)
	{
		try
		{
			String publicKeyPEM = publicBase64
					.replaceAll("\\n", "")
					.replace("-----BEGIN PUBLIC KEY-----", "")
					.replaceAll(System.lineSeparator(), "")
					.replace("-----END PUBLIC KEY-----", "");

			byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
			return keyFactory.generatePublic(keySpec);
		}
		catch(Throwable th)
		{
			System.err.println("Unable to load pubic key " + th.getMessage());
		}
		return null;
	}

	private static PrivateKey privateFromBase64(String privateBase64)
	{
		try
		{
			String privateKeyPEM = privateBase64
					.replaceAll("\\n", "")
					.replace("-----BEGIN PRIVATE KEY-----", "")
					.replaceAll(System.lineSeparator(), "")
					.replace("-----END PRIVATE KEY-----", "");

			byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
			return keyFactory.generatePrivate(keySpec);
		}
		catch(Throwable th)
		{
			System.err.println("Unable to load private key " + th.getMessage());
		}
		return null;
	}

	public String encryptWithPublicKey(String data)
	{
		return encrypt(data, this.pub);
	}

	public String encryptWithPrivateKey(String data)
	{
		return encrypt(data, this.pvt);
	}

	public String decryptWithPublicKey(String data)
	{
		return decrypt(data, this.pub);
	}

	public String decryptWithPrivateKey(String data)
	{
		return decrypt(data, this.pvt);
	}

	/**
	 * Accepts a string converts to bytes encrypts and converts to base64.
	 */
	private String encrypt(String data, Key key)
	{
		return encoder.encodeToString(encrypt(data.getBytes(StandardCharsets.UTF_8), key));
	}

	private byte[] encrypt(byte[] data, Key key)
	{
		try
		{
			this.cipher.init(Cipher.ENCRYPT_MODE, key);
			return this.cipher.doFinal(data);
		}
		catch(Throwable th)
		{
			System.err.println("Unable encrypt " + th.getMessage());
			return null;
		}
	}

	/*
	 * Accepts a base 64 string converts to bytes decrypts and converts back to String.
	 */
	private String decrypt(String data, Key key)
	{
		byte[] decoded = decoder.decode(data.getBytes(StandardCharsets.UTF_8));
		return new String(Objects.requireNonNull(decrypt(decoded, key)), StandardCharsets.UTF_8);
	}

	private byte[] decrypt(byte[] data, Key key)
	{
		try
		{
			this.cipher.init(Cipher.DECRYPT_MODE, key);
			return this.cipher.doFinal(data);
		}
		catch(Throwable th)
		{
			System.err.println("Unable decrypt " + th.getMessage());
			return null;
		}
	}

	public String getPvtBase64()
	{
		return "-----BEGIN PRIVATE KEY-----\n" +
				to64CharacterLines(encoder.encodeToString(pvt.getEncoded())) +
				"-----END PRIVATE KEY-----\n";
	}

	public String getPubBase64()
	{
		return "-----BEGIN PUBLIC KEY-----\n" +
				to64CharacterLines(encoder.encodeToString(pub.getEncoded())) +
				"-----END PUBLIC KEY-----\n";
	}

	private String to64CharacterLines(String pemText)
	{
		StringBuilder buffer = new StringBuilder();
		int index = 0;
		while(index < pemText.length())
		{
			buffer.append(pemText, index, Math.min(index + 64, pemText.length())).append("\n");
			index += 64;
		}
		return buffer.toString();
	}
}
