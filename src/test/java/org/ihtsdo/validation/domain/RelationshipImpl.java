package org.ihtsdo.validation.domain;

public class RelationshipImpl implements Relationship {

	private String sourceId;
	private String typeId;
	private String destinationId;

	public RelationshipImpl(String typeId) {
		this.typeId = typeId;
	}

	@Override
	public String getSourceId() {
		return sourceId;
	}

	@Override
	public String getTypeId() {
		return typeId;
	}

	@Override
	public String getDestinationId() {
		return destinationId;
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

	@Override
	public String toString() {
		return "Relationship{" +
				"sourceId='" + sourceId + '\'' +
				", typeId='" + typeId + '\'' +
				", destinationId='" + destinationId + '\'' +
				'}';
	}
}
