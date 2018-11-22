package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.service.ConceptService;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsRelationship;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DroolsConceptService implements ConceptService {

	private final SnomedDroolsComponentRepository repository;

	public DroolsConceptService(SnomedDroolsComponentRepository repository) {
		this.repository = repository;
	}

	@Override
	public boolean isActive(String conceptId) {
		DroolsConcept concept = repository.getConcept(conceptId);
		return concept != null && concept.isActive();
	}

	@Override
	public Set<String> getAllTopLevelHierarchies() {
		DroolsConcept rootConcept = repository.getConcept(Constants.ROOT_CONCEPT);
		Set<String> resultSet = new HashSet<>();
		for (DroolsRelationship relationship : rootConcept.getActiveInboundStatedRelationships()) {
			if(Constants.IS_A.equals(relationship.getTypeId())) {
				resultSet.add(relationship.getSourceId());
			}
		}
		return  resultSet;
	}

	@Override
	public Set<String> findStatedAncestorsOfConcept(Concept concept) {
		if(concept == null || concept.getId().equals(Constants.ROOT_CONCEPT)) {
			return Collections.emptySet();
		}
		DroolsConcept droolsConcept = repository.getConcept(concept.getId());
		Set<String> statedParents = new HashSet<>();
		for (DroolsRelationship relationship : droolsConcept.getRelationships()) {
			if(relationship.isActive() && Constants.IS_A.equals(relationship.getTypeId())
					&& Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				statedParents.add(relationship.getDestinationId());
			}
		}
		Set<String> resultSet = new HashSet<>(statedParents);
		for (String statedParent : statedParents) {
			resultSet.addAll(findStatedAncestorsOfConcept(repository.getConcept(statedParent)));
		}
		return resultSet;
	}

	@Override
	public Set<String> findTopLevelHierarchiesOfConcept(Concept concept) {
		DroolsConcept droolsConcept = repository.getConcept(concept.getId());
		Set<String> resultSet = new HashSet<>();
		for (DroolsRelationship relationship : droolsConcept.getRelationships()) {
			if(relationship.isActive() && Constants.IS_A.equals(relationship.getTypeId())
					&& Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				if(Constants.ROOT_CONCEPT.equals(relationship.getDestinationId())) {
					resultSet.add(relationship.getSourceId());
				} else {
					resultSet.addAll(findTopLevelHierarchiesOfConcept(repository.getConcept(relationship.getDestinationId())));
				}
			}
		}
		return resultSet;
	}


	@Override
	public Set<String> findStatedAncestorsOfConcepts(List<String> conceptIds) {
		Set<String> resultSet = new HashSet<>();
		for (String conceptId : conceptIds) {
			resultSet.addAll(findStatedAncestorsOfConcept(repository.getConcept(conceptId)));
		}
		return resultSet;
	}
}
