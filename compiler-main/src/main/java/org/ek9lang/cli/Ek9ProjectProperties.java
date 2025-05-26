package org.ek9lang.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.List;
import java.util.Properties;
import org.ek9lang.core.ExceptionConverter;
import org.ek9lang.core.Processor;

/**
 * Designed only to be used on the front end of the compiler to access
 * properties files that are in the .ek9 directory just off the same directory as the
 * file that the compiler was asked to compile. This code will cause System exit if it is
 * not possible to continue (i.e. it is not designed to be tolerant at all).
 * i.e. /some/path/to/project/MyAceProgram.ek9
 * We're looking in /some/path/to/project/.ek9/*.properties
 */
final class Ek9ProjectProperties {
  private final File file;

  Ek9ProjectProperties(final File propertiesFile) {

    this.file = propertiesFile;

  }

  boolean exists() {

    return file.exists();
  }

  String getFileName() {

    return file.getName();
  }

  String prepareListForStorage(final List<String> list) {

    return String.join(",", list);
  }

  boolean isNewerThan(final File sourceFile) {

    return file.exists() && file.lastModified() > sourceFile.lastModified();
  }

  /**
   * Load up all the properties and return them.
   */
  Properties loadProperties() {
    final var properties = new Properties();

    final Processor<Boolean> processor = () -> {
      try (final var reader = new FileReader(file)) {
        properties.load(reader);
        return true;
      }
    };

    new ExceptionConverter<Boolean>().apply(processor);
    return properties;
  }

  /**
   * Save the properties to the configured file.
   */
  void storeProperties(final Properties properties) {

    final Processor<Boolean> processor = () -> {
      try (final var output = new FileOutputStream(file)) {
        properties.store(output, "Package Properties");
        return true;
      }
    };

    new ExceptionConverter<Boolean>().apply(processor);
  }
}