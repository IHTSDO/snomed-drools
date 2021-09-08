package org.ihtsdo.drools.helper;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DescriptionHelperTest {

	@Test
	public void testSemanticTagCompatibleWithinHierarchy() throws Exception {
		Assert.assertFalse(DescriptionHelper.isSemanticTagCompatibleWithinHierarchy("Clinical finding (finding)", newSet("product")));
		Assert.assertTrue(DescriptionHelper.isSemanticTagCompatibleWithinHierarchy("Disease (disorder)", newSet("finding")));
	}

	private Set<String> newSet(String... string) {
		Set<String> strings = new HashSet<>();
		Collections.addAll(strings, string);
		return strings;
	}
}
