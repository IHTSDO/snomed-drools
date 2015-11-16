package org.ihtsdo.drools.unittest.domain;

import org.ihtsdo.drools.domain.Description;

import java.util.HashMap;
import java.util.Map;

public class DescriptionImpl implements Description {

	private final String id;
	private String conceptId;
	private String term;
	private Map<String, String> acceptabilityMap;

	public DescriptionImpl(String id, String term) {
		this.id = id;
		this.term = term;
		this.acceptabilityMap = new HashMap<>();
	}

	@Override
	public String getId() {
		return id;
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
	public String getTypeId() {
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

	@Override
	public String toString() {
		return "Description{" +
				"term='" + term + '\'' +
				'}';
	}
}
