package org.ek9lang.core.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;

class SigningKeyPairTest
{

	@Test
	void encryptPrivateDecryptPublic()
	{
		//Just check we can encrypt and decrypt
		String theMessage = "The quick brown fox jumps over the lazy dog";

		SigningKeyPair keyPair = SigningKeyPair.generate(2048);
		String cipherText = keyPair.encryptWithPrivateKey(theMessage);
		//System.out.println("[" + cipherText + "]");

		String plainText = keyPair.decryptWithPublicKey(cipherText);
		//System.out.println("[" + plainText +"]");
		assertEquals(theMessage, plainText);
	}


	@Test
	void encryptPublicDecryptPrivate()
	{
		//Just check we can encrypt and decrypt
		String theMessage = "The quick brown fox jumps over the lazy dog";

		SigningKeyPair keyPair = SigningKeyPair.generate(2048);
		String cipherText = keyPair.encryptWithPublicKey(theMessage);
		//System.out.println("[" + cipherText + "]");

		String plainText = keyPair.decryptWithPrivateKey(cipherText);
		//System.out.println("[" + plainText +"]");
		assertEquals(theMessage, plainText);
	}

	@Test
	void doInvalidEncryption()
	{
		assertThrows(java.lang.RuntimeException.class, () -> {
			String theMessage = "The quick brown fox jumps over the lazy dog";

			SigningKeyPair keyPair = SigningKeyPair.generate(2048);
			String cipherText = keyPair.encryptWithPublicKey(theMessage);
			//Now incorrectly use wrong key
			keyPair.decryptWithPublicKey(cipherText);
		});
	}

	@Test
	void doubleEncryption()
	{
		//I know this seems a bit pointless, but this is basically how you can
		//pass secure data and be assured that it came from a source.

		//But please note that the server key must be much larger (twice the size)
		//So that it can handle the length of the cipher text.
		//I think we need to have client keys at 1024 (quite short) and server keys at 2048.
		//If we move to 4096 - the encryption is noticeably slower (as you would expect).

		//So we're not actually sending the message but a hash of the message here.
		//This simulates the fact we'd be sending a hash of the zip file.

		String theMessage = "The quick brown fox jumps over the lazy dog";
		String toSend = Digest.digest(theMessage) + " *-";

		//Two key pairs needed for this one for server and one for client.
		SigningKeyPair simulatedServer = SigningKeyPair.generate(2048);
		SigningKeyPair simulatedClient = SigningKeyPair.generate(1024);

		//Scenario is the that client wants to send a secure message (hash) to the server.
		//The server really needs to be assured that it was the client that sent it.
		//The client also has reference to the servers public key and the
		//server has reference to the clients public key.
		//This is possible because they are 'public keys'!

		//Now the client encrypts with its private key - so only the clients public key can be used to decrypt this.
		//But 'everyone' has the clients public key - so anyone can decrypt it!
		String innerCipherText = simulatedClient.encryptWithPrivateKey(toSend);

		byte[] bytes = innerCipherText.getBytes(StandardCharsets.UTF_8);
		assertTrue(bytes.length > 0);
		
		//But the client also has the servers public key and so encrypts the encrypted message!
		String transmittableCipherText = simulatedServer.encryptWithPublicKey(innerCipherText);

		//So now only the server can decrypt the message to reveal the inner encrypted message.

		//Now on the server - the server code decrypts the message using its private key
		String clientsCipherText = simulatedServer.decryptWithPrivateKey(transmittableCipherText);

		//So it is still cipher text - we must now decrypt that with the clients public key
		String hashInPlainText = simulatedClient.decryptWithPublicKey(clientsCipherText);
		assertEquals(toSend, hashInPlainText);

		//So in the above scenario.
		//Client has public/private key pair (1024 length)- generated with ek9 -Gk some public.pem and private.pem are generated
		//and stored in $HOME/.ek9

		//Server has public/private key pair (2048 length) - private is kept safe, public is put on repo.ek9lang.org

		//client creates zip and sha256 of that zip.
		//As shown above client encrypts sha256 file with its private key, then crypts that with servers public key.
		//Now it can send the zip, the double encrypted hash AND its own public key all via https.

		//Now on the server, the server uses its own private key and then the supplied client public key to get the
		//hash. It can then hash the zip file using sha256 and check the values match!
		//If they match, then the file contents have not been altered and the supplied hash must have come from the
		//same client that supplied the zip. The server can now put the zip and the sha1 hash on the reo site once it
		//has been scanned for viruses.
	}

