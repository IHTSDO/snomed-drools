package org.ihtsdo.drools.validator.rf2.domain;

import org.ihtsdo.drools.domain.OntologyAxiom;

import java.util.Set;

public class DroolsOntologyAxiom extends DroolsComponent implements OntologyAxiom {

	private final String referencedComponentId;
	private final String owlExpression;
	private final Set<String> owlExpressionNamedConcepts;

	public DroolsOntologyAxiom(String id, boolean active, String moduleId, String referencedComponentId, String owlExpression, Set<String> owlExpressionNamedConcepts, boolean published, boolean released) {
		super(id, active, moduleId, published, released);
		this.referencedComponentId = referencedComponentId;
		this.owlExpression = owlExpression;
		this.owlExpressionNamedConcepts = owlExpressionNamedConcepts;
	}

	@Override
	public String getReferencedComponentId() {
		return referencedComponentId;
	}

	@Override
	public String getOwlExpression() {
		return owlExpression;
	}

	@Override
	public Set<String> getOwlExpressionNamedConcepts() {
		return owlExpressionNamedConcepts;
	}
}
