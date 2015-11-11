package org.ihtsdo.drools.testrig;

import org.ihtsdo.drools.domain.Relationship;

public class TestRelationship implements Relationship {

	private String id;
	private String sourceId;
	private String typeId;
	private String destinationId;

	public TestRelationship() {
	}

	@Override
	public String getId() {
		return id;
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

	public void setId(String id) {
		this.id = id;
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
				"id='" + id + '\'' +
				", sourceId='" + sourceId + '\'' +
				", typeId='" + typeId + '\'' +
				", destinationId='" + destinationId + '\'' +
				'}';
	}
}
