package org.ihtsdo.drools.domain;

/**
 * Represents a stated relationship, OWL Axiom fragment or inferred relationship
 */
public interface Relationship extends Component {

	String getSourceId();

	String getDestinationId();

	int getRelationshipGroup();

	String getTypeId();

	String getCharacteristicTypeId();
}
