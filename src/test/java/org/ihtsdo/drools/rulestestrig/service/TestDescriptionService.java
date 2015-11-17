package org.ihtsdo.drools.rulestestrig.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;

import java.util.Map;

public class TestDescriptionService {

	private final Map<String, Concept> concepts;

	public TestDescriptionService(Map<String, Concept> concepts) {
		this.concepts = concepts;
	}

	public boolean isUniqueActiveTerm(String searchTerm) {
		for (Concept testConcept : concepts.values()) {
			for (Description description : testConcept.getDescriptions()) {
				if (description.isActive() && description.getTerm().equals(searchTerm)) {
					return false;
				}
			}
		}
		return true;
	}

}
