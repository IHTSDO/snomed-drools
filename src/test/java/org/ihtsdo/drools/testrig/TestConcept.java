package org.ihtsdo.drools.testrig;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;

import java.util.ArrayList;
import java.util.Collection;

public class TestConcept<D extends Description, R extends Relationship> implements Concept {

	private String id;
	private Collection<D> descriptions;
	private Collection<R> relationships;

	public TestConcept() {
		descriptions = new ArrayList<>();
		relationships = new ArrayList<>();
	}

	@Override
	public String getId() {
		return id;
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
