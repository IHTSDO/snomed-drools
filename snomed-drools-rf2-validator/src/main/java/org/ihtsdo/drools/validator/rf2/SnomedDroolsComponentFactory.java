package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.validator.rf2.domain.*;
import org.ihtsdo.otf.snomedboot.domain.ConceptConstants;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ComponentStore;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ComponentStoreComponentFactoryImpl;
import org.jetbrains.annotations.NotNull;
import org.semanticweb.owlapi.io.OWLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.owltoolkit.conversion.AxiomRelationshipConversionService;
import org.snomed.otf.owltoolkit.conversion.ConversionException;
import org.snomed.otf.owltoolkit.domain.AxiomRepresentation;
import org.snomed.otf.owltoolkit.domain.Relationship;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;

public class SnomedDroolsComponentFactory extends ComponentStoreComponentFactoryImpl {

	private static final String TEXT_DEFINITION = "900000000000550004";
	private static final String OWL_AXIOM_REFSET = "733073007";
	private final SnomedDroolsComponentRepository repository;
	private final String authoringEffectiveTime;
	private final PreviousReleaseComponentFactory previousReleaseComponentIds;
	private final AxiomRelationshipConversionService axiomConverter;
	private final ComponentStore componentStore;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	SnomedDroolsComponentFactory(ComponentStore componentStore, SnomedDroolsComponentRepository repository, String authoringEffectiveTime, PreviousReleaseComponentFactory previousReleaseComponentIds) {
        super(componentStore);
		this.componentStore = componentStore;
        this.repository = repository;
		this.authoringEffectiveTime = authoringEffectiveTime;
		this.previousReleaseComponentIds = previousReleaseComponentIds;
		axiomConverter = new AxiomRelationshipConversionService(Collections.emptySet());// The set of ungrouped attributes are not needed to convert axioms to relationships.
	}

	public ComponentStore getComponentStore() {
		return this.componentStore;
	}

	@Override
	public void newConceptState(String conceptId, String effectiveTime, String active, String moduleId, String definitionStatusId) {
		super.newConceptState(conceptId, effectiveTime, active, moduleId, definitionStatusId);
		repository.addConcept(new DroolsConcept(conceptId, effectiveTime, isActive(active), moduleId, definitionStatusId,
				isThisStatePublished(effectiveTime), isThisConceptReleased(conceptId, effectiveTime)));
	}

	@Override
	public void newDescriptionState(String id, String effectiveTime, String active, String moduleId, String conceptId, String languageCode, String typeId, String term, String caseSignificanceId) {
		repository.addDescription(new DroolsDescription(id, effectiveTime, isActive(active), moduleId, conceptId, languageCode, typeId, term, caseSignificanceId, TEXT_DEFINITION.equals(typeId),
				isThisStatePublished(effectiveTime), isThisDescriptionReleased(id, effectiveTime)));
	}

	@Override
	public void newRelationshipState(String id, String effectiveTime, String active, String moduleId, String sourceId, String destinationId, String relationshipGroup, String typeId, String characteristicTypeId, String modifierId) {
		repository.addRelationship(new DroolsRelationship(null, effectiveTime, false, id, isActive(active), moduleId, sourceId, destinationId, Integer.parseInt(relationshipGroup), typeId, characteristicTypeId,
				isThisStatePublished(effectiveTime), isThisRelationshipReleased(id, effectiveTime), null));
	}

	@Override
	public void newReferenceSetMemberState(String[] fieldNames, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... otherValues) {
		boolean activeBool = isActive(active);

		if (activeBool && refsetId.equals(OWL_AXIOM_REFSET)) {
			// OWL OntologyAxiom reference set

			// Fields: id	effectiveTime	active	moduleId	refsetId	referencedComponentId	owlExpression
			String owlExpression = otherValues[0];
			try {
				AxiomRepresentation axiom = axiomConverter.convertAxiomToRelationships(owlExpression);
				final boolean published = isThisStatePublished(effectiveTime);
				final boolean released = isThisRefsetMemberReleased(id, effectiveTime);
				if (axiom != null) {
					boolean axiomGCI = false;
					if (axiom.getLeftHandSideNamedConcept() != null && axiom.getRightHandSideRelationships() != null) {
						// Regular axiom
						addRelationships(id, effectiveTime, false, axiom.getLeftHandSideNamedConcept(), axiom.getRightHandSideRelationships(), moduleId, published, released);
						// compare referencedComponentID and named concept in OWL expression
						validateComponentIdAndNamedConcept(id, effectiveTime, activeBool, moduleId, parseLong(referencedComponentId), axiom.getLeftHandSideNamedConcept());
					} else if (axiom.getRightHandSideNamedConcept() != null && axiom.getLeftHandSideRelationships() != null) {
						// GCI OntologyAxiom
						axiomGCI = true;
						addRelationships(id, effectiveTime, true, axiom.getRightHandSideNamedConcept(), axiom.getLeftHandSideRelationships(), moduleId, published, released);
						// compare referencedComponentID and named concept in OWL expression
						validateComponentIdAndNamedConcept(id, effectiveTime, activeBool, moduleId, parseLong(referencedComponentId), axiom.getRightHandSideNamedConcept());
					}
					repository.addOntologyAxiom(new DroolsOntologyAxiom(id, effectiveTime, activeBool, moduleId, referencedComponentId, owlExpression, null, published, released, axiom.isPrimitive(), axiomGCI));
				} else {
					// Can't be converted to relationships
					Set<String> namedConceptIds = axiomConverter.getIdsOfConceptsNamedInAxiom(owlExpression).stream().map(Object::toString).collect(Collectors.toSet());
					repository.addOntologyAxiom(new DroolsOntologyAxiom(id, effectiveTime, activeBool, moduleId, referencedComponentId, owlExpression, namedConceptIds, published, released, true, false));
				}
			} catch (ConversionException | OWLParserException e) {
				logger.warn("OntologyAxiom conversion failed for refset member " + id, e);
				repository.addComponentLoadingError(parseLong(referencedComponentId), new DroolsComponent(id, effectiveTime, activeBool, moduleId, false, false),
						"Error parsing Axiom owlExpression for Axiom " + id);
			}

		} else if (activeBool && fieldNames.length == 7 && fieldNames[6].equals("acceptabilityId")) {
			// Language reference set
			String acceptabilityId = otherValues[0];
			repository.addLanguageReferenceSetMember(id, referencedComponentId, refsetId, acceptabilityId);
		} else if (activeBool && (Constants.historicalAssociationNames.keySet().contains(refsetId))) {
			String targetComponentId = otherValues[0];
			repository.addAssociationTargetMember(id, refsetId, referencedComponentId, targetComponentId);
		}
	}

