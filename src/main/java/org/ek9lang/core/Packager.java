package org.ek9lang.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Focus on the responsibility of packaging. i.e. zipping and unzipping.
 */
public final class Packager {
  private final FileHandling fileHandling;

  public Packager(FileHandling fileHandling) {
    this.fileHandling = fileHandling;
  }

  /**
   * Create a compressed archive to file with zip sets.
   */
  public boolean createJar(String fileName, List<ZipSet> sets) {
    //Let exception break everything here - these are precondition.
    AssertValue.checkNotEmpty("Filename empty", fileName);
    AssertValue.checkNotNull("Zip Set cannot be null", sets);
    fileHandling.deleteFileIfExists(new File(fileName));

    Processor<Boolean> processor = () -> {
      try (FileSystem zip = FileSystems.newFileSystem(getZipUri(fileName), getZipEnv())) {
        sets.forEach(set -> addZipSet(zip, set));
        return true;
      }
    };
    return new ExceptionConverter<Boolean>().apply(processor);
  }

  /**
   * Unpacks a zip/jar file to a specific directory.
   */
  public boolean unZipFileTo(File zipFile, File unpackedDir) {
    //Preconditions
    AssertValue.checkCanReadFile("Zip File not readable", zipFile);
    fileHandling.makeDirectoryIfNotExists(unpackedDir);

    Processor<Boolean> processor = () -> {
      try (FileInputStream fis = new FileInputStream(zipFile);
           ZipInputStream zis = new ZipInputStream(fis)) {
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
          String fileName = ze.getName();
          File newFile = new File(unpackedDir, fileName);
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
  public boolean createZip(String fileName, ZipSet set, File sourcePropertiesFile) {
    //Preconditions
    AssertValue.checkNotEmpty("FileName is empty", fileName);
    AssertValue.checkNotNull("Zip Set is null", set);
    AssertValue.checkCanReadFile("Properties file not readable", sourcePropertiesFile);

    Processor<Boolean> processor = () -> {
      try (FileSystem zip = FileSystems.newFileSystem(getZipUri(fileName), getZipEnv())) {
        addZipSet(zip, set);

        //Now to include the properties file so when the zip is unpacked we can find the name
        //of the source tha was used to trigger the packaging.
        Path externalTxtFile = Path.of(sourcePropertiesFile.getAbsolutePath());
        Path pathInZipFile = zip.getPath(".package.properties");
        Files.copy(externalTxtFile, pathInZipFile, StandardCopyOption.REPLACE_EXISTING);
      }
      return true;
    };
    return new ExceptionConverter<Boolean>().apply(processor);
  }

  private URI getZipUri(String fileName) {
    AssertValue.checkNotEmpty("FileName is empty", fileName);
    String toFile = new File(fileName).toURI().toString();
    return URI.create("jar:" + toFile);
  }

  private Map<String, String> getZipEnv() {
    Map<String, String> env = new HashMap<>();
    env.put("create", "true");
    return env;
  }

  private void addZipSet(final FileSystem zip, final ZipSet set) {
    AssertValue.checkNotNull("Zip is null", zip);
    AssertValue.checkNotNull("Zip Set is null", set);

    Processor<Boolean> processor = () -> {
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
    for (ZipBinaryContent content : set.getEntries()) {
      Path pathInZipFile = zip.getPath(content.getEntryName());
      ensureParentPathExists(pathInZipFile);

      try (OutputStream out = Files.newOutputStream(pathInZipFile)) {
        out.write(content.getContent());
      }
    }
  }

  private void addFileBasedSet(final FileSystem zip, final ZipSet set) throws IOException {

    for (File file : set.getFiles()) {
      Path externalTxtFile = Path.of(file.getAbsolutePath());
      Path relative = set.getRelativePath().toAbsolutePath().relativize(externalTxtFile);

      Path pathInZipFile = zip.getPath(relative.toString());
      ensureParentPathExists(pathInZipFile);
      // Copy a file into the zip file
      Files.copy(externalTxtFile, pathInZipFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private void ensureParentPathExists(Path path) throws IOException {
    if (path.getParent() != null) {
      Files.createDirectories(path.getParent());
    }
  }
}
