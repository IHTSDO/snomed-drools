package org.ihtsdo.drools.rulestestrig.domain;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.OntologyAxiom;
import org.ihtsdo.drools.domain.Relationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class TestConcept<D extends Description, R extends Relationship> implements Concept, TestComponent {

	private String id;
	private boolean active;
	private boolean published;
	private boolean released;
	private String moduleId;
	private String definitionStatusId;
	private Collection<D> descriptions;
	private Collection<R> relationships;
	private Collection<OntologyAxiom> ontologyAxioms;

	public TestConcept() {
		active = true;
		descriptions = new ArrayList<>();
		relationships = new ArrayList<>();
		ontologyAxioms = new ArrayList<>();
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
	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
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
	public String getDefinitionStatusId() {
		return definitionStatusId;
	}

	@Override
	public Collection<D> getDescriptions() {
		return descriptions;
	}

	@Override
	public Collection<R> getRelationships() {
		return relationships;
	}

	@Override
	public Collection<OntologyAxiom> getOntologyAxioms() {
		return ontologyAxioms;
	}

	@Override
	public Map <String, Set<String>> getAssociationTargets() {
		return null;
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

	public void setDefinitionStatusId(String definitionStatusId) {
		this.definitionStatusId = definitionStatusId;
	}

	public void setDescriptions(Collection<D> descriptions) {
		this.descriptions = descriptions;
	}

	public void setRelationships(Collection<R> relationships) {
		this.relationships = relationships;
	}

	public void setOntologyAxioms(Collection<OntologyAxiom> ontologyAxioms) {
		this.ontologyAxioms = ontologyAxioms;
	}

	@Override
	public String toString() {
		return "TestConcept{" +
				"id='" + id + '\'' +
				", active=" + active +
				", published=" + published +
				", definitionStatusId=" + definitionStatusId +
				", descriptions=" + descriptions +
				", relationships=" + relationships +
				", ontologyAxioms=" + ontologyAxioms +
				'}';
	}
}
