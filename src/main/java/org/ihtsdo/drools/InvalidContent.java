package org.ihtsdo.drools;

import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;

public class InvalidContent {

	private String conceptId;
	private String message;
	private String componentId;

	public InvalidContent(String conceptId, String message) {
		this.conceptId = conceptId;
		this.message = message;
	}

	public InvalidContent(Description description, String message) {
		this(description.getConceptId(), message);
		componentId = description.getId();
	}

	public InvalidContent(Relationship relationship, String message) {
		this(relationship.getSourceId(), message);
		componentId = relationship.getSourceId();
	}

	public String getConceptId() {
		return conceptId;
	}

	public String getMessage() {
		return message;
	}

	public String getComponentId() {
		return componentId;
	}

	@Override
	public String toString() {
		return "InvalidContent{" +
				"conceptId='" + conceptId + '\'' +
				", message='" + message + '\'' +
				", componentId='" + componentId + '\'' +
				'}';
	}
}
