package org.ihtsdo.drools.domain;

public interface Component {

	String getId();

	boolean isActive();

	boolean isPublished();

	String getModuleId();

}
