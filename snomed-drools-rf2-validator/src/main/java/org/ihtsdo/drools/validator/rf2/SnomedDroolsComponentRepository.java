package org.ihtsdo.drools.validator.rf2;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.ihtsdo.drools.domain.Component;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;
import org.ihtsdo.drools.validator.rf2.domain.DroolsOntologyAxiom;
import org.ihtsdo.drools.validator.rf2.domain.DroolsRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Long.parseLong;

public class SnomedDroolsComponentRepository {

	private static final String STATED_RELATIONSHIP_CHARACTERISTIC_TYPE_ID = "900000000000010007";

	private final Map<Long, DroolsConcept> conceptMap;
	private final Map<Long, DroolsDescription> descriptionMap;
	private final Set<Long> ungroupedAttributes;
	private final Set<DroolsOntologyAxiom> ontologyAxioms;


	private Logger logger = LoggerFactory.getLogger(getClass());

	public SnomedDroolsComponentRepository() {
		conceptMap = new Long2ObjectOpenHashMap<>();
		descriptionMap = new Long2ObjectOpenHashMap<>();
		ungroupedAttributes = new HashSet<>();
		ontologyAxioms = new HashSet<>();
	}

	public void addConcept(DroolsConcept concept) {
		conceptMap.put(parseLong(concept.getId()), concept);
	}

	public void addDescription(DroolsDescription description) {
		DroolsConcept concept = getConceptOrThrow(parseLong(description.getConceptId()), description);
		concept.getDescriptions().add(description);
		synchronized (descriptionMap) {
			descriptionMap.put(parseLong(description.getId()), description);
		}
	}

	public void addLanguageReferenceSetMember(String memberId, String referencedComponentId, String refsetId, String acceptabilityId) {
		long descriptionId = parseLong(referencedComponentId);
		DroolsDescription description = descriptionMap.get(descriptionId);
		if (description == null) {
			// TODO: This should be a warning and part of the validation report.
			logger.warn("Language reference set member " + memberId + " references Description " + descriptionId + " which can not be found.");
		}
		description.getAcceptabilityMap().put(refsetId, acceptabilityId);
	}

	public void addRelationship(DroolsRelationship relationship) {
		DroolsConcept concept = getConceptOrThrow(parseLong(relationship.getSourceId()), relationship);
		concept.getRelationships().add(relationship);

		if (relationship.isActive() && relationship.getCharacteristicTypeId().equals(STATED_RELATIONSHIP_CHARACTERISTIC_TYPE_ID)) {
			long destinationId = parseLong(relationship.getDestinationId());
			DroolsConcept destinationConcept = conceptMap.get(destinationId);
			if (destinationConcept == null) {
				// TODO: This should be a warning and part of the validation report.
				logger.warn("Relationship " + relationship.getId() + " has destination Concept " + relationship.getDestinationId() + " which can not be found.");
			}
			destinationConcept.getActiveInboundStatedRelationships().add(relationship);
		}
	}

	public void addOntologyAxiom(DroolsOntologyAxiom droolsOntologyAxiom) {
		ontologyAxioms.add(droolsOntologyAxiom);
		DroolsConcept concept = getConceptOrThrow(parseLong(droolsOntologyAxiom.getReferencedComponentId()), droolsOntologyAxiom);
		concept.getOntologyAxioms().add(droolsOntologyAxiom);
	}

	private DroolsConcept getConceptOrThrow(long conceptId, Component component) {
		DroolsConcept concept = conceptMap.get(conceptId);
		if (concept == null) {
			throw new RuntimeException(component.getClass().getSimpleName() + " " + component.getId() + " is part of Concept " + conceptId + " which can not be found.");
		}
		return concept;
	}

	public DroolsConcept getConcept(String conceptId) {
		return conceptMap.get(parseLong(conceptId));
	}

	public Collection<DroolsConcept> getConcepts() {
		return conceptMap.values();
	}

	public Collection<DroolsDescription> getDescriptions() {
		return descriptionMap.values();
	}

	public DroolsDescription getDescription(String descriptionId) {
		return descriptionMap.get(parseLong(descriptionId));
	}

	public Set<Long> getUngroupedAttributes() {
		return ungroupedAttributes;
	}

	public Set<DroolsOntologyAxiom> getOntologyAxioms() {
		return ontologyAxioms;
	}

	public void cleanup() {
		conceptMap.clear();
		descriptionMap.clear();
		ungroupedAttributes.clear();
		ontologyAxioms.clear();
	}
}
