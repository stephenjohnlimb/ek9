package org.ek9lang.compiler.parsing;

import java.io.File;
import java.net.URISyntaxException;

import org.ek9lang.cli.EK9SourceVisitor;
import org.junit.Test;

import junit.framework.TestCase;

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
		TestCase.assertFalse(result);
	}
	
	@Test
	public void testBadPackage() throws URISyntaxException
	{
		JustParser underTest = new JustParser();
		
		File file = new File(getClass().getResource("/examples/constructs/packages/BadPackage.ek9").toURI());		
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		boolean result = underTest.readSourceFile(file, visitor);
		TestCase.assertFalse(result);		
	}
	
	@Test
	public void testSmallPackage() throws URISyntaxException
	{
		JustParser underTest = new JustParser();
		
		File file = new File(getClass().getResource("/examples/constructs/packages/HandyTools.ek9").toURI());		
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		boolean result = underTest.readSourceFile(file, visitor);
		TestCase.assertTrue(result);
		TestCase.assertTrue("ekopen.net.handy.tools".equals(visitor.getModuleName()));
	}
	
	@Test
	public void testSimplePackage() throws URISyntaxException
	{
		JustParser underTest = new JustParser();
		
		File file = new File(getClass().getResource("/examples/constructs/packages/SinglePackage.ek9").toURI());		
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		boolean result = underTest.readSourceFile(file, visitor);
		TestCase.assertTrue(result);
		TestCase.assertTrue("net.customer".equals(visitor.getModuleName()));
		TestCase.assertTrue(visitor.isPublicAccess());
		TestCase.assertTrue("1.0.0-0".equals(visitor.getVersion()));
		TestCase.assertTrue("Simulation of something to exclude".equals(visitor.getDescription()));
		TestCase.assertTrue(visitor.getTags().contains("tools"));
		TestCase.assertTrue(visitor.getIncludeFiles().contains("**.{csv,jpeg}"));
		TestCase.assertTrue(visitor.getIncludeFiles().contains("Chelford"));
		TestCase.assertTrue(visitor.getIncludeFiles().contains("Guff"));
	
		TestCase.assertTrue(visitor.getDeps().containsKey("ekopen.network.support.utils"));
		TestCase.assertTrue(visitor.getDeps().get("ekopen.network.support.utils").equals("1.6.1-9"));
		TestCase.assertTrue(visitor.getDeps().containsKey("ekopen.net.handy.tools"));
		TestCase.assertTrue(visitor.getDeps().get("ekopen.net.handy.tools").equals("3.2.1-0"));
		
		TestCase.assertTrue(visitor.getDevDeps().containsKey("ekopen.org.supertools.util"));
		TestCase.assertTrue(visitor.getDevDeps().get("ekopen.org.supertools.util").equals("4.6.1-6"));
		TestCase.assertTrue(visitor.getDevDeps().containsKey("ekopen.org.net.tools.misc"));
		TestCase.assertTrue(visitor.getDevDeps().get("ekopen.org.net.tools.misc").equals("3.2.3-21"));
		
		TestCase.assertTrue(visitor.getExcludeDeps().containsKey("ekopen.some.bad.dependency.pack"));
		TestCase.assertTrue(visitor.getExcludeDeps().get("ekopen.some.bad.dependency.pack").equals("ekopen.org.supertools.util"));
		
		
		
	}
}
