package org.ek9lang.cli;

import java.io.File;
import java.util.Optional;
import org.ek9lang.compiler.common.Ek9SourceVisitor;
import org.ek9lang.compiler.common.JustParser;
import org.ek9lang.compiler.common.Reporter;
import org.ek9lang.core.AssertValue;

/**
 * Once the EK9 'Edp' dependency module has determined that a packaged module now needs to be
 * resolved it will call upon this resolver to make sure that it is available.
 * <p>
 * This resolver will initially look in the users $HOME/.ek9/lib directory for a zip file
 * matching the vector for the packages, module i.e. 'ekopen.network.support.utils-1.6.1-9.zip'
 * </p>
 * <p>
 * If that is not present then it will make a https request to repo.ek9lang.com
 * to obtain that zip file, and hash of zip fingerprint.
 * The manual steps for this are:
 * </p>
 * <pre>
 * curl https://repo.ek9lang.org/ekopen.net.handy.tools-3.2.1-0.zip -o ekopen.net.handy.tools-3.2.1-0.zip
 * curl https://repo.ek9lang.org/ekopen.net.handy.tools-3.2.1-0.zip.sha256 -o ekopen.net.handy.tools-3.2.1-0.zip.sha256
 * cat ekopen.net.handy.tools-3.2.1-0.zip | shasum -a 256 -c ekopen.net.handy.tools-3.2.1-0.zip.sha256
 * </pre>
 * <p>
 * Note when the publisher of the package uploaded the zip, they did a couple of extra bits
 * to be able to provide a secure copy of the hash of the zip.
 * See SigningKeyPair.doubleEncryption as an example of this.
 * </p>
 * <p>
 * They used their private key to encrypt the hash of the zip they created.
 * They then used the public key of the repo server to encrypt that data.
 * That ensures that only the server with its private key can decrypt that payload, but then
 * only by using the publishers public key can the inner payload be decrypted to reveal the hash.
 * That hash can then be checked against a re-run of hashing of the zip file.
 * </p>
 * <p>
 * On the repo server, the zip is taken and put to one side for virus scanning and later processing.
 * The first layer of encryption is decrypted by the repo serer using its own private key.
 * This ensures that the data decrypted (the still encrypted hash and un-encrypted client public
 * key) has not been tampered with.
 * </p>
 * <p>
 * Now the provided client public key can be used to decrypt the encrypted hash.
 * That hash value can be checked against the hash calculated against the zip.
 * If the zip is virus free, then the zip, the encrypted (with the client private key) hash and
 * the clients public key are all stored on the S3 server.
 * </p>
 * <p>
 * So when the zip is downloaded, this resolver will use the same hashing routine to calculate
 * the fingerprint.
 * It will also get the clients public key to decrypt the encrypted hash and check that the hash
 * values match.
 * </p>
 * <p>
 * <b>Anyway that's the general idea.</b>
 * </p>
 */
final class PackageResolver extends Reporter {

  private final CommandLine commandLine;

  PackageResolver(final CommandLine commandLine, final boolean muteReportedErrors) {

    super(commandLine.options().isVerbose(), muteReportedErrors);
    this.commandLine = commandLine;

  }

  @Override
  protected String messagePrefix() {

    return "Resolve : ";
  }

  /**
   * Provides a source visitor for a packages dependency, so it it has
   * already been resolved and unpacked, it will be returned.
   * But if it needs to be downloaded then it will be pulled down, unpacked and returned.
   */
  Optional<Ek9SourceVisitor> resolve(final String dependencyVector) {

    log("Checking '" + dependencyVector + "'");

    final var zipFileName = commandLine.getFileHandling().makePackagedModuleZipFileName(dependencyVector);
    final var homeEk9Lib = commandLine.getFileHandling().getUsersHomeEk9LibDirectory();
    final var unpackedDir = new File(homeEk9Lib, dependencyVector);
    final var zipFile = new File(homeEk9Lib, zipFileName);

    //See if it is already unpackaged and available
    if (commandLine.getOsSupport().isDirectoryReadable(unpackedDir)) {
      log("Already unpacked '" + dependencyVector + "'");
      return Optional.ofNullable(processPackageProperties(unpackedDir));
    }

    if (!commandLine.getOsSupport().isFileReadable(zipFile) && !downloadDependency(dependencyVector)) {
      report("'" + dependencyVector + "' cannot be resolved!");
      return Optional.empty();
    }

    //So it should now have been downloaded we can un-package.
    if (commandLine.getOsSupport().isFileReadable(zipFile)) {
      log("Unpacking '" + zipFile + "'");
      if (unZip(zipFile, unpackedDir)) {
        return Optional.ofNullable(processPackageProperties(unpackedDir));
      }
    }

    return Optional.empty();
  }

  boolean downloadDependency(final String dependencyVector) {

    AssertValue.checkNotNull("DependencyVector cannot be null", dependencyVector);

    //TODO the download part
    return false;
  }

  /**
   * Load .package.properties file from the unpacked dir and fet the property 'sourceFile'
   * This will tell us which of the sources of ek9 files is the one containing the package
   * directive to use.
   *
   * @param unpackedDir The directory where the zip is unpacked to.
   * @return A Visitor with all the details of the package from the source file.
   */
  private Ek9SourceVisitor processPackageProperties(final File unpackedDir) {

    final var propertiesFile = new File(unpackedDir, ".package.properties");
    log("Loading '" + propertiesFile + "'");

    final var properties = new Ek9ProjectProperties(propertiesFile).loadProperties().getProperty("sourceFile");
    final var src = new File(unpackedDir, properties);

    log("SourceFile '" + src + "'");
    if (commandLine.getOsSupport().isFileReadable(src)) {
      return loadFileAndVisit(src);
    }

    report("Unable to read sourceFile '" + src + "'");
    return null;
  }

  private boolean unZip(final File zipFile, final File unpackedDir) {

    return commandLine.getFileHandling().unZipFileTo(zipFile, unpackedDir);
  }

  private Ek9SourceVisitor loadFileAndVisit(final File sourceFile) {

    final var visitor = new Ek9SourceVisitor();
    if (!new JustParser(true).readSourceFile(sourceFile, visitor)) {
      report("Unable to Parse source file [" + sourceFile.getAbsolutePath() + "]");
      return null;
    }

    return visitor;
  }
}
