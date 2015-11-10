package org.ihtsdo.drools.domain;

public class DescriptionImpl implements Description {

	private final String id;
	private String conceptId;
	private String term;

	public DescriptionImpl(String id, String term) {
		this.id = id;
		this.term = term;
	}

	@Override
	public String getId() {
		return id;
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