	@Test
	void testPublicPrivateKeySerialise()
	{
		SigningKeyPair underTest = SigningKeyPair.generate(2048);
		
		String publicPem = underTest.getPubBase64();
		String privatePem = underTest.getPvtBase64();

		SigningKeyPair reverseTest = new SigningKeyPair(privatePem, publicPem);
		assertNotNull(reverseTest);
		
		String checkPublicPem = reverseTest.getPubBase64();
		String checkPrivatePem = reverseTest.getPvtBase64();
		
		assertEquals(publicPem, checkPublicPem);
		assertEquals(privatePem, checkPrivatePem);
	}

	static String getOpenSSLGeneratedPrivateKey()
	{
		return """
				-----BEGIN PRIVATE KEY-----
				MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDPAohiIpO6iE94
				q++J/AxXoqP/UwjtXp81mvXBemgdQMUukzaKVkag1FxfWzaVUZIXoCCFuoLJqTQz
				hmxoQxiWk8CYRguH09iJeUKGR4hJ6ayJzMVbQHuD/FyKkXNUIQiO0979gxPgz7+6
				yHQ9UFcCLuOxdAKDGd8O6ALtkNaKe08GZq0xJOJVTZ0y374IgITB/4xG9DGzXHtv
				8yir1W9yfuEZbXiem2aKZh0UZH+hmqRKrP9cRXYyUDaC29r6tlTNjA2D86+0Gc7x
				hDeFayWr3+aIM3/nYdF4KVH7MS9mNl0AqMWYKm+w6d35iPTBZ6a9dIUxXhND6KXZ
				bZUPx1AVAgMBAAECggEBAMzFOJu6kF9oDxTTHzRjOuJFY+xg+uezPdIM4yTsLKkj
				NOoJfV4CUgMtjeJVm/zAn773CrS99jn9QkHlqr7IAD6TEWSPhUBq8juq0394qvxS
				RfZZtK+7bXL146S+lFKYCpseQeqMCtHLL2FYgINWQXtLXrhehU2h/eAE6UCOkWqR
				HZ3dMf24hFfGP/RgCMOikPl/JZQ+HlwJALWhqf4APnV12cr/PsFFz4YKG6eEtQ2t
				FI34zlienx0OINIws53LJf8plknY2Ee3fBObhyYSuGkWfsTMMsoBq/CTCO4FN5/R
				ctJsr1JnJUujD3nZlPdM57qetmS+hfCnAhR2iqPP1wECgYEA/vfTH9enA1ugeMHj
				xTepcQlgvoM0A+z6CMLunUrbbJXrR8vcxa9/UEbdGM1s4zdrdtovn/NxWzIMYA6u
				7lT26C9gn8dvb4F8gf0C3GVUOQQzUWVhZl5QFletgys8FP9+o8ZfvSUkqH9feNqo
				jiyDFXksEahYcOKzPz5fmac890UCgYEAz9kEnjEnzuTpHpYq0TuzSB11kKNOwXy/
				bxh7YGZy6MKvr7q9mVvcSQQLzEZ12PdDNp7UKyLVj0ozEjUc/slrUDVJyDtb6oUZ
				qRg8kiKgEXWnVscRYPRiD1cj/nKk4ug0xGlXqy38YYGHQVL3Z2I6lBygVZKzfIFJ
				Kol0sFqdWpECgYBXZhov4xtz8jeqzUPHedISVlWcWJs+8T+wOynSLzPSH/3byYzv
				Er6AhRoLA3fc10V2I0qJ+MgVq61ETCQ4vFU6t+rWOmX/ghJs+I6/f9iEBuWUdD0T
				+yZW0k0/FlXtGLuPZlOxIYdUXv0aD1ISEj4I4VVy5BMU9xDckAPzRrmGRQKBgQCm
				y+Cx1n68wOT3f8kSqhdAyytajEOacLLrw7jW7sBOXFZC7thSpu5Lxix1nSHboOpW
				ffWEPGsnl4MOnIMNULSG5Iy2XDRyKqgiE+of2Bueh01oDU68AMJkzyh6fKQn1/Lo
				oCUToU7FLP5PBPa4B5M84xAkPBs/0jzWHwL+t2lJkQKBgQCc6X1q/1MHUUYzuZ28
				MsSTVFSH0u/AvBsJKBriqyEgZcJ70IGnKE7sy0WqARpzsMuxUmWenK5CKLFeGvy9
				Ny4iN20CP0kIuvwAUPHQGl862BZbzBq21IoLynEgC+kq0lvpdpmUZnz6shfiG/2U
				lB+Wd5IMvyl9u41WOP9ESjZ6Cg==
				-----END PRIVATE KEY-----
				""";
	}

