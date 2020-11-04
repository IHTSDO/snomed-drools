package org.ihtsdo.drools.validator.rf2.domain;

import org.ihtsdo.drools.domain.Relationship;

public class DroolsRelationship extends DroolsComponent implements Relationship {

	private final String axiomId;
	private final boolean isAxiomGCI;
	private final String sourceId;
	private final String destinationId;
	private final int group;
	private final String typeId;
	private final String characteristicTypeId;
	private String concreteValue;

	public DroolsRelationship(String axiomId, boolean isAxiomGCI, String id, boolean active, String moduleId, String sourceId, String destinationId, int group, String typeId, String characteristicTypeId, boolean published, boolean released, String concreteValue) {
		super(id, active, moduleId, published, released);
		this.axiomId = axiomId;
		this.isAxiomGCI = isAxiomGCI;
		this.sourceId = sourceId;
		this.destinationId = destinationId;
		this.group = group;
		this.typeId = typeId;
		this.characteristicTypeId = characteristicTypeId;
		this.concreteValue = concreteValue;
	}

	@Override
	public String getAxiomId() {
		return axiomId;
	}

	@Override
	public boolean isAxiomGCI() {
		return isAxiomGCI;
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
		return group;
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
	public String getConcreteValue() {
		return concreteValue;
	}

	@Override
	public String toString() {
		return "DroolsRelationship{" +
				"sourceId='" + sourceId + '\'' +
				", destinationId='" + destinationId + '\'' +
				", group=" + group +
				", typeId='" + typeId + '\'' +
				", characteristicTypeId='" + characteristicTypeId + '\'' +
				'}';
	}
}
