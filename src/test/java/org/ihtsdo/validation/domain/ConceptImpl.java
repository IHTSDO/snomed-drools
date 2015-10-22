package org.ihtsdo.validation.domain;

import java.util.Collection;
import java.util.HashSet;

public class ConceptImpl implements Concept {

	private String id;
	private Collection<Description> descriptions;
	private Collection<Relationship> relationships;

	public ConceptImpl(String id) {
		this.id = id;
		descriptions = new HashSet<>();
		relationships = new HashSet<>();
	}

	@Override
	public String getId() {
		return id;
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
	public Collection<Description> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(Collection<Description> descriptions) {
		this.descriptions = descriptions;
	}

	@Override
	public Collection<Relationship> getRelationships() {
		return relationships;
	}

	@Override
	public String toString() {
		return "Concept{" +
				"id='" + id + '\'' +
				'}';
	}
}
