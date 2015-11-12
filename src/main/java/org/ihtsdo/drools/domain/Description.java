package org.ihtsdo.drools.domain;

import java.util.Map;

public interface Description extends Component {

	String getTypeId();

	String getConceptId();

	String getTerm();

	Map<String, String> getAcceptabilityMap();
}
