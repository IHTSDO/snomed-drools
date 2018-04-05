package org.ihtsdo.drools.service;

import java.util.*;

import org.ihtsdo.drools.domain.Concept;

public interface RelationshipService {

	boolean hasActiveInboundStatedRelationship(String conceptId);

	boolean hasActiveInboundStatedRelationship(String conceptId, String relationshipTypeId);

	Set<String> findParentsNotContainSematicTag(Concept c, String sematicTag);
}
