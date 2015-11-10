package org.ihtsdo.drools.domain;

import java.util.Collection;

public interface Concept {
	String getId();

	Collection<Description> getDescriptions();

	Collection<Relationship> getRelationships();
}
