package org.ihtsdo.drools.helper;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;

import java.util.*;

public class DescriptionHelper {

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
		return false;
	}

}
