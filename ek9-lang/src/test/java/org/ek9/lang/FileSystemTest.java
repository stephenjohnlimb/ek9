package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FileSystemTest extends Common {

  @Test
  void testConstructionAndIsSet() {
    final var fileSystem = new FileSystem();
    assertNotNull(fileSystem);

    // FileSystem is always set
    assertTrue(fileSystem._isSet().state);
    assertSet.accept(fileSystem);
  }

  @Test
  void testCurrentWorkingDirectory() {
    final var fileSystem = new FileSystem();
    final var cwd = fileSystem.cwd();

    assertCommonPathProperties(cwd);
  }

  @Test
  void testTemporaryDirectory() {
    final var fileSystem = new FileSystem();
    final var tmp = fileSystem.tmp();

    assertCommonPathProperties(tmp);
  }

  private void assertCommonPathProperties(FileSystemPath fsPath) {
    assertNotNull(fsPath);
    assertSet.accept(fsPath);

    // Temporary directory should exist, be absolute, and be writable
    assertTrue.accept(fsPath.exists());
    assertTrue.accept(fsPath.isAbsolute());
    assertTrue.accept(fsPath.isDirectory());
    assertTrue.accept(fsPath.isReadable());
    assertTrue.accept(fsPath.isWritable());
  }

  @Test
  void testConsistentBehavior() {
    final var fileSystem = new FileSystem();
    assertNotNull(fileSystem);

    // Multiple calls should return equivalent results
    assertTrue.accept(fileSystem.cwd()._eq(fileSystem.cwd()));
    assertTrue.accept(fileSystem.tmp()._eq(fileSystem.tmp()));

    // Multiple instances should return equivalent results
    final var fileSystem2 = new FileSystem();
    assertTrue.accept(fileSystem.cwd()._eq(fileSystem2.cwd()));
    assertTrue.accept(fileSystem.tmp()._eq(fileSystem2.tmp()));

    assertFalse.accept(fileSystem.tmp()._eq(fileSystem2.cwd()));
  }
}