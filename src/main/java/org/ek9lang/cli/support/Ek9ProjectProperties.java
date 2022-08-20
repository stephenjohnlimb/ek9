package org.ek9lang.cli.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import org.ek9lang.core.utils.Logger;

/**
 * Designed only to be used on the front end of the compiler to access
 * properties files that are in the .ek9 directory just off the same directory as the
 * file that the compiler was asked to compile. This code will cause System exit if it is
 * not possible to continue (i.e. it is not designed to be tolerant at all).
 * i.e. /some/path/to/project/MyAceProgram.ek9
 * We're looking in /some/path/to/project/.ek9/*.properties
 */
public class Ek9ProjectProperties {
  private final File file;

  public Ek9ProjectProperties(File propertiesFile) {
    this.file = propertiesFile;
  }

  public boolean exists() {
    return file.exists();
  }

  public String getFileName() {
    return file.getName();
  }

  public String prepareListForStorage(List<String> list) {
    return String.join(",", list);
  }

  public boolean isNewerThan(File sourceFile) {
    return file.exists() && file.lastModified() > sourceFile.lastModified();
  }

  /**
   * Load up all the properties and return them.
   */
  public Properties loadProperties() {
    try (FileReader reader = new FileReader(file)) {
      Properties properties = new Properties();
      properties.load(reader);
      return properties;
    } catch (Exception ex) {
      Logger.error("Unable to load properties " + file.getName() + " " + ex.getMessage());
      System.exit(3);
    }
    //Can't get here because of exit.
    return new Properties();
  }

  /**
   * Save the properties to the configured file.
   */
  public void storeProperties(Properties properties) {
    try (OutputStream output = new FileOutputStream(file)) {
      properties.store(output, "Package Properties");
    } catch (Exception ex) {
      Logger.error("Unable to save properties " + file.getName() + " " + ex.getMessage());
      System.exit(3);
    }
  }
}