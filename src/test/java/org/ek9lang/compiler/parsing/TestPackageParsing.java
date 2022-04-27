package org.ek9lang.compiler.parsing;

import org.ek9lang.cli.support.EK9SourceVisitor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Just test the parsing of packages using the Basic Parser used in the compiler
 * This also checks that the basic EK9 source visitor works.
 * <p>
 * These are both used in the command line tool as part of the packaging options.
 */
public class TestPackageParsing
{


	@Test
	public void testUnableToOpenFile()
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
		var actual = visitor.getPackageDetails().map(details -> details.moduleName()).orElse("");
		assertEquals("ekopen.net.handy.tools", actual);
	}

	@Test
	public void testFullApplicationPackage() throws URISyntaxException
	{
		JustParser underTest = new JustParser();

		File file = new File(getClass().getResource("/examples/fullPrograms/TCPExample.ek9").toURI());
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		boolean result = underTest.readSourceFile(file, visitor);
		assertTrue(result);
		visitor.getPackageDetails().ifPresentOrElse(details -> {
			assertEquals("example.networking", details.moduleName());
			assertFalse(details.publicAccess());
			assertEquals("2.3.14-20", details.version());

			assertFalse(details.applyStandardIncludes());
			assertTrue(details.applyStandardExcludes());

			assertTrue(details.includeFiles().contains("**.{txt,cal}"));
			assertTrue(details.excludeFiles().contains("sample/images/{perch.png,nonSuch.jpeg}"));

			assertEquals(25, details.versionNumberOnLine());
			assertTrue(details.packagePresent());
			assertEquals(4, details.programs().size());
			assertEquals("MIT", details.license());

			//Will always be this for the dependencies in this example (TCPExample.ek9)
			assertEquals("0C5F4976C78292B001221E300A80F414D4B6F161CF4EFBA17B66DBF7DA7E3A5A", details.dependencyFingerPrint());
		}, () -> fail("No package Details"));
	}

	@Test
	public void testSimplePackage() throws URISyntaxException
	{
		JustParser underTest = new JustParser();

		File file = new File(getClass().getResource("/examples/constructs/packages/SinglePackage.ek9").toURI());
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		boolean result = underTest.readSourceFile(file, visitor);
		assertTrue(result);
		visitor.getPackageDetails().ifPresentOrElse(details -> {
			assertEquals("net.customer", details.moduleName());
			assertEquals("1.0.0-0", details.version());
			assertTrue(details.publicAccess());
			assertEquals("Simulation of something to exclude", details.description());

			assertTrue(details.tags().contains("tools"));
			assertTrue(details.includeFiles().contains("**.{csv,jpeg}"));
			assertTrue(details.includeFiles().contains("Chelford"));
			assertTrue(details.includeFiles().contains("Guff"));

			assertTrue(details.deps().containsKey("ekopen.network.support.utils"));
			assertEquals("1.6.1-9", details.deps().get("ekopen.network.support.utils"));
			assertTrue(details.deps().containsKey("ekopen.net.handy.tools"));
			assertEquals("3.2.1-0", details.deps().get("ekopen.net.handy.tools"));

			assertTrue(details.devDeps().containsKey("ekopen.org.supertools.util"));
			assertEquals("4.6.1-6", details.devDeps().get("ekopen.org.supertools.util"));
			assertTrue(details.devDeps().containsKey("ekopen.org.net.tools.misc"));
			assertEquals("3.2.3-21", details.devDeps().get("ekopen.org.net.tools.misc"));

			assertTrue(details.excludeDeps().containsKey("ekopen.some.bad.dependency.pack"));
			assertEquals("ekopen.org.supertools.util", details.excludeDeps().get("ekopen.some.bad.dependency.pack"));
		}, () -> fail("No package Details"));
	}
}
