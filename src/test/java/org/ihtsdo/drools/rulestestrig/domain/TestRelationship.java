package org.ihtsdo.drools.rulestestrig.domain;

import org.ihtsdo.drools.domain.Relationship;

public class TestRelationship implements Relationship {

	private String id;
	private boolean active;
	private boolean published;
	private String sourceId;
	private int relationshipGroup;
	private String typeId;
	private String destinationId;

	public TestRelationship() {
		active = true;
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

	public void setId(String id) {
		this.id = id;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setPublished(boolean published) {
		this.published = published;
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

	@Override
	public String toString() {
		return "Relationship{" +
				"id='" + id + '\'' +
				", sourceId='" + sourceId + '\'' +
				", destinationId='" + destinationId + '\'' +
				", relationshipGroup='" + relationshipGroup + '\'' +
				", typeId='" + typeId + '\'' +
				'}';
	}
}