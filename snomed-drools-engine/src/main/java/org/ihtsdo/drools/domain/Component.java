package org.ihtsdo.drools.domain;

public interface Component {

	String getId();

	boolean isActive();

	/**
	 * Has the latest state of this component been published.
	 * False if the component has been changed this cycle.
	 * @return true if not changed since the last release.
	 */
	boolean isPublished();

	/**
	 * Has any version of this component ever been included in a release.
	 * @return true is this component has ever been released.
	 */
	boolean isReleased();

	String getModuleId();

}
