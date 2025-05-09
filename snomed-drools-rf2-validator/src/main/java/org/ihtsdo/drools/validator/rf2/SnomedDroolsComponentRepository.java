package org.ihtsdo.drools.validator.rf2;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.ihtsdo.drools.domain.Component;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.helper.IdentifierHelper;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.response.Severity;
import org.ihtsdo.drools.validator.rf2.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.Long.parseLong;

public class SnomedDroolsComponentRepository {

	private static final String STATED_RELATIONSHIP_CHARACTERISTIC_TYPE_ID = "900000000000010007";

	private final Map<Long, DroolsConcept> conceptMap;
	private final Map<Long, DroolsDescription> descriptionMap;
	private final Set<DroolsOntologyAxiom> ontologyAxioms;
	private final List<InvalidContent> componentLoadingErrors;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public SnomedDroolsComponentRepository() {
		conceptMap = new Long2ObjectOpenHashMap<>();
		descriptionMap = new Long2ObjectOpenHashMap<>();
		ontologyAxioms = new HashSet<>();
		componentLoadingErrors = new ArrayList<>();
	}

	public void addConcept(DroolsConcept concept) {
		conceptMap.put(parseLong(concept.getId()), concept);
	}

	public void addDescription(DroolsDescription description) {
		Optional<DroolsConcept> conceptOptional = getConceptOrRecordError(parseLong(description.getConceptId()), description);
		conceptOptional.ifPresent(concept -> {
			concept.getDescriptions().add(description);
			synchronized (descriptionMap) {
				descriptionMap.put(parseLong(description.getId()), description);
			}
		});
	}

	public synchronized void addLanguageReferenceSetMember(String memberId, String referencedComponentId, String refsetId, String acceptabilityId) {
		long descriptionId = parseLong(referencedComponentId);
		DroolsDescription description = descriptionMap.get(descriptionId);
		if (description == null) {
			// TODO: This should be a warning and part of the validation report.
			String message = "Language reference set member " + memberId + " references Description " + descriptionId + " which can not be found.";
			logger.warn(message);
		} else {
			description.getAcceptabilityMap().put(refsetId, acceptabilityId);
		}
	}

	public synchronized void addAssociationTargetMember(String memberId, String refsetId, String referencedComponentId, String targetComponentId) {
		if (!IdentifierHelper.isConceptId(referencedComponentId)) return;
		
		long conceptId = parseLong(referencedComponentId);
		DroolsConcept concept = conceptMap.get(conceptId);
		if (concept == null) {
			// TODO: This should be a warning and part of the validation report.
			String message = "Association reference set member " + memberId + " references Concept " + conceptId + " which can not be found.";
			logger.warn(message);
		} else {
			String association = Constants.historicalAssociationNames.get(refsetId);
			if (association == null) {
				association = refsetId;
			}
			Map<String, Set<String>> associationTargetMap = concept.getAssociationTargets();
			Set<String> associationType = associationTargetMap.computeIfAbsent(association, k -> new HashSet<>());
			associationType.add(targetComponentId);
		}
	}

	public void addRelationship(DroolsRelationship relationship) {
		Optional<DroolsConcept> conceptOptional = getConceptOrRecordError(parseLong(relationship.getSourceId()), relationship);
		conceptOptional.ifPresent(concept -> {
			concept.getRelationships().add(relationship);
			if (relationship.isActive() && relationship.getCharacteristicTypeId().equals(STATED_RELATIONSHIP_CHARACTERISTIC_TYPE_ID)) {
				if (relationship.getDestinationId() != null) {
					long destinationId = parseLong(relationship.getDestinationId());
					DroolsConcept destinationConcept = conceptMap.get(destinationId);
					if (destinationConcept == null) {
						// TODO: This should be a warning and part of the validation report.
						String message = "Relationship " + relationship.getId() + " has destination Concept " + relationship.getDestinationId() + " which can not be found.";
						logger.warn(message);
						addComponentLoadingWarning(parseLong(concept.getId()), relationship, message);
					} else {
						destinationConcept.getActiveInboundStatedRelationships().add(relationship);
					}
				}
			}
		});
	}

	public void addOntologyAxiom(DroolsOntologyAxiom droolsOntologyAxiom) {
		ontologyAxioms.add(droolsOntologyAxiom);
		Optional<DroolsConcept> conceptOptional = getConceptOrRecordError(parseLong(droolsOntologyAxiom.getReferencedComponentId()), droolsOntologyAxiom);
		conceptOptional.ifPresent(concept -> concept.getOntologyAxioms().add(droolsOntologyAxiom));
	}

	private Optional<DroolsConcept> getConceptOrRecordError(long conceptId, Component component) {
		DroolsConcept concept = conceptMap.get(conceptId);
		if (concept == null) {
			StringBuilder errorMessageBuilder = new StringBuilder();
			if (component instanceof DroolsDescription) {
				errorMessageBuilder.append("Description ");
			} else if (component instanceof DroolsRelationship) {
				errorMessageBuilder.append("Relationship ");
			} else if (component instanceof DroolsOntologyAxiom) {
				errorMessageBuilder.append("Axiom Refset ");
			} else {
				errorMessageBuilder.append("Component ");
			}
			errorMessageBuilder.append(component.getId()).append(" references conceptId ").append(conceptId).append(" which does not exist");
			addComponentLoadingError(conceptId, component, errorMessageBuilder.toString());
			return Optional.empty();
		}
		return Optional.of(concept);
	}

	public synchronized void addComponentLoadingError(long conceptId, Component component, String message) {
		componentLoadingErrors.add(new InvalidContent("component-loading-error", conceptId + "", component, message, Severity.ERROR));
	}

	public synchronized void addComponentLoadingWarning(long conceptId, Component component, String message) {
		componentLoadingErrors.add(new InvalidContent("component-loading-warning", conceptId + "", component, message, Severity.WARNING));
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

	public Set<DroolsOntologyAxiom> getOntologyAxioms() {
		return ontologyAxioms;
	}

	public List<InvalidContent> getComponentLoadingErrors() {
		return componentLoadingErrors;
	}

	public void cleanup() {
		conceptMap.clear();
		descriptionMap.clear();
		ontologyAxioms.clear();
	}
}
