package org.ihtsdo.drools.rulestestrig.domain;

import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Relationship;

public class TestRelationship implements Relationship, TestComponent {

	private String id;
	private String effectiveTime;
	private String axiomId;
	private boolean axiomGCI;
	private boolean active;
	private boolean published;
	private boolean released;
	private String moduleId;
	private String sourceId;
	private int relationshipGroup;
	private String typeId;
	private String destinationId;
	private String characteristicTypeId;
	private String concreteValue;

	public TestRelationship() {
		active = true;
		characteristicTypeId = Constants.STATED_RELATIONSHIP;
	}

	@Override
	public String getAxiomId() {
		return axiomId;
	}

	@Override
	public boolean isAxiomGCI() {
		return axiomGCI;
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
	public boolean isReleased() {
		return released;
	}

	@Override
	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	@Override
	public String getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	@Override
	public String getSourceId() {
		return sourceId;
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
	public String getDestinationId() {
		return destinationId;
	}

	@Override
	public String getCharacteristicTypeId() {
		return characteristicTypeId;
	}

	@Override
	public String getConcreteValue() {
		return concreteValue;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setAxiomId(String axiomId) {
		this.axiomId = axiomId;
	}

	public void setAxiomGCI(boolean axiomGCI) {
		this.axiomGCI = axiomGCI;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public void setReleased(boolean released) {
		this.released = released;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public void setDestinationId(String destinationId) {
		this.destinationId = destinationId;
	}

	public void setRelationshipGroup(int relationshipGroup) {
		this.relationshipGroup = relationshipGroup;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	public void setCharacteristicTypeId(String characteristicTypeId) {
		this.characteristicTypeId = characteristicTypeId;
	}

	public void setConcreteValue(String value) {
		this.concreteValue = value;
	}

	@Override
	public String toString() {
		return "Relationship{" +
				"id='" + id + '\'' +
				", sourceId='" + sourceId + '\'' +
				", destinationId='" + destinationId + '\'' +
				", relationshipGroup='" + relationshipGroup + '\'' +
				", typeId='" + typeId + '\'' +
				", characteristicTypeId='" + characteristicTypeId + '\'' +
				'}';
	}
}
