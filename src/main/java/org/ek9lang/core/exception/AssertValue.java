package org.ek9lang.core.exception;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import org.ek9lang.core.utils.OsSupport;

/**
 * Used as simple one-liners to check a value and issue an illegal argument exception if empty.
 */
public class AssertValue {
  private static final OsSupport osSupport = new OsSupport();

  private AssertValue() {
    //Just to stop instantiation.
  }

  /**
   * A check on range of value.
   */
  public static void checkRange(String messageIfOutside, Integer valueToCheck, Integer min,
                                Integer max) {
    if (valueToCheck == null) {
      throw new IllegalArgumentException(messageIfOutside);
    }
    if (min != null && valueToCheck < min) {
      throw new IllegalArgumentException(messageIfOutside);
    }
    if (max != null && valueToCheck > max) {
      throw new IllegalArgumentException(messageIfOutside);
    }
  }

  /**
   * Not null of exception error.
   */
  public static void checkNotNull(String messageIfNull, Object valueToCheck) {
    if (valueToCheck == null) {
      throw new IllegalArgumentException(messageIfNull);
    }
  }

  /**
   * Check filename valid and file can be read.
   */
  public static void checkCanReadFile(String messageIfNoRead, String fileName) {
    checkNotEmpty("Filename cannot be empty or null", fileName);
    if (!osSupport.isFileReadable(fileName)) {
      throw new IllegalArgumentException(messageIfNoRead + "[" + fileName + "]");
    }
  }

  /**
   * Check file not null valid and file can be read.
   */
  public static void checkCanReadFile(String messageIfNoRead, File file) {
    checkNotNull("File cannot be be null", file);
    if (!osSupport.isFileReadable(file)) {
      throw new IllegalArgumentException(messageIfNoRead + "[" + file.getPath() + "]");
    }
  }

  /**
   * Check directory name valid and directory can be read.
   */
  public static void checkDirectoryReadable(String messageIfNoRead, String directoryName) {
    checkNotEmpty("Filename cannot be empty or null", directoryName);
    if (!osSupport.isDirectoryReadable(directoryName)) {
      throw new IllegalArgumentException(messageIfNoRead + "[" + directoryName + "]");
    }
  }

  /**
   * Check directory name valid and directory can be read.
   */
  public static void checkDirectoryReadable(String messageIfNoRead, File dir) {
    AssertValue.checkNotNull(messageIfNoRead, dir);
    if (!osSupport.isDirectoryReadable(dir)) {
      throw new IllegalArgumentException(messageIfNoRead + "[" + dir.getPath() + "]");
    }
  }

  /**
   * Check directory name valid and directory can be written to.
   */
  public static void checkDirectoryWritable(String messageIfNoWrite, String directoryName) {
    AssertValue.checkNotNull(messageIfNoWrite, directoryName);
    if (!osSupport.isDirectoryWritable(directoryName)) {
      throw new IllegalArgumentException(messageIfNoWrite + "[" + directoryName + "]");
    }
  }

  /**
   * Check directory name valid and directory can be written to.
   */
  public static void checkDirectoryWritable(String messageIfNoWrite, File dir) {
    AssertValue.checkNotNull(messageIfNoWrite, dir);
    if (!osSupport.isDirectoryWritable(dir)) {
      throw new IllegalArgumentException(messageIfNoWrite + "[" + dir.getPath() + "]");
    }
  }

  /**
   * Check item not null and not empty.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static void checkNotEmpty(String messageIfEmpty, Optional<?> item) {
    checkNotNull(messageIfEmpty, item);
    if (item.isEmpty()) {
      throw new IllegalArgumentException(messageIfEmpty);
    }
  }

  /**
   * Checks if a string value is null or an empty string and if so issues an
   * illegal argument exception.
   *
   * @param valueToCheck The value to check.
   */
  public static void checkNotEmpty(String messageIfEmpty, String valueToCheck) {
    if (valueToCheck == null || "".equals(valueToCheck)) {
      throw new IllegalArgumentException(messageIfEmpty);
    }
  }

  /**
   * Checks if a string values is null or empty strings and if so issues an
   * illegal argument exception.
   *
   * @param valuesToCheck The values to check.
   */
  public static void checkNotEmpty(String messageIfEmpty, String[] valuesToCheck) {
    if (valuesToCheck == null || valuesToCheck.length == 0) {
      throw new IllegalArgumentException(messageIfEmpty);
    }
    for (String value : valuesToCheck) {
      AssertValue.checkNotEmpty(messageIfEmpty, value);
    }
  }

  /**
   * Checks if a collection of items is null or empty and if so issues an
   * illegal argument exception.
   *
   * @param items The values to check.
   */
  public static void checkNotEmpty(String messageIfEmpty, Collection<?> items) {
    checkNotNull(messageIfEmpty, items);
    if (items.isEmpty()) {
      throw new IllegalArgumentException(messageIfEmpty);
    }
    items.forEach(item -> checkNotNull(messageIfEmpty, item));
  }

  /**
   * Assets that a value is true or illegal argument exception.
   */
  public static void checkTrue(String messageIfFalse, boolean value) {
    if (!value) {
      throw new IllegalArgumentException(messageIfFalse);
    }
  }

  /**
   * Asserts that the value is false, illegal argument exception if not.
   */
  public static void checkFalse(String messageIfTrue, boolean value) {
    if (value) {
      throw new IllegalArgumentException(messageIfTrue);
    }
  }

  /**
   * Just trigger a direct failure.
   *
   * @param withMessage The message to issue.
   */
  public static void fail(String withMessage) {
    throw new IllegalArgumentException(withMessage);
  }
}
