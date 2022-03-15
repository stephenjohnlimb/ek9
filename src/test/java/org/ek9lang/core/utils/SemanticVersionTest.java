package org.ek9lang.core.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SemanticVersionTest
{

	@Test
	public void testInvalidSemanticVersionNumber()
	{		
		assertNull(SemanticVersion._withNoBuildNumber("1.2.A"));
		
		assertNull(SemanticVersion._of("1.2.A"));
	}
	
	@Test
	public void testSemenaticVersionWithoutBuildNumber()
	{
		SemanticVersion v2 = SemanticVersion._withNoBuildNumber("1.2.3");
		assertEquals(1, v2.major());
		assertEquals(2, v2.minor());
		assertEquals(3, v2.patch());
		assertEquals(0, v2.buildNumber());
		assertNull(v2.feature());
		
		SemanticVersion v3 = SemanticVersion._withNoBuildNumber("10.8.13-feature29");
		assertEquals(10, v3.major());
		assertEquals(8, v3.minor());
		assertEquals(13, v3.patch());
		assertEquals(0, v3.buildNumber());		
		assertEquals("feature29", v3.feature());
	}
	
	@Test
	public void testSemanticVersionNumber()
	{
		
		SemanticVersion v1 = new SemanticVersion("1.2.3-9");
		
		SemanticVersion v2 = SemanticVersion._of("1.2.3-9");
		assertEquals(1, v2.major());
		assertEquals(2, v2.minor());
		assertEquals(3, v2.patch());
		assertEquals(9, v2.buildNumber());
		assertNull(v2.feature());

		assertTrue(v1.equals(v2));
		
		SemanticVersion v3 = SemanticVersion._of("10.8.13-feature29-95");
		assertEquals(10, v3.major());
		assertEquals(8, v3.minor());
		assertEquals(13, v3.patch());
		assertEquals(95, v3.buildNumber());		
		assertEquals("feature29", v3.feature());

		SemanticVersion v4 = new SemanticVersion("10.8.13-feature29-95");
		assertTrue(v3.equals(v4));
		
		SemanticVersion v5 = new SemanticVersion("10.8.13-95");
		assertFalse(v5.equals(v4));
		
		v3.incrementBuildNumber();
		assertTrue(v3.toString().equals("10.8.13-feature29-96"));

		v3.incrementPatch();
		assertTrue(v3.toString().equals("10.8.14-feature29-0"));

		v3.incrementBuildNumber();
		assertTrue(v3.toString().equals("10.8.14-feature29-1"));

		v3.incrementMinor();
		assertTrue(v3.toString().equals("10.9.0-feature29-0"));

		v3.incrementBuildNumber();
		assertTrue(v3.toString().equals("10.9.0-feature29-1"));

		v3.incrementMajor();
		assertTrue(v3.toString().equals("11.0.0-feature29-0"));
		
		SemanticVersion ANull = null;
		assertFalse(SemanticVersion._of("1.0.0-0").equals(ANull));
		
		assertTrue(SemanticVersion._of("1.0.0-0").compareTo(SemanticVersion._of("1.0.0-0")) == 0);
		
		assertEquals(SemanticVersion._of("1.0.0-0").hashCode(), SemanticVersion._of("1.0.0-0").hashCode());
		
		assertTrue(SemanticVersion._of("1.0.0-0").compareTo(SemanticVersion._of("1.0.0-10")) < 0);
		
		assertTrue(SemanticVersion._of("1.0.1-2").compareTo(SemanticVersion._of("1.0.0-10")) > 0);
		
		assertTrue(SemanticVersion._of("1.0.21-0").compareTo(SemanticVersion._of("1.1.0-10")) < 0);
		
		assertTrue(SemanticVersion._of("1.0.2-2").compareTo(SemanticVersion._of("1.0.1-10")) > 0);
		
		assertTrue(SemanticVersion._of("4.0.21-0").compareTo(SemanticVersion._of("6.1.9-10")) < 0);
		
		assertTrue(SemanticVersion._of("8.5.2-2").compareTo(SemanticVersion._of("7.99.1-10")) > 0);
		

		//Features
		assertTrue(SemanticVersion._of("1.0.0-alpha1-0").compareTo(SemanticVersion._of("1.0.0-alpha3-10")) < 0);
		
		assertTrue(SemanticVersion._of("1.0.0-alpha-2").compareTo(SemanticVersion._of("1.0.0-beta-1")) < 0);
		
		//Because it is a feature.
		assertTrue(SemanticVersion._of("1.0.0-alpha-2").compareTo(SemanticVersion._of("1.0.0-1")) < 0);
	}

}
