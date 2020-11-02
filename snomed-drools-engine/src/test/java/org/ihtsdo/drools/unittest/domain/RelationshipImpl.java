package org.ihtsdo.drools.unittest.domain;

import org.ihtsdo.drools.domain.Relationship;
import org.snomed.otf.owltoolkit.domain.Relationship.ConcreteValue;

public class RelationshipImpl implements Relationship {

	private final String id;
	private String moduleId;
	private String sourceId;
	private String destinationId;
	private int relationshipGroup;
	private String typeId;
	private String characteristicTypeId;
	private boolean released;
	private ConcreteValue concreteValue;

	public RelationshipImpl(String id, String typeId) {
		this.id = id;
		this.typeId = typeId;
	}

	@Override
	public String getAxiomId() {
		// We don't have any unit tests using axioms in this module
		return null;
	}

	@Override
	public boolean isAxiomGCI() {
		return false;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	@Override
	public boolean isPublished() {
		return false;
	}

	@Override
	public String getSourceId() {
		return sourceId;
	}

	@Override
	public String getDestinationId() {
		return destinationId;
	}

	@Override
	public int getRelationshipGroup() {
		return relationshipGroup;
	}

	@Override
	public String getTypeId() {
		return typeId;
	}

	@Override
	public String getCharacteristicTypeId() {
		return characteristicTypeId;
	}

	@Override
	public boolean isReleased() {
		return released;
	}

	@Override
	public ConcreteValue getConcreteValue() {
		return concreteValue;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	public void setDestinationId(String destinationId) {
		this.destinationId = destinationId;
	}

	public void setRelationshipGroup(int relationshipGroup) {
		this.relationshipGroup = relationshipGroup;
	}

	public void setCharacteristicTypeId(String characteristicTypeId) {
		this.characteristicTypeId = characteristicTypeId;
	}

	public void setReleased(boolean released) {
		this.released = released;
	}

	public void setConcreteValue(ConcreteValue value) {
		this.concreteValue = value;
	}

	@Override
	public String toString() {
		return "RelationshipImpl{" +
				"id='" + id + '\'' +
				", sourceId='" + sourceId + '\'' +
				", destinationId='" + destinationId + '\'' +
				", relationshipGroup='" + relationshipGroup + '\'' +
				", typeId='" + typeId + '\'' +
				", characteristicTypeId='" + characteristicTypeId + '\'' +
				'}';
	}
}
