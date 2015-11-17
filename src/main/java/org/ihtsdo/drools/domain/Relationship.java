package org.ihtsdo.drools.domain;

public interface Relationship extends Component {

	String getSourceId();

	String getDestinationId();

	int getRelationshipGroup();

	String getTypeId();
}
