package org.ihtsdo.drools.domain;
import org.snomed.otf.owltoolkit.domain.Relationship.ConcreteValue;

/**
 * Represents a stated relationship, OWL Axiom fragment or inferred relationship
 */
public interface Relationship extends Component {

	/**
	 * UUID of axiom reference set member or null if relationship is from stated or inferred relationships.
	 * @return Axiom id or null
	 */
	String getAxiomId();

	boolean isAxiomGCI();

	String getSourceId();

	int getRelationshipGroup();

	String getTypeId();

	String getDestinationId();

	String getCharacteristicTypeId();

	ConcreteValue getConcreteValue();
}
