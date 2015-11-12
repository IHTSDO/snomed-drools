package org.ihtsdo.drools.rulestestrig;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;

import java.util.ArrayList;
import java.util.Collection;

public class TestConcept<D extends Description, R extends Relationship> implements Concept {

	private String id;
	private boolean active;
	private boolean published;
	private Collection<D> descriptions;
	private Collection<R> relationships;

	public TestConcept() {
		active = true;
		descriptions = new ArrayList<>();
		relationships = new ArrayList<>();
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
	public Collection<Description> getDescriptions() {
		return (Collection<Description>) descriptions;
	}

	@Override
	public Collection<Relationship> getRelationships() {
		return (Collection<Relationship>) relationships;
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

	public void setDescriptions(Collection<D> descriptions) {
		this.descriptions = descriptions;
	}

	public void setRelationships(Collection<R> relationships) {
		this.relationships = relationships;
	}

	@Override
	public String toString() {
		return "Concept{" +
				"id='" + id + '\'' +
				", descriptions=" + descriptions +
				", relationships=" + relationships +
				'}';
	}
}
