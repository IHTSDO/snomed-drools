package org.ihtsdo.drools.domain;

public interface Annotation extends Component {
	String getConceptId();

	String getLanguageDialectCode();

	String getTypeId();

	String getValue();
}
