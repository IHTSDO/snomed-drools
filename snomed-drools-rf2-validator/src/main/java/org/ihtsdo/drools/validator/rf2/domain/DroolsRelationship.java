package org.ihtsdo.drools.validator.rf2.domain;

import org.ihtsdo.drools.domain.Relationship;

public class DroolsRelationship extends DroolsComponent implements Relationship {

	private final String sourceId;
	private final String destinationId;
	private final int group;
	private final String typeId;
	private final String characteristicTypeId;

	public DroolsRelationship(String id, boolean active, String moduleId, String sourceId, String destinationId, int group, String typeId, String characteristicTypeId, boolean published, boolean released) {
		super(id, active, moduleId, published, released);
		this.sourceId = sourceId;
		this.destinationId = destinationId;
		this.group = group;
		this.typeId = typeId;
		this.characteristicTypeId = characteristicTypeId;
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
