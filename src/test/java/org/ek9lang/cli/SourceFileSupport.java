package org.ek9lang.cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
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
		assertNotNull(fileForTest);
		File example = new File(fileForTest.getPath());

		//Now we can put a dummy source file in the simulated/test cwd and then try and process it.
		File cwd = new File(osSupport.getCurrentWorkingDirectory());
		assertTrue(cwd.exists());
		File aSourceFile = new File(cwd, ek9SourceFileName);
		if(!aSourceFile.exists())
		{
			assertTrue(fileHandling.copy(new File(example.getParent()), cwd, ek9SourceFileName));
			assertTrue(aSourceFile.exists());
		}


		return aSourceFile;
	}
}
