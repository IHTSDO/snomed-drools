package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.drools.validator.rf2.domain.*;
import org.ihtsdo.otf.snomedboot.domain.ConceptConstants;
import org.ihtsdo.otf.snomedboot.factory.ImpotentComponentFactory;
import org.semanticweb.owlapi.io.OWLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.conversion.AxiomRelationshipConversionService;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;

public class SnomedDroolsComponentFactory extends ImpotentComponentFactory {

	private static final String TEXT_DEFINITION = "900000000000550004";
	private static final String INFERRED_RELATIONSHIP = "900000000000011006";
	private static final String OWL_AXIOM_REFSET = "733073007";
	static final String MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFSET = "723561005";

	private final SnomedDroolsComponentRepository repository;
	private final AxiomRelationshipConversionService axiomConverter;
	private final String currentEffectiveTime;
	private final PreviousReleaseComponentFactory previousReleaseComponentFactory;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private boolean axiomParsingError = false;

	SnomedDroolsComponentFactory(SnomedDroolsComponentRepository repository, String currentEffectiveTime, PreviousReleaseComponentFactory previousReleaseComponentFactory) {
		this.repository = repository;
		axiomConverter = new AxiomRelationshipConversionService(repository.getUngroupedAttributes());
		this.currentEffectiveTime = currentEffectiveTime;
		this.previousReleaseComponentFactory = previousReleaseComponentFactory;
	}

	@Override
	public void newConceptState(String conceptId, String effectiveTime, String active, String moduleId, String definitionStatusId) {
		repository.addConcept(new DroolsConcept(conceptId, isActive(active), moduleId, definitionStatusId,
				published(effectiveTime), previousReleaseComponentFactory != null ? previousReleaseComponentFactory.getReleasedConcepts().contains(conceptId) : published(effectiveTime)));
	}

	@Override
	public void newDescriptionState(String id, String effectiveTime, String active, String moduleId, String conceptId, String languageCode, String typeId, String term, String caseSignificanceId) {
		repository.addDescription(new DroolsDescription(id, isActive(active), moduleId, conceptId, languageCode, typeId, term, caseSignificanceId, TEXT_DEFINITION.equals(typeId),
				published(effectiveTime), previousReleaseComponentFactory != null ? previousReleaseComponentFactory.getReleasedDescriptions().contains(id) : published(effectiveTime)));
	}

	@Override
	public void newRelationshipState(String id, String effectiveTime, String active, String moduleId, String sourceId, String destinationId, String relationshipGroup, String typeId, String characteristicTypeId, String modifierId) {
		if (!characteristicTypeId.equals(INFERRED_RELATIONSHIP)) {
			repository.addRelationship(new DroolsRelationship(null, false, id, isActive(active), moduleId, sourceId, destinationId, Integer.parseInt(relationshipGroup), typeId, characteristicTypeId,
					published(effectiveTime), previousReleaseComponentFactory != null ? previousReleaseComponentFactory.getReleasedRelationships().contains(id) : published(effectiveTime), null));
		}
	}

	@Override
	public void newReferenceSetMemberState(String[] fieldNames, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... otherValues) {
		boolean activeBool = isActive(active);
		if (activeBool && refsetId.equals(OWL_AXIOM_REFSET)) {
			// OWL OntologyAxiom reference set

			// Fields: id	effectiveTime	active	moduleId	refsetId	referencedComponentId	owlExpression
			String owlExpression = otherValues[0];
			try {
				//AxiomRepresentation axiom = axiomConverter.convertAxiomToRelationships(parseLong(referencedComponentId), owlExpression);
				AxiomRepresentation axiom = axiomConverter.convertAxiomToRelationships(owlExpression);
				if (axiom != null) {
					if (axiom.getLeftHandSideNamedConcept() != null && axiom.getRightHandSideRelationships() != null) {
						// Regular axiom
						addRelationships(id, false, axiom.getLeftHandSideNamedConcept(), axiom.getRightHandSideRelationships(), moduleId, effectiveTime);
						// compare referencedComponentID and named concept in OWL expression
						validateComponentIdAndNamedConcept(id, activeBool, moduleId, parseLong(referencedComponentId), axiom.getLeftHandSideNamedConcept());
					} else if (axiom.getRightHandSideNamedConcept() != null && axiom.getLeftHandSideRelationships() != null) {
						// GCI OntologyAxiom
						addRelationships(id, true, axiom.getRightHandSideNamedConcept(), axiom.getLeftHandSideRelationships(), moduleId, effectiveTime);
						// compare referencedComponentID and named concept in OWL expression
						validateComponentIdAndNamedConcept(id, activeBool, moduleId, parseLong(referencedComponentId), axiom.getRightHandSideNamedConcept());
					}
				} else {
					// Can't be converted to relationships
					Set<String> namedConceptIds = axiomConverter.getIdsOfConceptsNamedInAxiom(owlExpression).stream().map(Object::toString).collect(Collectors.toSet());
					repository.addOntologyAxiom(new DroolsOntologyAxiom(id, activeBool, moduleId, referencedComponentId, owlExpression, namedConceptIds, published(effectiveTime), published(effectiveTime)));

				}
			} catch (ConversionException | OWLParserException e) {
				logger.warn("OntologyAxiom conversion failed for refset member " + id, e);
				repository.addComponentLoadingError(parseLong(referencedComponentId), new DroolsComponent(id, activeBool, moduleId, false, false),
						"Error parsing Axiom owlExpression for Axiom " + id);
			}

		} else if (activeBool && fieldNames.length == 7 && fieldNames[6].equals("acceptabilityId")) {
			// Language reference set
			String acceptabilityId = otherValues[0];
			repository.addLanguageReferenceSetMember(id, referencedComponentId, refsetId, acceptabilityId);
		}
	}

	boolean isAxiomParsingError() {
		return axiomParsingError;
	}

	private void addRelationships(String axiomId, boolean isGCI, Long namedConcept, Map<Integer, List<Relationship>> groups, String moduleId, String effectiveTime) {
		groups.forEach((group, relationships) -> relationships.forEach(relationship -> {

			long typeId = relationship.getTypeId();
			long destinationId = relationship.getDestinationId();

			// Build a composite identifier for this 'relationship' (which is actually a fragment of an axiom expression) because it doesn't have its own component identifier.
			String compositeIdentifier = axiomId + "/Group_" + group + "/Type_" + typeId + "/Destination_" + destinationId;

			DroolsRelationship relationship1 = new DroolsRelationship(axiomId, isGCI, compositeIdentifier, true, moduleId,
                    namedConcept.toString(), destinationId + "",
                    group, typeId + "", ConceptConstants.STATED_RELATIONSHIP, published(effectiveTime), published(effectiveTime), relationship.getValue() != null ? relationship.getValue().asString() : null);
			logger.info("Add axiom relationship {}", relationship1);
			repository.addRelationship(relationship1);
		}));
	}

	private boolean isActive(String active) {
		return "1".equals(active);
	}

	private boolean published(String effectiveTime) {
		return !currentEffectiveTime.equals(effectiveTime);
	}

	private void validateComponentIdAndNamedConcept(String axiomId, boolean active, String moduleId, Long referencedComponentId, Long namedConceptId) {
		if(!referencedComponentId.equals(namedConceptId)) {
			repository.addComponentLoadingError(referencedComponentId, new DroolsComponent(axiomId, active, moduleId, false, false),
					"ReferencedComponentId " + referencedComponentId + " does not match named concept " + namedConceptId + " in Axiom " + axiomId);
		}
	}
}
