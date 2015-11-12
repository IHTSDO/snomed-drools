package org.ihtsdo.drools.domain;

import java.util.Map;

public interface Description extends Component {

	String getConceptId();

	String getTypeId();

	String getTerm();

	String getCaseSignificanceId();

	boolean isTextDefinition();

	Map<String, String> getAcceptabilityMap();
}
