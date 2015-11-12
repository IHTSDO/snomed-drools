package org.ihtsdo.drools.domain;

public interface Relationship extends Component {

	String getSourceId();

	String getTypeId();

	String getDestinationId();
}
