package org.ihtsdo.validation;

import org.ihtsdo.validation.domain.Concept;
import org.ihtsdo.validation.domain.Description;
import org.ihtsdo.validation.domain.Relationship;

public class InvalidContent {

	private Concept concept;
	private String reason;
	private String detail;

	public InvalidContent(Concept concept, String reason) {
		this.concept = concept;
		this.reason = reason;
	}

	public InvalidContent(Concept concept, String reason, Description description) {
		this(concept, reason);
		detail = description.toString();
	}

	public InvalidContent(Concept concept, String reason, Relationship relationship) {
		this(concept, reason);
		detail = relationship.toString();
	}

	public Concept getConcept() {
		return concept;
	}

	public void setConcept(Concept concept) {
		this.concept = concept;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	@Override
	public String toString() {
		return "InvalidContent{" +
				"concept=" + concept +
				", reason='" + reason + '\'' +
				", detail='" + detail + '\'' +
				'}';
	}
}
