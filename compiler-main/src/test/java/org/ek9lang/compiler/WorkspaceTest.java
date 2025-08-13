package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class WorkspaceTest {

  @Test
  void testWorkspaceCreation() {
    var workspace = new Workspace();
    assertNotNull(workspace);
  }

  @Test
  void testCheckingSources() {
    var workspace = new Workspace();
    assertTrue(workspace.getSources().isEmpty());

    URL helloWorld = WorkspaceTest.class.getResource("/examples/parseAndCompile/basics/HelloWorld.ek9");
    assertNotNull(helloWorld);
    Path path = Path.of(helloWorld.getPath());

    workspace.addSource(path);
    assertEquals(1, workspace.getSources().size());

    //add same again and check only present once.
    workspace.addSource(path);
    assertEquals(1, workspace.getSources().size());

    var compilableSource = workspace.getSource(path);
    assertNotNull(compilableSource);
    //Now remove it
    workspace.removeSource(path);
    assertTrue(workspace.getSources().isEmpty());

  }

}
