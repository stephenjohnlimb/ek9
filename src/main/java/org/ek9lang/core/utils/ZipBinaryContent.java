package org.ek9lang.core.utils;

import java.util.Arrays;
import java.util.Objects;

/**
 * Very simple wrapper for binary contents.
 */
public final record ZipBinaryContent(String entryName, byte[] content) {
  public String getEntryName() {
    return entryName;
  }

  public byte[] getContent() {
    return content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZipBinaryContent that = (ZipBinaryContent) o;
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
    return new StringBuilder("ZipBinaryContent{")
        .append("entryName='")
        .append(entryName)
        .append("'")
        .append(", content=")
        .append(Arrays.toString(content))
        .append("}").toString();
  }
}
