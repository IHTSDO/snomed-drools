package org.ihtsdo.drools.response;

import org.ihtsdo.drools.domain.Component;
import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;

public class InvalidContent {

	private String conceptId;
	private Component component;
	private String message;
	private Severity severity;

	private InvalidContent(String conceptId, Component component, String message, Severity severity) {
		this.conceptId = conceptId;
		this.component = component;
		this.message = message;
		this.severity = severity;
	}

	public InvalidContent(Concept concept, String message, Severity severity) {
		this(concept.getId(), concept, message, severity);
	}

	public InvalidContent(Concept concept, String message) {
		this(concept, message, Severity.ERROR);
	}

	public InvalidContent(Description description, String message, Severity severity) {
		this(description.getConceptId(), description, message, severity);
	}

	public InvalidContent(Description description, String message) {
		this(description, message, Severity.ERROR);
	}

	public InvalidContent(Relationship relationship, String message, Severity severity) {
		this(relationship.getSourceId(), relationship, message, severity);
	}

	public InvalidContent(Relationship relationship, String message) {
		this(relationship, message, Severity.ERROR);
	}

	public String getConceptId() {
		return conceptId;
	}

	public String getComponentId() {
		return component.getId();
	}

	public boolean isPublished() {
		return component.isPublished();
	}

	public String getMessage() {
		return message;
	}

	public Severity getSeverity() {
		return severity;
	}

	@Override
	public String toString() {
		return "InvalidContent{" +
				"conceptId='" + conceptId + '\'' +
				", componentId='" + getComponentId() + '\'' +
				", published='" + isPublished() + '\'' +
				", message='" + message + '\'' +
				'}';
	}
}
