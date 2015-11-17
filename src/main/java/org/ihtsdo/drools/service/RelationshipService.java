package org.ihtsdo.drools.service;

public interface RelationshipService {

	boolean hasActiveInboundRelationship(String conceptId, String relationshipTypeId);

}
