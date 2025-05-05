package org.ihtsdo.drools.validator.rf2.domain;

import org.ihtsdo.drools.domain.Component;

public class DroolsComponent implements Component {

	private final String id;
	private final String effectiveTime;
	private final boolean active;
	private final String moduleId;
	private final boolean published;
	private final boolean released;

	public DroolsComponent(String id, String effectiveTime, boolean active, String moduleId, boolean published, boolean released) {
		this.id = id;
		this.effectiveTime = effectiveTime;
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

	@Override
	public String getEffectiveTime() {
		return effectiveTime;
	}
}
