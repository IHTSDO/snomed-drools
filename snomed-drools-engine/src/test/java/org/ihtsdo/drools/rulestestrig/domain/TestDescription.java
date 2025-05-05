package org.ihtsdo.drools.rulestestrig.domain;

import org.ihtsdo.drools.domain.Description;

import java.util.HashMap;
import java.util.Map;

public class TestDescription implements Description, TestComponent {

	private String id;
	private String effectiveTime;
	private boolean active;
	private boolean published;
	private boolean released;
	private String moduleId;
	private String languageCode;
	private String conceptId;
	private String typeId;
	private String caseSignificanceId;
	private String term;
	private Map<String, String> acceptabilityMap;
	private boolean textDefinition;

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
	public boolean isReleased() {
		return released;
	}

	@Override
	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	@Override
	public String getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	@Override
	public String getTypeId() {
		return typeId;
	}

	@Override
	public String getLanguageCode() {
		return languageCode;
	}

	@Override
	public String getConceptId() {
		return conceptId;
	}

	@Override
	public String getCaseSignificanceId() {
		return caseSignificanceId;
	}

	@Override
	public String getTerm() {
		return term;
	}

	@Override
	public boolean isTextDefinition() {
		return textDefinition;
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

	public void setReleased(boolean released) {
		this.released = released;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	public void setCaseSignificanceId(String caseSignificanceId) {
		this.caseSignificanceId = caseSignificanceId;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public void setAcceptabilityMap(Map<String, String> acceptabilityMap) {
		this.acceptabilityMap = acceptabilityMap;
	}

	public void setTextDefinition(boolean textDefinition) {
		this.textDefinition = textDefinition;
	}

	@Override
	public String toString() {
		return "TestDescription{" +
				"id='" + id + '\'' +
				", active=" + active +
				", published=" + published +
				", languageCode='" + languageCode + '\'' +
				", conceptId='" + conceptId + '\'' +
				", typeId='" + typeId + '\'' +
				", caseSignificanceId='" + caseSignificanceId + '\'' +
				", term='" + term + '\'' +
				", acceptabilityMap=" + acceptabilityMap +
				", textDefinition=" + textDefinition +
				'}';
	}
}
