package org.ihtsdo.validation.domain;

import org.ihtsdo.validation.domain.Description;

public class DescriptionImpl implements Description {

	private String conceptId;
	private String term;

	public DescriptionImpl(String term) {
		this.term = term;
	}

	@Override
	public String getConceptId() {
		return conceptId;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	@Override
	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	@Override
	public String toString() {
		return "Description{" +
				"term='" + term + '\'' +
				'}';
	}
}
