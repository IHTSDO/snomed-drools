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

	public static boolean isAnyDuplicateActiveDescriptionsWithinAConcept(Concept concept) {
		Set<String> terms = new HashSet<>();
		for (Description description : concept.getDescriptions()) {
			if (description.isActive()) {
				final String term = description.getTerm();
				if (terms.contains(term)) {
					return true;
				}
				terms.add(term);
			}
		}
		return false;
	}

}
