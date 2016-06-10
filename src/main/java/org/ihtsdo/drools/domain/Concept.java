package org.ihtsdo.drools.domain;

import java.util.Collection;

public interface Concept extends Component {

	String getDefinitionStatusId();

	Collection<? extends Description> getDescriptions();

	Collection<? extends Relationship> getRelationships();
}
