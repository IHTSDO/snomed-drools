package org.ihtsdo.drools.validator.rf2.domain;

import org.ihtsdo.drools.domain.Description;

import java.util.HashMap;
import java.util.Map;

public class DroolsDescription extends DroolsComponent implements Description {

	private String conceptId;
	private String languageCode;
	private String typeId;
	private String term;
	private String caseSignificanceId;
	private boolean textDefinition;
	private Map<String, String> acceptabilityMap;

	public DroolsDescription(String id, boolean active, String moduleId, String conceptId, String languageCode, String typeId, String term, String caseSignificanceId, boolean textDefinition, boolean published, boolean released) {
		super(id, active, moduleId, published, released);
		this.conceptId = conceptId;
		this.languageCode = languageCode;
		this.typeId = typeId;
		this.term = term;
		this.caseSignificanceId = caseSignificanceId;
		this.textDefinition = textDefinition;
		this.acceptabilityMap = new HashMap<>();
	}

	@Override
	public String getConceptId() {
		return conceptId;
	}

	@Override
	public String getLanguageCode() {
		return languageCode;
	}

	@Override
	public String getTypeId() {
		return typeId;
	}

	@Override
	public String getTerm() {
		return term;
	}

	@Override
	public String getCaseSignificanceId() {
		return caseSignificanceId;
	}

	@Override
	public boolean isTextDefinition() {
		return textDefinition;
	}

	@Override
	public Map<String, String> getAcceptabilityMap() {
		return acceptabilityMap;
	}
}
