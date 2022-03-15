package org.ek9lang.compiler.parsing;

import java.io.File;
import java.net.URISyntaxException;

import org.ek9lang.cli.support.EK9SourceVisitor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Just test the parsing of packages using the Basic Parser used in the compiler
 * This also checks that the basic EK9 source visitor works.
 * 
 * These are both used in the command line tool as part of the packaging options.
 *
 */
public class TestPackageParsing
{
	
	
	@Test
	public void testUnableToOpenFile() throws URISyntaxException
	{
		JustParser underTest = new JustParser();
		
		File file = new File("nonSuch.ek9");		
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		boolean result = underTest.readSourceFile(file, visitor);
		assertFalse(result);
	}
	
	@Test
	public void testBadPackage() throws URISyntaxException
	{
		JustParser underTest = new JustParser();
		
		File file = new File(getClass().getResource("/examples/constructs/packages/BadPackage.ek9").toURI());		
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		boolean result = underTest.readSourceFile(file, visitor);
		assertFalse(result);		
	}
	
	@Test
	public void testUnParsablePackage() throws URISyntaxException
	{
		JustParser underTest = new JustParser();
		
		File file = new File(getClass().getResource("/badExamples/basics/unevenIndentation.ek9").toURI());		
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		boolean result = underTest.readSourceFile(file, visitor);
		assertFalse(result);		
	}
	
	@Test
	public void testSmallPackage() throws URISyntaxException
	{
		JustParser underTest = new JustParser();
		
		File file = new File(getClass().getResource("/examples/constructs/packages/HandyTools.ek9").toURI());		
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		boolean result = underTest.readSourceFile(file, visitor);
		assertTrue(result);
		assertTrue("ekopen.net.handy.tools".equals(visitor.getModuleName()));
	}
	
	@Test
	public void testFullApplicationPackage() throws URISyntaxException
	{
		JustParser underTest = new JustParser();
		
		File file = new File(getClass().getResource("/examples/fullPrograms/TCPExample.ek9").toURI());		
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		boolean result = underTest.readSourceFile(file, visitor);
		assertTrue(result);
		assertTrue("example.networking".equals(visitor.getModuleName()));
		
		assertFalse(visitor.isPublicAccess());
		assertTrue("2.3.14-20".equals(visitor.getVersion()));
		assertFalse(visitor.isApplyStandardIncludes());
		assertTrue(visitor.isApplyStandardExcludes());
		
		assertTrue(visitor.getIncludeFiles().contains("**.{txt,cal}"));
		assertTrue(visitor.getExcludeFiles().contains("sample/images/{perch.png,nonSuch.jpeg}"));
		
		assertTrue(visitor.isSemanticVersioning());
		assertEquals(25, visitor.getVersionNumberOnLine());
		assertTrue(visitor.isPackagePresent());
		assertTrue(visitor.getPrograms().size() == 4);
		assertTrue("MIT".equals(visitor.getLicense()));

		//Will always be this for the dependencies in this example (TCPExample.ek9)
		assertTrue("0C5F4976C78292B001221E300A80F414D4B6F161CF4EFBA17B66DBF7DA7E3A5A".equals(visitor.getDependencyFingerPrint()));		
	}
	
	@Test
	public void testSimplePackage() throws URISyntaxException
	{
		JustParser underTest = new JustParser();
		
		File file = new File(getClass().getResource("/examples/constructs/packages/SinglePackage.ek9").toURI());		
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		boolean result = underTest.readSourceFile(file, visitor);
		assertTrue(result);
		assertTrue("net.customer".equals(visitor.getModuleName()));
		assertTrue(visitor.isPublicAccess());
		assertTrue("1.0.0-0".equals(visitor.getVersion()));
		assertTrue("Simulation of something to exclude".equals(visitor.getDescription()));
		assertTrue(visitor.getTags().contains("tools"));
		assertTrue(visitor.getIncludeFiles().contains("**.{csv,jpeg}"));
		assertTrue(visitor.getIncludeFiles().contains("Chelford"));
		assertTrue(visitor.getIncludeFiles().contains("Guff"));
	
		assertTrue(visitor.getDeps().containsKey("ekopen.network.support.utils"));
		assertTrue(visitor.getDeps().get("ekopen.network.support.utils").equals("1.6.1-9"));
		assertTrue(visitor.getDeps().containsKey("ekopen.net.handy.tools"));
		assertTrue(visitor.getDeps().get("ekopen.net.handy.tools").equals("3.2.1-0"));
		
		assertTrue(visitor.getDevDeps().containsKey("ekopen.org.supertools.util"));
		assertTrue(visitor.getDevDeps().get("ekopen.org.supertools.util").equals("4.6.1-6"));
		assertTrue(visitor.getDevDeps().containsKey("ekopen.org.net.tools.misc"));
		assertTrue(visitor.getDevDeps().get("ekopen.org.net.tools.misc").equals("3.2.3-21"));
		
		assertTrue(visitor.getExcludeDeps().containsKey("ekopen.some.bad.dependency.pack"));
		assertTrue(visitor.getExcludeDeps().get("ekopen.some.bad.dependency.pack").equals("ekopen.org.supertools.util"));		
	}
}
