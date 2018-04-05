package org.ihtsdo.drools.validator.rf2.service;

import java.util.Set;

import org.ihtsdo.drools.service.RelationshipService;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;
import org.ihtsdo.drools.validator.rf2.domain.DroolsRelationship;

public class DroolsRelationshipService implements RelationshipService {

	private final SnomedDroolsComponentRepository repository;

	public DroolsRelationshipService(SnomedDroolsComponentRepository repository) {
		this.repository = repository;
	}

	@Override
	public boolean hasActiveInboundStatedRelationship(String conceptId) {
		return !repository.getConcept(conceptId).getActiveInboundStatedRelationships().isEmpty();
	}

	@Override
	public boolean hasActiveInboundStatedRelationship(String conceptId, String relationshipTypeId) {
		Set<DroolsRelationship> relationships = repository.getConcept(conceptId).getActiveInboundStatedRelationships();
		for (DroolsRelationship relationship : relationships) {
			if (relationship.getTypeId().equals(relationshipTypeId)) {
				return true;
			}
		}
		return false;
	}
}
