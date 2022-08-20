package org.ek9lang.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.utils.Digest;
import org.ek9lang.core.utils.SigningKeyPair;

/**
 * Deploy a package / all packages inside the project directory.
 */
public class Ed extends E {
  public Ed(CommandLineDetails commandLine, FileCache sourceFileCache) {
    super(commandLine, sourceFileCache);
  }

  @Override
  protected String messagePrefix() {
    return "Deploy  : ";
  }

  protected boolean doRun() {
    if (new Ep(commandLine, sourceFileCache).run() && new Egk(commandLine, sourceFileCache).run()) {
      //Need to ensure that the user has some signing keys.

      if (!prepareEncryptedZipHash()) {
        report("Unable to complete package deployment");
        return false;
      }
      //Still TODO
      //OK now we can zip the zip, encrypted hash and clients public key and send

      //Also needs an account with some credentials to send to https://deploy.ek9lang.org
      //TODO - we will leave this for now - see SigningKeyPairTest on how we will do it.
      return true;

    }
    return false;
  }

  /**
   * Once zipped, packaging and everything is ready.
   * This method can take the sha256 file and double encrypt it ready to accompany
   * the zip and the clients public key.
   */
  private boolean prepareEncryptedZipHash() {
    String zipFileName =
        getFileHandling().makePackagedModuleZipFileName(commandLine.getModuleName(),
            commandLine.getVersion());
    File zipFile =
        new File(getFileHandling().getDotEk9Directory(commandLine.getSourceFileDirectory()),
            zipFileName);
    File sha256File =
        new File(getFileHandling().getDotEk9Directory(commandLine.getSourceFileDirectory()),
            zipFileName + ".sha256");

    if (zipFile.exists() && sha256File.exists()) {
      log("Signing deployment package");
      //Yes hard coded again - revisit later maybe
      String serverPublicKey = getServerPublicKey("repo.ek9lang.org");
      if (serverPublicKey == null) {
        report("Unable to sign package for deployment");
      } else {
        SigningKeyPair usersSigningKeyPair = getFileHandling().getUsersSigningKeyPair();
        if (usersSigningKeyPair == null) {
          report("Unable to load users signing keys");
        } else {
          SigningKeyPair halfKeyPair = SigningKeyPair.ofPublic(serverPublicKey);
          //OK ready to go!
          String plainHashText = Digest.digest(sha256File).toString();
          String innerCipherText = usersSigningKeyPair.encryptWithPrivateKey(plainHashText);
          String finalCipherText = halfKeyPair.encryptWithPublicKey(innerCipherText);
          File sha256EncFile =
              new File(getFileHandling().getDotEk9Directory(commandLine.getSourceFileDirectory()),
                  zipFileName + ".sha256.enc");

          try (FileOutputStream fos = new FileOutputStream((sha256EncFile))) {
            fos.write(finalCipherText.getBytes(StandardCharsets.UTF_8));
            log("Deployment package signed [" + sha256EncFile.getPath() + "]");
            return true;
          } catch (Exception ex) {
            report("Failed to create signed package " + ex.getMessage());
          }
        }
      }
    } else {
      report("Failed to find deployable zip/sha files");
    }
    return false;
  }

  /**
   * For now; we will embed this public key in this deployment.
   * Eventually we will pull a public key from a repo server.
   *
   * @param serverName The server to pull the public certificate from i.e. repo.ek9lang.org
   * @return The servers public certificate.
   */
  private String getServerPublicKey(String serverName) {
    AssertValue.checkNotNull("ServerName cannot be null", serverName);
    //TODO actually get te server public key

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
}
