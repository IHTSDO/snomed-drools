package org.ihtsdo.validation;

import org.ihtsdo.validation.domain.Concept;
import org.ihtsdo.validation.domain.Relationship;

public class ECL {

	public boolean match(String eclExpression, Concept c) {
		return eclExpression.equals(c.getId());
	}

	public boolean attTypeMatch(String eclExpression, Relationship r) {
		return r.getTypeId().equals(eclExpression);
	}

	public boolean attValueNotMatch(String eclExpression, Relationship r) {
		return !r.getDestinationId().equals(eclExpression);
	}

}
