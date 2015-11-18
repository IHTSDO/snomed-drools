package org.ihtsdo.drools.helper;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DescriptionHelper {

	public static final Pattern TAG_PATTERN = Pattern.compile("^.*\\((.*)\\)$");

	public static Collection<Description> filterByActiveTypeAndDialectPreferred(Concept concept, boolean active, String typeId, String dialectPreferred) {
		Collection<Description> descriptions = new HashSet<>();
		for (Description description : concept.getDescriptions()) {
			if (description.isActive() == active && typeId.equals(description.getTypeId())
					&& Constants.ACCEPTABILITY_PREFERRED.equals(description.getAcceptabilityMap().get(dialectPreferred))) {
				descriptions.add(description);
			}
		}
		return descriptions;
	}

	public static boolean isUniqueWithinConcept(Description description, Concept concept) {
		final String term = description.getTerm();
		for (Description otherDescription : concept.getDescriptions()) {
			if (!description.getId().equals(otherDescription.getId()) && term.equals(otherDescription.getTerm())) {
				return false;
			}
		}
		return true;
	}

	public static boolean isPreferredInAnyDialect(Description description) {
		final Map<String, String> acceptabilityMap = description.getAcceptabilityMap();
		for (String lang : acceptabilityMap.keySet()) {
			if (lang != null && Constants.ACCEPTABILITY_PREFERRED.equals(acceptabilityMap.get(lang))) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSemanticTagEquivalentToAnother(String testTerm, Set<String> otherTerms) {
		String tag = getTag(testTerm);
		if (tag != null) {
			for (String otherTerm : otherTerms) {
				if (tag.equals(getTag(otherTerm))) {
					return true;
				}
			}
		}
		return false;
	}

	private static String getTag(String term) {
		final Matcher matcher = TAG_PATTERN.matcher(term);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
	}

}
