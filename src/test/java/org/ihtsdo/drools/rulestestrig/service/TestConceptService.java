package org.ihtsdo.drools.rulestestrig.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.service.ConceptService;

import java.util.Map;

public class TestConceptService implements ConceptService {

	private final Map<String, Concept> concepts;

	public TestConceptService(Map<String, Concept> concepts) {
		this.concepts = concepts;
	}

	@Override
	public boolean isActive(String conceptId) {
		return concepts.get(conceptId).isActive();
	}
}
