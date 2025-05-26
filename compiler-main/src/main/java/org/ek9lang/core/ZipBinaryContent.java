package org.ek9lang.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Very simple wrapper for binary contents.
 */
public record ZipBinaryContent(String entryName, byte[] content) {
  public String getEntryName() {
    return entryName;
  }

  public byte[] getContent() {
    return content;
  }

  @Override
  public boolean equals(final Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final var that = (ZipBinaryContent) o;
    return Objects.equals(entryName, that.entryName) && Arrays.equals(content, that.content);
  }

  @Override
  public int hashCode() {

    int result = Objects.hash(entryName);
    result = 31 * result + Arrays.hashCode(content);

    return result;
  }

  @Override
  public String toString() {

    return String.format("ZipBinaryContent{entryName='%s',content=%s}",
        entryName, Arrays.toString(content));
  }
}