	static String getOpenSSLGeneratedPublicKey()
	{
		return """
				-----BEGIN PUBLIC KEY-----
				MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzwKIYiKTuohPeKvvifwM
				V6Kj/1MI7V6fNZr1wXpoHUDFLpM2ilZGoNRcX1s2lVGSF6AghbqCyak0M4ZsaEMY
				lpPAmEYLh9PYiXlChkeISemsiczFW0B7g/xcipFzVCEIjtPe/YMT4M+/ush0PVBX
				Ai7jsXQCgxnfDugC7ZDWintPBmatMSTiVU2dMt++CICEwf+MRvQxs1x7b/Moq9Vv
				cn7hGW14nptmimYdFGR/oZqkSqz/XEV2MlA2gtva+rZUzYwNg/OvtBnO8YQ3hWsl
				q9/miDN/52HReClR+zEvZjZdAKjFmCpvsOnd+Yj0wWemvXSFMV4TQ+il2W2VD8dQ
				FQIDAQAB
				-----END PUBLIC KEY-----
				""";
	}

	@Test
	void testOpenSSLGeneratedFromFile()
	{
		URL rootDirectoryForTest = this.getClass().getResource("/forSigningKeyTests");
		assertNotNull(rootDirectoryForTest);

		File publicFile = new File(rootDirectoryForTest.getPath(), "publicKey.pem");
		File privateFile = new File(rootDirectoryForTest.getPath(), "privateKey.pem");

		SigningKeyPair openTest = SigningKeyPair.of(privateFile, publicFile);
		assertNotNull(openTest);

		SigningKeyPair privateSigningKey = SigningKeyPair.ofPrivate(privateFile);
		assertNotNull(privateSigningKey);
		SigningKeyPair publicSigningKey = SigningKeyPair.ofPublic(publicFile);
		assertNotNull(publicSigningKey);
	}

	@Test
	void testNullPublicKeyFile()
	{
		assertFalse(SigningKeyPair.ofPublic((File)null).isPublic());
		assertFalse(SigningKeyPair.ofPublic((File)null).isPrivate());
	}

	@Test
	void testOpenSSLGenerated()
	{
		//Generates using openSSL
		//openssl genrsa -out mykey.pem 2048
		//openssl pkcs8 -topk8 -inform PEM -outform PEM -in mykey.pem     -out private_key.pem -nocrypt
		//openssl rsa -in mykey.pem -pubout > public_key.pem
		
		String publicPem = getOpenSSLGeneratedPublicKey();
		
		String privatePem = getOpenSSLGeneratedPrivateKey();
		
		SigningKeyPair openTest = new SigningKeyPair(privatePem, publicPem);
		assertNotNull(openTest);
		
		String checkPublicPem = openTest.getPubBase64();
		String checkPrivatePem = openTest.getPvtBase64();
		
		//System.out.println(checkPublicPem);
		//System.out.println(checkPrivatePem);
		
		assertEquals(publicPem, checkPublicPem);
		assertEquals(privatePem, checkPrivatePem);
	}
}