	private void addRelationships(String axiomId, String effectiveTime, boolean isGCI, Long namedConcept, Map<Integer, List<Relationship>> groups, String moduleId, boolean published, boolean released) {
		groups.forEach((group, relationships) -> relationships.forEach(relationship -> {

			long typeId = relationship.getTypeId();
			long destinationId = relationship.getDestinationId();
			Relationship.ConcreteValue concreteValue = relationship.getValue();

			// Build a composite identifier for this 'relationship' (which is actually a fragment of an axiom expression) because it doesn't have its own component identifier.
			String compositeIdentifier = getCompositeIdentifier(axiomId, group, concreteValue, destinationId, typeId);

			DroolsRelationship droolsRelationship = new DroolsRelationship(axiomId, effectiveTime, isGCI, compositeIdentifier, true, moduleId,
					namedConcept.toString(), destinationId != -1 ? destinationId + "" : null,
					group, typeId + "", ConceptConstants.STATED_RELATIONSHIP, published, released, concreteValue != null ? concreteValue.asString() : null);
			logger.debug("Add axiom relationship {}", droolsRelationship);
			repository.addRelationship(droolsRelationship);

			if (!isGCI && ConceptConstants.isA.equals(typeId + "")) {
				this.addStatedConceptParent(namedConcept.toString(), destinationId + "");
				this.addStatedConceptChild(namedConcept.toString(), destinationId + "");
			}
		}));
	}

	@NotNull
	private static String getCompositeIdentifier(String axiomId, Integer group, Relationship.ConcreteValue concreteValue, long destinationId, long typeId) {
		final String destination = destinationId != -1 ? String.valueOf(destinationId) : "";
		final String destinationOrConcrete = concreteValue == null ? "/Destination_" + destination : "/ConcreteValue_" + concreteValue.asString();
		return (axiomId + "/Group_" + group + "/Type_" + typeId + destinationOrConcrete);
	}

	private boolean isActive(String active) {
		return "1".equals(active);
	}

	private void validateComponentIdAndNamedConcept(String axiomId, String effectiveTime, boolean active, String moduleId, Long referencedComponentId, Long namedConceptId) {
		if(!referencedComponentId.equals(namedConceptId)) {
			repository.addComponentLoadingError(referencedComponentId, new DroolsComponent(axiomId, effectiveTime, active, moduleId, false, false),
					"ReferencedComponentId " + referencedComponentId + " does not match named concept " + namedConceptId + " in Axiom " + axiomId);
		}
	}

	private boolean isThisStatePublished(String effectiveTime) {
		return !Objects.equals(authoringEffectiveTime, effectiveTime);
	}

	private boolean isThisConceptReleased(String id, String effectiveTime) {
		return (previousReleaseComponentIds != null && previousReleaseComponentIds.getReleasedConceptIds().contains(parseLong(id))) || isThisStatePublished(effectiveTime);
	}

	private boolean isThisDescriptionReleased(String id, String effectiveTime) {
		return (previousReleaseComponentIds != null && previousReleaseComponentIds.getReleasedDescriptionIds().contains(parseLong(id))) || isThisStatePublished(effectiveTime);
	}

	private boolean isThisRelationshipReleased(String id, String effectiveTime) {
		return (previousReleaseComponentIds != null && previousReleaseComponentIds.getReleasedRelationshipIds().contains(parseLong(id))) || isThisStatePublished(effectiveTime);
	}

	private boolean isThisRefsetMemberReleased(String id, String effectiveTime) {
		return (previousReleaseComponentIds != null && previousReleaseComponentIds.getReleaseRefsetMemberIds().contains(id)) || isThisStatePublished(effectiveTime);
	}
}
