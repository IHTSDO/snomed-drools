package org.ihtsdo.drools.rulestestrig.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.helper.DescriptionHelper;
import org.ihtsdo.drools.service.RelationshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRelationshipService implements RelationshipService {

	private final Map<String, Concept> concepts;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public TestRelationshipService(Map<String, Concept> concepts) {
		this.concepts = concepts;
	}

	@Override
	public boolean hasActiveInboundStatedRelationship(String conceptId) {
		return hasActiveInboundStatedRelationship(conceptId, null);
	}

	@Override
	public boolean hasActiveInboundStatedRelationship(String conceptId, String relationshipTypeId) {
		for (Concept concept : concepts.values()) {
			if (!conceptId.equals(concept.getId())) {
				for (Relationship relationship : concept.getRelationships()) {
					if (relationship.isActive()
							&& conceptId.equals(relationship.getDestinationId())
							&& !Constants.INFERRED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())
							&& (relationshipTypeId == null || relationshipTypeId.equals(relationship.getTypeId()))) {
						logger.info("Active inbound relationship sourceId {}", relationship.getSourceId());
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public Set<String> findParentsNotContainSematicTag(Concept c, String sematicTag) {
		Set<String> parentIds = new HashSet<>();
		for (Relationship r: c.getRelationships()) {
			if(r.isActive() && Constants.IS_A.equals(r.getTypeId()) && !Constants.INFERRED_RELATIONSHIP.equals(r.getCharacteristicTypeId())) {
				Concept parentConcept = concepts.get(r.getDestinationId());
				for (Description d : parentConcept.getDescriptions()) {
					if (d.isActive() && Constants.FSN.equals(d.getTypeId())) {
						String parentSematicTag = DescriptionHelper.getTag(d.getTerm());
						if (!sematicTag.equals(parentSematicTag)) {
							parentIds.add(parentConcept.getId());
						}
					}
				}				
			}
		}
		
		return parentIds;
	}

}
