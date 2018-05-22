package org.ihtsdo.drools.domain;


import java.util.Collection;

/**
 * A representation for axioms which can can not be transformed to relationships.
 * These include property chains and property behaviour axioms.
 */
public interface OntologyAxiom extends Component {

	String getReferencedComponentId();

	String getOwlExpression();

	Collection<String> getOwlExpressionNamedConcepts();

}
