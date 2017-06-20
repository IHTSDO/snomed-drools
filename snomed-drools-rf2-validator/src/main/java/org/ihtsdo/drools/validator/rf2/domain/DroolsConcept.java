package org.ihtsdo.drools.validator.rf2.domain;

import org.ihtsdo.drools.domain.Concept;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DroolsConcept extends DroolsComponent implements Concept {

	private String definitionStatusId;
	private Set<DroolsDescription> descriptions;
	private Set<DroolsRelationship> relationships;
	private Set<DroolsRelationship> activeInboundStatedRelationships;

	public DroolsConcept(String id, boolean active, String moduleId, String definitionStatusId, boolean published, boolean released) {
		super(id, active, moduleId, published, released);
		this.definitionStatusId = definitionStatusId;
		descriptions = new HashSet<>();
		relationships = new HashSet<>();
		activeInboundStatedRelationships = new HashSet<>();
	}

	@Override
	public String getDefinitionStatusId() {
		return definitionStatusId;
	}

	@Override
	public Collection<DroolsDescription> getDescriptions() {
		return descriptions;
	}

	@Override
	public Collection<DroolsRelationship> getRelationships() {
		return relationships;
	}

	public Set<DroolsRelationship> getActiveInboundStatedRelationships() {
		return activeInboundStatedRelationships;
	}
}
