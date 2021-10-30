package org.ek9lang.compiler.files;

public interface Source
{
    String getFileName();

    default boolean isDev() { return false; }

    default boolean isLib() { return false; }
}
