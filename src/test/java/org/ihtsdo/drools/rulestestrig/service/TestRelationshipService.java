package org.ihtsdo.drools.rulestestrig.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.service.RelationshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TestRelationshipService implements RelationshipService {

	private final Map<String, Concept> concepts;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public TestRelationshipService(Map<String, Concept> concepts) {
		this.concepts = concepts;
	}

	@Override
	public boolean hasActiveInboundRelationship(String conceptId, String relationshipTypeId) {
		for (Concept concept : concepts.values()) {
			if (!concept.equals(concept.getId())) {
				for (Relationship relationship : concept.getRelationships()) {
					if (relationship.isActive()
							&& relationshipTypeId.equals(relationship.getTypeId())
							&& conceptId.equals(relationship.getDestinationId())) {
						logger.info("Active inbound relationship sourceId {}", relationship.getSourceId());
						return true;
					}
				}
			}
		}
		return false;
	}

}
