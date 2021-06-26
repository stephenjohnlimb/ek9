package org.ek9lang.core.utils;

import junit.framework.TestCase;
import org.junit.Test;

public class GlobTest
{

    @Test
    public void basicGlobbing()
    {
        Glob underTest = new Glob();
        TestCase.assertFalse(underTest.isAcceptable("src/Foo.java"));

        underTest.addInclude("**/*.java");
        TestCase.assertTrue(underTest.isAcceptable("src/Foo.java"));
        TestCase.assertTrue(underTest.isAcceptable("src/Bar.java"));
        TestCase.assertTrue(underTest.isAcceptable("src/much/deeper/path/Bar.java"));
        TestCase.assertFalse(underTest.isAcceptable("src/much/deeper/path/Foo.txt"));

        TestCase.assertFalse(underTest.isAcceptable("Foo.java"));

        underTest.addExclude("**/Bar.*");
        TestCase.assertTrue(underTest.isAcceptable("src/Foo.java"));
        TestCase.assertFalse(underTest.isAcceptable("src/Bar.java"));
        TestCase.assertFalse(underTest.isAcceptable("src/much/deeper/path/Bar.java"));
    }

    @Test
    public void groupGlobbing()
    {
        Glob underTest = new Glob();
        underTest.addInclude("**.{html,htm}");
        underTest.addInclude("**/*.png");
        underTest.addInclude("**/{*.gif,*.jpg,*.jpeg}");

        underTest.addExclude("**/craft.*");

        TestCase.assertFalse(underTest.isAcceptable("src/Foo.java"));
        TestCase.assertFalse(underTest.isAcceptable("src/much/deeper/path/Bar.java"));

        TestCase.assertTrue(underTest.isAcceptable("index.html"));
        TestCase.assertTrue(underTest.isAcceptable("area51/allowed.html"));
        TestCase.assertTrue(underTest.isAcceptable("area50/access.htm"));

        TestCase.assertFalse(underTest.isAcceptable("area50/craft.png"));
        TestCase.assertTrue(underTest.isAcceptable("area50/hangar.png"));

        TestCase.assertFalse(underTest.isAcceptable("area50/craft.gif"));

        TestCase.assertTrue(underTest.isAcceptable("area50/secure/hangar.jpg"));
    }

    @Test
    public void pathPartGlobbing()
    {
        Glob underTest = new Glob();
        underTest.addInclude("**.{html,htm}");
        underTest.addInclude("**/info/**.{txt,properties}");
        TestCase.assertTrue(underTest.isAcceptable("index.html"));
        TestCase.assertFalse(underTest.isAcceptable("details.txt"));
        TestCase.assertFalse(underTest.isAcceptable("location/details.txt"));
        TestCase.assertTrue(underTest.isAcceptable("location/info/details.txt"));
        TestCase.assertTrue(underTest.isAcceptable("location/layered/info/with/more/dirs/details.txt"));
    }

    @Test
    public void exampleGlobbing()
    {
        Glob underTest = new Glob();
        underTest.addInclude("sample/images/*.{png,jpeg}");
        underTest.addInclude("**.{txt,cal}");

        underTest.addExclude("sample/images/{perch,nonSuch}.png");

        TestCase.assertTrue(underTest.isAcceptable("basic.txt"));
        TestCase.assertFalse(underTest.isAcceptable("basic.text"));
        TestCase.assertTrue(underTest.isAcceptable("basic.cal"));
        TestCase.assertTrue(underTest.isAcceptable("subdirectory/basic.cal"));
        TestCase.assertFalse(underTest.isAcceptable("basic.calendar"));

        TestCase.assertFalse(underTest.isAcceptable("subdirectory/image.png"));

        TestCase.assertTrue(underTest.isAcceptable("sample/images/image.png"));
        TestCase.assertTrue(underTest.isAcceptable("sample/images/perchy.png"));

        TestCase.assertFalse(underTest.isAcceptable("sample/images/perch.png"));
    }
    
