package org.ihtsdo.drools.rulestestrig;

import org.ihtsdo.drools.domain.Description;

import java.util.HashMap;
import java.util.Map;

public class TestDescription implements Description {

	private String id;
	private boolean active;
	private boolean published;
	private String typeId;
	private String conceptId;
	private String term;
	private Map<String, String> acceptabilityMap;

	public TestDescription() {
		active = true;
		acceptabilityMap = new HashMap<>();
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
	public boolean isPublished() {
		return published;
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

	@Override
	public Map<String, String> getAcceptabilityMap() {
		return acceptabilityMap;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setPublished(boolean published) {
		this.published = published;
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

	public void setAcceptabilityMap(Map<String, String> acceptabilityMap) {
		this.acceptabilityMap = acceptabilityMap;
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
