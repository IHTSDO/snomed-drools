package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.service.ConceptService;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DroolsConceptService implements ConceptService {

	private final SnomedDroolsComponentRepository repository;

	public DroolsConceptService(SnomedDroolsComponentRepository repository) {
		this.repository = repository;
	}

	@Override
	public boolean isActive(String conceptId) {
		return repository.getConcept(conceptId).isActive();
	}

	@Override
	public Set<String> getAllTopLevelHierachies() {
		// TODO: Add support for this
		return Collections.emptySet();
	}

	@Override
	public Set<String> findStatedAncestorsOfConcept(Concept concept) {
		// TODO: Add support for this
		return Collections.emptySet();
	}

	@Override
	public Set<String> findTopLevelHierachiesOfConcept(Concept concept) {
		// TODO: Add support for this
		return Collections.emptySet();
	}

	@Override
	public Set<String> findSematicTagOfAncestors(List<String> conceptIds) {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}
}