    @Test
    public void onlySourceFilesGlobbing()
    {
        Glob underTest = new Glob();
        //underTest.addInclude("*.ek9");
        underTest.addInclude("**.ek9");
        
        underTest.addExclude("**.ek9/**"); //for .ek9 directories exclusion        
        
        TestCase.assertTrue(underTest.isAcceptable("Basic.ek9"));
        TestCase.assertTrue(underTest.isAcceptable("other/directory/dev/ok/down/here/Basic.ek9"));
        TestCase.assertTrue(underTest.isAcceptable("dev/directory/Basic.ek9"));
        
        TestCase.assertFalse(underTest.isAcceptable("basic.txt"));
        TestCase.assertFalse(underTest.isAcceptable("some/dir/basic.txt"));
        
        TestCase.assertFalse(underTest.isAcceptable(".ek9/lots/of/dirs/Some.ek9"));        
        
        //No longer want was is in dev directory
        underTest.addExclude("dev/**.ek9");
        TestCase.assertTrue(underTest.isAcceptable("Basic.ek9"));
        TestCase.assertTrue(underTest.isAcceptable("other/directory/dev/ok/down/here/Basic.ek9"));
        TestCase.assertFalse(underTest.isAcceptable("dev/directory/Basic.ek9"));
        TestCase.assertFalse(underTest.isAcceptable("basic.txt"));
        TestCase.assertFalse(underTest.isAcceptable("some/dir/basic.txt"));
    }
    
    @Test
    public void dotFileGlobbing()
    {
        Glob underTest = new Glob();
        //Everything
        underTest.addInclude("**");
        TestCase.assertTrue(underTest.isAcceptable("Basic.ek9"));
        TestCase.assertTrue(underTest.isAcceptable(".ek9/lots/of/dirs/Some.java"));
        TestCase.assertTrue(underTest.isAcceptable("basic.txt"));
        TestCase.assertTrue(underTest.isAcceptable("/some/place/basic.txt"));
        TestCase.assertTrue(underTest.isAcceptable("/some/place/.aws/credentials"));        
        TestCase.assertTrue(underTest.isAcceptable(".git/lots/of/dirs"));
        TestCase.assertTrue(underTest.isAcceptable(".aws/credentials")); 
        TestCase.assertTrue(underTest.isAcceptable(".gitignore"));
        TestCase.assertTrue(underTest.isAcceptable("/a/directory/with/.gitignore"));
        
        //Now lets do some excludes
        underTest.addExclude("**.ek9/**");
        underTest.addExclude(".ek9"); //for just directory when empty
        underTest.addExclude("**.git/**");        
        underTest.addExclude("**.aws/**");
        underTest.addExclude("**.gitignore");
        
        //one is root and one in another directory
        TestCase.assertTrue(underTest.isAcceptable("Basic.ek9"));
        TestCase.assertTrue(underTest.isAcceptable("other/directory/Basic.ek9"));
        //empty .ek9 dir
        TestCase.assertFalse(underTest.isAcceptable(".ek9/"));
        TestCase.assertFalse(underTest.isAcceptable(".ek9/lots/of/dirs/Some.java"));
        TestCase.assertFalse(underTest.isAcceptable("lots/of/.ek9/dirs/Some.java"));
        TestCase.assertFalse(underTest.isAcceptable("lots/of/.ek9/dirs/Some.ek9"));
        
        TestCase.assertTrue(underTest.isAcceptable("basic.txt"));
        TestCase.assertTrue(underTest.isAcceptable("/some/place/basic.txt"));
        TestCase.assertFalse(underTest.isAcceptable("/some/place/.aws/credentials"));    
        TestCase.assertFalse(underTest.isAcceptable(".aws/credentials"));
        TestCase.assertFalse(underTest.isAcceptable(".git/lots/of/dirs"));
        TestCase.assertFalse(underTest.isAcceptable(".gitignore"));
        TestCase.assertFalse(underTest.isAcceptable("/a/directory/with/.gitignore"));        
    }    
}
