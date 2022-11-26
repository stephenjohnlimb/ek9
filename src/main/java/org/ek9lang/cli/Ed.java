package org.ek9lang.cli;

import java.io.File;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.ek9lang.cli.support.CompilationContext;
import org.ek9lang.core.utils.Digest;
import org.ek9lang.core.utils.SigningKeyPair;

/**
 * Deploy a package / all packages inside the project directory.
 */
public class Ed extends E {

  private static final String REPO_URL = "repo.ek9lang.org";

  public Ed(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Deploy  : ";
  }

  protected boolean doRun() {

    return Optional.of(new Ep(compilationContext).run()).stream()
        .filter(result -> result)
        .map(result -> new Egk(compilationContext).run())
        .filter(result -> result)
        .map(result -> getFileHandling()
            .makePackagedModuleZipFileName(compilationContext.commandLine().getModuleName(),
                compilationContext.commandLine().getVersion()))
        .map(this::prepareEncryptedZipHash)
        .findAny()
        .orElse(false);


    //Still to be done
    //OK now we can zip the zip, encrypted hash and clients public key and send

    //Also needs an account with some credentials to send to https://deploy.ek9lang.org
    //We will leave this for now - see SigningKeyPairTest on how we will do it.

  }

  /**
   * Once zipped, packaging and everything is ready.
   * This method can take the sha256 file and double encrypt it ready to accompany
   * the zip and the clients public key.
   */
  private boolean prepareEncryptedZipHash(final String fileName) {

    final UnaryOperator<String> getServerPublicKey = serverName ->
        //Actually get the server public key - for now hard code
        """
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

    final Predicate<String> zipFileExists = zipFileName -> new File(
        getFileHandling().getDotEk9Directory(
            compilationContext.commandLine().getSourceFileDirectory()),
        zipFileName).exists();

    final Function<String, File> toSha256File = zipFileName -> new File(
        getFileHandling().getDotEk9Directory(
            compilationContext.commandLine().getSourceFileDirectory()),
        zipFileName + ".sha256");

    final Predicate<String> sha256FileExists =
        zipFileName -> toSha256File.apply(zipFileName).exists();

    final Function<File, String> toPlainHashText =
        sha256File -> Digest.digest(sha256File).toString();

    final UnaryOperator<String> byUserSigning =
        plainHashText -> getFileHandling().getUsersSigningKeyPair()
            .encryptWithPrivateKey(plainHashText);

    final UnaryOperator<String> byServerPublicKey =
        innerCipherText -> SigningKeyPair.ofPublic(getServerPublicKey.apply(REPO_URL))
            .encryptWithPublicKey(innerCipherText);

    final Function<String, Boolean> saveEncryptedContents = finalCipherText -> {
      File sha256EncFile =
          new File(getFileHandling().getDotEk9Directory(
              compilationContext.commandLine().getSourceFileDirectory()),
              fileName + ".sha256.enc");
      var rtn = getFileHandling().saveToOutput(sha256EncFile, finalCipherText);

      if (rtn) {
        log("Deployment package signed [" + sha256EncFile.getPath() + "]");
      }
      return rtn;
    };

    //Now the actual processing.
    return Optional.of(fileName).stream()
        .filter(zipFileExists)
        .filter(sha256FileExists)
        .map(toSha256File)
        .map(toPlainHashText)
        .map(byUserSigning)
        .map(byServerPublicKey)
        .map(saveEncryptedContents)
        .findAny()
        .orElse(false);
  }
}
