package org.ihtsdo.drools.response;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;

public class InvalidContent {

	private String conceptId;
	private String message;
	private String componentId;
	private Severity severity;

	public InvalidContent(String conceptId, String message, Severity severity) {
		this.conceptId = conceptId;
		this.message = message;
		this.severity = severity;
	}

	public InvalidContent(String conceptId, String message) {
		this(conceptId, message, Severity.ERROR);
	}

	public InvalidContent(Concept concept, String message, Severity severity) {
		this(concept.getId(), message, severity);
	}

	public InvalidContent(Concept concept, String message) {
		this(concept, message, Severity.ERROR);
	}

	public InvalidContent(Description description, String message, Severity severity) {
		this(description.getConceptId(), message, severity);
		componentId = description.getId();
	}

	public InvalidContent(Description description, String message) {
		this(description, message, Severity.ERROR);
	}

	public InvalidContent(Relationship relationship, String message, Severity severity) {
		this(relationship.getSourceId(), message, severity);
		componentId = relationship.getSourceId();
	}

	public InvalidContent(Relationship relationship, String message) {
		this(relationship, message, Severity.ERROR);
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

	public Severity getSeverity() {
		return severity;
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
