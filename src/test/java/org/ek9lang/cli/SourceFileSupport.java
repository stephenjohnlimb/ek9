package org.ek9lang.cli;

import junit.framework.TestCase;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;

import java.io.File;
import java.net.URL;

public class SourceFileSupport
{
	private final OsSupport osSupport;
	private final FileHandling fileHandling;

	public SourceFileSupport(FileHandling fileHandling, OsSupport osSupport)
	{
		this.fileHandling = fileHandling;
		this.osSupport = osSupport;
	}

	public File copyFileToTestCWD(String fromRelativeTestUrl, String ek9SourceFileName)
	{
		URL fileForTest = this.getClass().getResource(fromRelativeTestUrl+ek9SourceFileName);
		TestCase.assertNotNull(fileForTest);
		File example = new File(fileForTest.getPath());

		//Now we can put a dummy source file in the simulated/test cwd and then try and process it.
		File cwd = new File(osSupport.getCurrentWorkingDirectory());
		TestCase.assertTrue(cwd.exists());
		File aSourceFile = new File(cwd, ek9SourceFileName);
		if(!aSourceFile.exists())
		{
			TestCase.assertTrue(fileHandling.copy(new File(example.getParent()), cwd, ek9SourceFileName));
			TestCase.assertTrue(aSourceFile.exists());
		}


		return aSourceFile;
	}
}
