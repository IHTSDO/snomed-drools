package org.ihtsdo.drools.unittest.domain;

import org.ihtsdo.drools.domain.Description;

import java.util.HashMap;
import java.util.Map;

public class DescriptionImpl implements Description {

	private final String id;
	private String moduleId;
	private String conceptId;
	private String term;
	private Map<String, String> acceptabilityMap;
	private boolean published;
	private boolean released;

	public DescriptionImpl(String id, String term) {
		this.id = id;
		this.term = term;
		this.acceptabilityMap = new HashMap<>();
	}

	public DescriptionImpl published() {
		published = true;
		return this;
	}

	public DescriptionImpl addToAcceptability(String key, String value) {
		acceptabilityMap.put(key, value);
		return this;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public boolean isPublished() {
		return published;
	}

	@Override
	public boolean isReleased() {
		return released;
	}

	@Override
	public String getTypeId() {
		return "";
	}

	@Override
	public String getLanguageCode() {
		return "";
	}

	@Override
	public String getConceptId() {
		return conceptId;
	}

	@Override
	public String getCaseSignificanceId() {
		return "";
	}

	@Override
	public String getTerm() {
		return term;
	}

	@Override
	public boolean isTextDefinition() {
		return false;
	}

	@Override
	public Map<String, String> getAcceptabilityMap() {
		return acceptabilityMap;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public void setReleased(boolean released) {
		this.released = released;
	}

	@Override
	public String toString() {
		return "Description{" +
				"term='" + term + '\'' +
				'}';
	}
}
