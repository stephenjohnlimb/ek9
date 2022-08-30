package org.ek9lang.core.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlobTest {

  @Test
  void basicGlobbing() {
    Glob underTest = new Glob();
    assertFalse(underTest.isAcceptable("src/Foo.java"));

    underTest.addInclude("**/*.java");
    assertTrue(underTest.isAcceptable("src/Foo.java"));
    assertTrue(underTest.isAcceptable("src/Bar.java"));
    assertTrue(underTest.isAcceptable("src/much/deeper/path/Bar.java"));
    assertFalse(underTest.isAcceptable("src/much/deeper/path/Foo.txt"));

    assertFalse(underTest.isAcceptable("Foo.java"));

    underTest.addExclude("**/Bar.*");
    assertTrue(underTest.isAcceptable("src/Foo.java"));
    assertFalse(underTest.isAcceptable("src/Bar.java"));
    assertFalse(underTest.isAcceptable("src/much/deeper/path/Bar.java"));
  }

  @Test
  void groupGlobbing() {
    Glob underTest = new Glob();
    underTest.addInclude("**.{html,htm}");
    underTest.addInclude("**/*.png");
    underTest.addInclude("**/{*.gif,*.jpg,*.jpeg}");

    underTest.addExclude("**/craft.*");

    assertFalse(underTest.isAcceptable("src/Foo.java"));
    assertFalse(underTest.isAcceptable("src/much/deeper/path/Bar.java"));

    assertTrue(underTest.isAcceptable("index.html"));
    assertTrue(underTest.isAcceptable("area51/allowed.html"));
    assertTrue(underTest.isAcceptable("area50/access.htm"));

    assertFalse(underTest.isAcceptable("area50/craft.png"));
    assertTrue(underTest.isAcceptable("area50/hangar.png"));

    assertFalse(underTest.isAcceptable("area50/craft.gif"));

    assertTrue(underTest.isAcceptable("area50/secure/hangar.jpg"));
  }

  @Test
  void pathPartGlobbing() {
    Glob underTest = new Glob();
    underTest.addInclude("**.{html,htm}");
    underTest.addInclude("**/info/**.{txt,properties}");
    assertTrue(underTest.isAcceptable("index.html"));
    assertFalse(underTest.isAcceptable("details.txt"));
    assertFalse(underTest.isAcceptable("location/details.txt"));
    assertTrue(underTest.isAcceptable("location/info/details.txt"));
    assertTrue(underTest.isAcceptable("location/layered/info/with/more/dirs/details.txt"));
  }

  @Test
  void exampleGlobbing() {
    Glob underTest = new Glob();
    underTest.addInclude("sample/images/*.{png,jpeg}");
    underTest.addInclude("**.{txt,cal}");

    underTest.addExclude("sample/images/{perch,nonSuch}.png");

    assertTrue(underTest.isAcceptable("basic.txt"));
    assertFalse(underTest.isAcceptable("basic.text"));
    assertTrue(underTest.isAcceptable("basic.cal"));
    assertTrue(underTest.isAcceptable("subdirectory/basic.cal"));
    assertFalse(underTest.isAcceptable("basic.calendar"));

    assertFalse(underTest.isAcceptable("subdirectory/image.png"));

    assertTrue(underTest.isAcceptable("sample/images/image.png"));
    assertTrue(underTest.isAcceptable("sample/images/perchy.png"));

    assertFalse(underTest.isAcceptable("sample/images/perch.png"));
  }

  @Test
  void onlySourceFilesGlobbing() {
    Glob underTest = new Glob();
    //underTest.addInclude("*.ek9");
    underTest.addInclude("**.ek9");

    underTest.addExclude("**.ek9/**"); //for .ek9 directories exclusion

    assertTrue(underTest.isAcceptable("Basic.ek9"));
    assertTrue(underTest.isAcceptable("other/directory/dev/ok/down/here/Basic.ek9"));
    assertTrue(underTest.isAcceptable("dev/directory/Basic.ek9"));

    assertFalse(underTest.isAcceptable("basic.txt"));
    assertFalse(underTest.isAcceptable("some/dir/basic.txt"));

    assertFalse(underTest.isAcceptable(".ek9/lots/of/dirs/Some.ek9"));

    //No longer want was is in dev directory
    underTest.addExclude("dev/**.ek9");
    assertTrue(underTest.isAcceptable("Basic.ek9"));
    assertTrue(underTest.isAcceptable("other/directory/dev/ok/down/here/Basic.ek9"));
    assertFalse(underTest.isAcceptable("dev/directory/Basic.ek9"));
    assertFalse(underTest.isAcceptable("basic.txt"));
    assertFalse(underTest.isAcceptable("some/dir/basic.txt"));
  }

  @Test
  void dotFileGlobbing() {
    Glob underTest = new Glob();
    //Everything
    underTest.addInclude("**");
    assertTrue(underTest.isAcceptable("Basic.ek9"));
    assertTrue(underTest.isAcceptable(".ek9/lots/of/dirs/Some.java"));
    assertTrue(underTest.isAcceptable("basic.txt"));
    assertTrue(underTest.isAcceptable("/some/place/basic.txt"));
    assertTrue(underTest.isAcceptable("/some/place/.aws/credentials"));
    assertTrue(underTest.isAcceptable(".git/lots/of/dirs"));
    assertTrue(underTest.isAcceptable(".aws/credentials"));
    assertTrue(underTest.isAcceptable(".gitignore"));
    assertTrue(underTest.isAcceptable("/a/directory/with/.gitignore"));

    //Now lets do some excludes
    underTest.addExclude("**.ek9/**");
    underTest.addExclude(".ek9"); //for just directory when empty
    underTest.addExclude("**.git/**");
    underTest.addExclude("**.aws/**");
    underTest.addExclude("**.gitignore");

    //one is root and one in another directory
    assertTrue(underTest.isAcceptable("Basic.ek9"));
    assertTrue(underTest.isAcceptable("other/directory/Basic.ek9"));
    //empty .ek9 dir
    assertFalse(underTest.isAcceptable(".ek9/"));
    assertFalse(underTest.isAcceptable(".ek9/lots/of/dirs/Some.java"));
    assertFalse(underTest.isAcceptable("lots/of/.ek9/dirs/Some.java"));
    assertFalse(underTest.isAcceptable("lots/of/.ek9/dirs/Some.ek9"));

    assertTrue(underTest.isAcceptable("basic.txt"));
    assertTrue(underTest.isAcceptable("/some/place/basic.txt"));
    assertFalse(underTest.isAcceptable("/some/place/.aws/credentials"));
    assertFalse(underTest.isAcceptable(".aws/credentials"));
    assertFalse(underTest.isAcceptable(".git/lots/of/dirs"));
    assertFalse(underTest.isAcceptable(".gitignore"));
    assertFalse(underTest.isAcceptable("/a/directory/with/.gitignore"));
  }
}
