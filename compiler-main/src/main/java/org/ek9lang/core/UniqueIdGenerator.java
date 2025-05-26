package org.ek9lang.core;

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

    final var newId = UUID.randomUUID();

    return newId.toString().replace("-", "_");
  }
}
