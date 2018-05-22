package org.ihtsdo.drools.domain;

import java.util.Collection;
import java.util.Set;

public interface Concept extends Component {

	String getDefinitionStatusId();

	Collection<? extends Description> getDescriptions();

	Collection<? extends Relationship> getRelationships();

	/**
	 * Most axioms can be transformed into relationships for validation.
	 * These represent general ontology axioms like property chains which can not be transformed into relationships.
	 * @return A set of ontology axioms which have this concept as the referencedComponentId.
	 */
	Collection<? extends OntologyAxiom> getOntologyAxioms();
}
