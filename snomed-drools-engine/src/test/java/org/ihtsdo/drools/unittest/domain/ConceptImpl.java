package org.ihtsdo.drools.unittest.domain;

import org.ihtsdo.drools.domain.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConceptImpl implements Concept {

	private String id;
	private String moduleId;
	private String definitionStatusId;
	private Collection<Description> descriptions;
	private Collection<Annotation> annotations;
	private Collection<Relationship> relationships;
	private Collection<OntologyAxiom> ontologyAxioms;
	private boolean released;

	public ConceptImpl(String id) {
		this.id = id;
		descriptions = new HashSet<>();
		this.annotations = new HashSet<>();
		relationships = new HashSet<>();
		ontologyAxioms = new HashSet<>();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public String getModuleId() {
		return moduleId;
	}

	@Override
	public String getEffectiveTime() {
		return null;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	@Override
	public boolean isPublished() {
		return false;
	}

	@Override
	public boolean isReleased() {
		return released;
	}

	public void setReleased(boolean released) {
		this.released = released;
	}

	public ConceptImpl addDescription(DescriptionImpl description) {
		description.setConceptId(id);
		descriptions.add(description);
		return this;
	}

	public ConceptImpl addRelationship(RelationshipImpl relationship) {
		relationship.setSourceId(id);
		relationships.add(relationship);
		return this;
	}

	@Override
	public String getDefinitionStatusId() {
		return definitionStatusId;
	}

	public void setDefinitionStatusId(String definitionStatusId) {
		this.definitionStatusId = definitionStatusId;
	}

	@Override
	public Collection<Description> getDescriptions() {
		return descriptions;
	}


	public void setDescriptions(Collection<Description> descriptions) {
		this.descriptions = descriptions;
	}

	@Override
	public Collection<? extends Annotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(Collection<Annotation> annotations) {
		this.annotations = annotations;
	}

	@Override
	public Collection<Relationship> getRelationships() {
		return relationships;
	}

	@Override
	public Collection<? extends OntologyAxiom> getOntologyAxioms() {
		return ontologyAxioms;
	}

	@Override
	public Map <String, Set<String>> getAssociationTargets() {
		return null;
	}

	@Override
	public String toString() {
		return "Concept{" +
				"id='" + id + '\'' +
				'}';
	}
}
