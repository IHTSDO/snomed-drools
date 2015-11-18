package org.ihtsdo.drools.helper;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DescriptionHelperTest {

	@Test
	public void testIsSemanticTagEquivalentToAnother() throws Exception {
		Assert.assertFalse(DescriptionHelper.isSemanticTagEquivalentToAnother("Clinical finding (finding)", newSet("SNOMED CT Concept (SNOMED RT+CTV3)")));
		Assert.assertTrue(DescriptionHelper.isSemanticTagEquivalentToAnother("Bleeding (finding)", newSet("Clinical finding (finding)")));
	}

	private Set<String> newSet(String... string) {
		Set<String> strings = new HashSet<>();
		Collections.addAll(strings, string);
		return strings;
	}
}
