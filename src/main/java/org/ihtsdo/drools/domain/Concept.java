package org.ihtsdo.drools.domain;

import java.util.Collection;

public interface Concept extends Component {

	String getDefinitionStatusId();

	Collection<Description> getDescriptions();

	Collection<Relationship> getRelationships();
}
