package org.ihtsdo.drools.validator.rf2.domain;

import org.ihtsdo.drools.domain.Concept;

import java.util.*;

public class DroolsConcept extends DroolsComponent implements Concept {

	private final String definitionStatusId;
	private final Set<DroolsDescription> descriptions;
	private final Set<DroolsRelationship> relationships;
	private final Set<DroolsRelationship> activeInboundStatedRelationships;
	private final Set<DroolsOntologyAxiom> ontologyAxioms;
	private Map<String, Set<String>> associationTargets;
    private Set<Long> statedAncestorIds;

	public DroolsConcept(String id, String effectiveTime, boolean active, String moduleId, String definitionStatusId, boolean published, boolean released) {
		super(id, effectiveTime, active, moduleId, published, released);
		this.definitionStatusId = definitionStatusId;
		descriptions = new HashSet<>();
		relationships = new HashSet<>();
		activeInboundStatedRelationships = new HashSet<>();
		ontologyAxioms = new HashSet<>();
		associationTargets = new HashMap<>();
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

	@Override
	public Collection<DroolsOntologyAxiom> getOntologyAxioms() {
		return ontologyAxioms;
	}

	public Set<DroolsRelationship> getActiveInboundStatedRelationships() {
		return activeInboundStatedRelationships;
	}

	@Override
	public Map <String, Set<String>> getAssociationTargets() {
		return associationTargets;
	}

	public DroolsConcept setStatedAncestorIds(Set<Long> statedAncestorIds) {
		this.statedAncestorIds = statedAncestorIds;
		return this;
	}

	public Set<Long> getStatedAncestorIds() {
		return statedAncestorIds;
	}
}
