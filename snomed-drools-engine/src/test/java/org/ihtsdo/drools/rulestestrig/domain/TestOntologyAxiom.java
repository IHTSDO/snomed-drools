package org.ihtsdo.drools.rulestestrig.domain;

import org.ihtsdo.drools.domain.OntologyAxiom;

import java.util.*;

public class TestOntologyAxiom implements OntologyAxiom, TestComponent {

	private String id;
	private boolean active;
	private boolean published;
	private boolean released;
	private String moduleId;
	private String referencedComponentId;
	private String owlExpression;
	private Collection<String> owlExpressionNamedConcepts;

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Override
	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	@Override
	public boolean isReleased() {
		return released;
	}

	public void setReleased(boolean released) {
		this.released = released;
	}

	@Override
	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	@Override
	public String getReferencedComponentId() {
		return referencedComponentId;
	}

	public void setReferencedComponentId(String referencedComponentId) {
		this.referencedComponentId = referencedComponentId;
	}

	@Override
	public String getOwlExpression() {
		return owlExpression;
	}

	public void setOwlExpression(String owlExpression) {
		this.owlExpression = owlExpression;
	}

	@Override
	public Collection<String> getOwlExpressionNamedConcepts() {
		return owlExpressionNamedConcepts;
	}

	public void setOwlExpressionNamedConcepts(Collection<String> owlExpressionNamedConcepts) {
		this.owlExpressionNamedConcepts = owlExpressionNamedConcepts;
	}

	@Override
	public String toString() {
		return "TestOntologyAxiom{" +
				"id='" + id + '\'' +
				", active=" + active +
				", published=" + published +
				", released=" + released +
				", moduleId='" + moduleId + '\'' +
				", referencedComponentId='" + referencedComponentId + '\'' +
				", owlExpression='" + owlExpression + '\'' +
				", owlExpressionNamedConcepts=" + owlExpressionNamedConcepts +
				'}';
	}
}
