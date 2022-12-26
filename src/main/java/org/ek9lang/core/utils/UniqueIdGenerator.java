package org.ek9lang.core.utils;

import java.util.UUID;

/**
 * Unique ID generator.
 */
public class UniqueIdGenerator {

  private UniqueIdGenerator() {

  }

  /**
   * Provide the next unique ID.
   */
  public static String getNewUniqueId() {
    UUID newId = UUID.randomUUID();

    return newId.toString().replace("-", "_");
  }
}
