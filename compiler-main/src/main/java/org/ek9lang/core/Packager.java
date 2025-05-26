package org.ek9lang.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * Focus on the responsibility of packaging. i.e. zipping and unzipping.
 */
public final class Packager {
  private final FileHandling fileHandling;

  public Packager(final FileHandling fileHandling) {

    this.fileHandling = fileHandling;

  }

  /**
   * Create a compressed archive to file with zip sets.
   */
  public boolean createJar(final String fileName, final List<ZipSet> sets) {

    //Let exception break everything here - these are precondition.
    AssertValue.checkNotEmpty("Filename empty", fileName);
    AssertValue.checkNotNull("Zip Set cannot be null", sets);

    fileHandling.deleteFileIfExists(new File(fileName));

    final Processor<Boolean> processor = () -> {
      try (final var zip = FileSystems.newFileSystem(getZipUri(fileName), getZipEnv())) {
        sets.forEach(set -> addZipSet(zip, set));
        return true;
      }
    };

    return new ExceptionConverter<Boolean>().apply(processor);
  }

  /**
   * Unpacks a zip/jar file to a specific directory.
   */
  public boolean unZipFileTo(final File zipFile, final File unpackedDir) {

    AssertValue.checkCanReadFile("Zip File not readable", zipFile);
    fileHandling.makeDirectoryIfNotExists(unpackedDir);

    final Processor<Boolean> processor = () -> {
      try (final var fis = new FileInputStream(zipFile);
           final var zis = new ZipInputStream(fis)) {

        //buffer for read and write data to file
        final var buffer = new byte[1024];
        var ze = zis.getNextEntry();

        while (ze != null) {
          final var fileName = ze.getName();
          final var newFile = new File(unpackedDir, fileName);

          //create directories for subdirectories in zip
          fileHandling.makeDirectoryIfNotExists(new File(newFile.getParent()));
          if (ze.isDirectory()) {
            fileHandling.makeDirectoryIfNotExists(newFile);
          } else {
            try (FileOutputStream fos = new FileOutputStream(newFile)) {
              int len;
              while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
              }
            }
          }
          //close this ZipEntry
          zis.closeEntry();
          ze = zis.getNextEntry();
        }
        //close last ZipEntry
        zis.closeEntry();
      }
      return true;
    };

    return new ExceptionConverter<Boolean>().apply(processor);
  }

  /**
   * Create a zip with the zip set and copy over the properties file.
   */
  public boolean createZip(final String fileName, final ZipSet set, final File sourcePropertiesFile) {

    AssertValue.checkNotEmpty("FileName is empty", fileName);
    AssertValue.checkNotNull("Zip Set is null", set);
    AssertValue.checkCanReadFile("Properties file not readable", sourcePropertiesFile);

    final Processor<Boolean> processor = () -> {
      try (final var zip = FileSystems.newFileSystem(getZipUri(fileName), getZipEnv())) {
        addZipSet(zip, set);

        //Now to include the properties file so when the zip is unpacked we can find the name
        //of the source tha was used to trigger the packaging.
        final var externalTxtFile = Path.of(sourcePropertiesFile.getAbsolutePath());
        final var pathInZipFile = zip.getPath(".package.properties");

        Files.copy(externalTxtFile, pathInZipFile, StandardCopyOption.REPLACE_EXISTING);
      }
      return true;
    };

    return new ExceptionConverter<Boolean>().apply(processor);
  }

  private URI getZipUri(final String fileName) {

    AssertValue.checkNotEmpty("FileName is empty", fileName);
    final var toFile = new File(fileName).toURI().toString();

    return URI.create("jar:" + toFile);
  }

  private Map<String, String> getZipEnv() {

    final Map<String, String> env = new HashMap<>();
    env.put("create", "true");

    return env;
  }

  private void addZipSet(final FileSystem zip, final ZipSet set) {

    AssertValue.checkNotNull("Zip is null", zip);
    AssertValue.checkNotNull("Zip Set is null", set);

    final Processor<Boolean> processor = () -> {
      if (!set.isEmpty()) {
        if (set.isFileBased()) {
          addFileBasedSet(zip, set);
        } else if (set.isEntryBased()) {
          addEntryBasedSet(zip, set);
        }
      }

      //it is empty - which OK, just nothing to add.
      return true;
    };
    new ExceptionConverter<Boolean>().apply(processor);

  }

  private void addEntryBasedSet(final FileSystem zip, final ZipSet set) throws IOException {

    for (final var content : set.getEntries()) {

      final var pathInZipFile = zip.getPath(content.getEntryName());
      ensureParentPathExists(pathInZipFile);

      try (final var out = Files.newOutputStream(pathInZipFile)) {
        out.write(content.getContent());
      }
    }
  }

  private void addFileBasedSet(final FileSystem zip, final ZipSet set) throws IOException {

    for (final var file : set.getFiles()) {

      final var externalTxtFile = Path.of(file.getAbsolutePath());
      final var relative = set.getRelativePath().toAbsolutePath().relativize(externalTxtFile);
      final var pathInZipFile = zip.getPath(relative.toString());

      ensureParentPathExists(pathInZipFile);
      // Copy a file into the zip file
      Files.copy(externalTxtFile, pathInZipFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private void ensureParentPathExists(final Path path) throws IOException {

    if (path.getParent() != null) {
      Files.createDirectories(path.getParent());
    }

  }
}
