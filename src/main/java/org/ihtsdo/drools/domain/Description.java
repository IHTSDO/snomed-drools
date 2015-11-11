package org.ihtsdo.drools.domain;

public interface Description {

	String getId();

	boolean isActive();

	String getTypeId();

	String getConceptId();

	String getTerm();
}
