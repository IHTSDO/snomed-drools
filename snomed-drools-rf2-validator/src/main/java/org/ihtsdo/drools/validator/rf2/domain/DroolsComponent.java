package org.ihtsdo.drools.validator.rf2.domain;

import org.ihtsdo.drools.domain.Component;

public class DroolsComponent implements Component {

	private String id;
	private boolean active;
	private String moduleId;
	private boolean published;
	private boolean released;

	public DroolsComponent(String id, boolean active, String moduleId, boolean published, boolean released) {
		this.id = id;
		this.moduleId = moduleId;
		this.active = active;
		this.published = published;
		this.released = released;
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
}
