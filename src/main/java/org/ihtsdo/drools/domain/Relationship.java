package org.ihtsdo.drools.domain;

public interface Relationship {

	String getId();

	String getSourceId();

	String getTypeId();

	String getDestinationId();
}
