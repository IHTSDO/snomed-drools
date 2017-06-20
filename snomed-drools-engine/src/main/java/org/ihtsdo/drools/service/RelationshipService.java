package org.ihtsdo.drools.service;

public interface RelationshipService {

	boolean hasActiveInboundStatedRelationship(String conceptId);

	boolean hasActiveInboundStatedRelationship(String conceptId, String relationshipTypeId);

}
