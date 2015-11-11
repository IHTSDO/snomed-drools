package org.ihtsdo.drools.testrig;

import org.ihtsdo.drools.domain.Description;

public class TestDescription implements Description {

	private String id;
	private boolean active;
	private String typeId;
	private String conceptId;
	private String term;

	public TestDescription() {
		active = true;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public String getTypeId() {
		return typeId;
	}

	@Override
	public String getConceptId() {
		return conceptId;
	}

	@Override
	public String getTerm() {
		return term;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	@Override
	public String toString() {
		return "Description{" +
				"id='" + id + '\'' +
				", active=" + active +
				", typeId='" + typeId + '\'' +
				", conceptId='" + conceptId + '\'' +
				", term='" + term + '\'' +
				'}';
	}
}
