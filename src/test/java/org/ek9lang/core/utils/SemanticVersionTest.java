package org.ek9lang.core.utils;

import org.junit.Test;

import junit.framework.TestCase;

public class SemanticVersionTest
{

	@Test
	public void testInvalidSemanticVersionNumber()
	{		
		TestCase.assertNull(SemanticVersion._withNoBuildNumber("1.2.A"));
		
		TestCase.assertNull(SemanticVersion._of("1.2.A"));
	}
	
	@Test
	public void testSemenaticVersionWithoutBuildNumber()
	{
		SemanticVersion v2 = SemanticVersion._withNoBuildNumber("1.2.3");
		TestCase.assertEquals(1, v2.major());
		TestCase.assertEquals(2, v2.minor());
		TestCase.assertEquals(3, v2.patch());
		TestCase.assertEquals(0, v2.buildNumber());
		TestCase.assertNull(v2.feature());
		
		SemanticVersion v3 = SemanticVersion._withNoBuildNumber("10.8.13-feature29");
		TestCase.assertEquals(10, v3.major());
		TestCase.assertEquals(8, v3.minor());
		TestCase.assertEquals(13, v3.patch());
		TestCase.assertEquals(0, v3.buildNumber());		
		TestCase.assertEquals("feature29", v3.feature());
	}
	
	@Test
	public void testSemanticVersionNumber()
	{
		
		SemanticVersion v2 = SemanticVersion._of("1.2.3-9");
		TestCase.assertEquals(1, v2.major());
		TestCase.assertEquals(2, v2.minor());
		TestCase.assertEquals(3, v2.patch());
		TestCase.assertEquals(9, v2.buildNumber());
		TestCase.assertNull(v2.feature());

		SemanticVersion v3 = SemanticVersion._of("10.8.13-feature29-95");
		TestCase.assertEquals(10, v3.major());
		TestCase.assertEquals(8, v3.minor());
		TestCase.assertEquals(13, v3.patch());
		TestCase.assertEquals(95, v3.buildNumber());		
		TestCase.assertEquals("feature29", v3.feature());

		v3.incrementBuildNumber();
		TestCase.assertTrue(v3.toString().equals("10.8.13-feature29-96"));

		v3.incrementPatch();
		TestCase.assertTrue(v3.toString().equals("10.8.14-feature29-0"));

		v3.incrementBuildNumber();
		TestCase.assertTrue(v3.toString().equals("10.8.14-feature29-1"));

		v3.incrementMinor();
		TestCase.assertTrue(v3.toString().equals("10.9.0-feature29-0"));

		v3.incrementBuildNumber();
		TestCase.assertTrue(v3.toString().equals("10.9.0-feature29-1"));

		v3.incrementMajor();
		TestCase.assertTrue(v3.toString().equals("11.0.0-feature29-0"));
		
		SemanticVersion ANull = null;
		TestCase.assertFalse(SemanticVersion._of("1.0.0-0").equals(ANull));
		
		TestCase.assertTrue(SemanticVersion._of("1.0.0-0").compareTo(SemanticVersion._of("1.0.0-0")) == 0);
		
		TestCase.assertEquals(SemanticVersion._of("1.0.0-0").hashCode(), SemanticVersion._of("1.0.0-0").hashCode());
		
		TestCase.assertTrue(SemanticVersion._of("1.0.0-0").compareTo(SemanticVersion._of("1.0.0-10")) < 0);
		
		TestCase.assertTrue(SemanticVersion._of("1.0.1-2").compareTo(SemanticVersion._of("1.0.0-10")) > 0);
		
		TestCase.assertTrue(SemanticVersion._of("1.0.21-0").compareTo(SemanticVersion._of("1.1.0-10")) < 0);
		
		TestCase.assertTrue(SemanticVersion._of("1.0.2-2").compareTo(SemanticVersion._of("1.0.1-10")) > 0);
		
		TestCase.assertTrue(SemanticVersion._of("4.0.21-0").compareTo(SemanticVersion._of("6.1.9-10")) < 0);
		
		TestCase.assertTrue(SemanticVersion._of("8.5.2-2").compareTo(SemanticVersion._of("7.99.1-10")) > 0);
		

		//Features
		TestCase.assertTrue(SemanticVersion._of("1.0.0-alpha1-0").compareTo(SemanticVersion._of("1.0.0-alpha3-10")) < 0);
		
		TestCase.assertTrue(SemanticVersion._of("1.0.0-alpha-2").compareTo(SemanticVersion._of("1.0.0-beta-1")) < 0);
		
		//Because it is a feature.
		TestCase.assertTrue(SemanticVersion._of("1.0.0-alpha-2").compareTo(SemanticVersion._of("1.0.0-1")) < 0);
	}

}
