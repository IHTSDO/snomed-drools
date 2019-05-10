package org.ihtsdo.drools.response;

import org.ihtsdo.drools.domain.*;

public class InvalidContent {

	private String conceptId;
	private Component component;
	private String message;
	private Severity severity;
	private boolean ignorePublishedCheck;

	public static InvalidContent getGeneralWarning(String message) {
		return new InvalidContent(Constants.ROOT_CONCEPT, new DummyComponent(), message, Severity.WARNING);
	}

	public InvalidContent(String conceptId, Component component, String message, Severity severity) {
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

	public InvalidContent(OntologyAxiom ontologyAxiom, String message, Severity severity) {
		this(ontologyAxiom.getReferencedComponentId(), ontologyAxiom, message, severity);
	}

	public InvalidContent(OntologyAxiom ontologyAxiom, String message) {
		this(ontologyAxiom, message, Severity.ERROR);
	}

	// This method used to return the object instance in the style of the Builder Pattern but this caused strange drools behaviour so have been removed.
	public void ignorePublishedCheck() {
		ignorePublishedCheck = true;
	}

	public String getConceptId() {
		return conceptId;
	}

	public String getComponentId() {
		return component.getId();
	}

	/**
	 * The ignorePublishedCheck field can be set to true by rules which test data
	 * where the published state cannot be evaluated. For example language reference set members.
	 * @return
	 */
	public boolean isIgnorePublishedCheck() {
		return ignorePublishedCheck;
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

	public Component getComponent() {
		return component;
	}

	public void setComponent(Component component) {
		this.component = component;
	}

	@Override
	public String toString() {
		return "InvalidContent{" +
				"conceptId='" + conceptId + '\'' +
				", componentId='" + getComponentId() + '\'' +
				", ignorePublishedCheck='" + isIgnorePublishedCheck() + '\'' +
				", published='" + isPublished() + '\'' +
				", message='" + message + '\'' +
				'}';
	}

	private static final class DummyComponent implements Component {

		@Override
		public String getId() {
			return Constants.ROOT_CONCEPT;
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public boolean isPublished() {
			return false;
		}

		@Override
		public boolean isReleased() {
			return false;
		}

		@Override
		public String getModuleId() {
			return Constants.ROOT_CONCEPT;
		}
	}
}
