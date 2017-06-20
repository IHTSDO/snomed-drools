package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.service.ConceptService;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;

public class DroolsConceptService implements ConceptService {

	private final SnomedDroolsComponentRepository repository;

	public DroolsConceptService(SnomedDroolsComponentRepository repository) {
		this.repository = repository;
	}

	@Override
	public boolean isActive(String conceptId) {
		return repository.getConcept(conceptId).isActive();
	}
}
