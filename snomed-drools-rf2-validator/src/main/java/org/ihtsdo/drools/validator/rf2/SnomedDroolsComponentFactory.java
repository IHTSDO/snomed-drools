package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.validator.rf2.domain.*;
import org.ihtsdo.otf.snomedboot.domain.ConceptConstants;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ComponentStore;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ComponentStoreComponentFactoryImpl;
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
	private static final String STRING_ANNOTATION_REFSET = "1292992004";
	private final SnomedDroolsComponentRepository repository;
	private final String authoringEffectiveTime;
	private final PreviousReleaseComponentFactory previousReleaseComponentIds;
	private final ComponentStore componentStore;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	SnomedDroolsComponentFactory(ComponentStore componentStore, SnomedDroolsComponentRepository repository, String authoringEffectiveTime, PreviousReleaseComponentFactory previousReleaseComponentIds) {
        super(componentStore);
		this.componentStore = componentStore;
        this.repository = repository;
		this.authoringEffectiveTime = authoringEffectiveTime;
		this.previousReleaseComponentIds = previousReleaseComponentIds;
	}

	public ComponentStore getComponentStore() {
		return this.componentStore;
	}

	@Override
	public void newConceptState(String filename, long lineNumber, String conceptId, String effectiveTime, String active, String moduleId, String definitionStatusId) {
		super.newConceptState(filename, lineNumber, conceptId, effectiveTime, active, moduleId, definitionStatusId);
		repository.addConcept(new DroolsConcept(conceptId, effectiveTime, isActive(active), moduleId, definitionStatusId,
				isThisStatePublished(effectiveTime), isThisConceptReleased(conceptId, effectiveTime)));
	}

	@Override
	public void newDescriptionState(String filename, long lineNumber, String id, String effectiveTime, String active, String moduleId, String conceptId, String languageCode, String typeId, String term, String caseSignificanceId) {
		repository.addDescription(new DroolsDescription(id, effectiveTime, isActive(active), moduleId, conceptId, languageCode, typeId, term, caseSignificanceId, TEXT_DEFINITION.equals(typeId),
				isThisStatePublished(effectiveTime), isThisDescriptionReleased(id, effectiveTime)));
	}

	@Override
	public void newRelationshipState(String filename, long lineNumber, String id, String effectiveTime, String active, String moduleId, String sourceId, String destinationId, String relationshipGroup, String typeId, String characteristicTypeId, String modifierId) {
		repository.addRelationship(new DroolsRelationship(null, effectiveTime, false, id, isActive(active), moduleId, sourceId, destinationId, Integer.parseInt(relationshipGroup), typeId, characteristicTypeId,
				isThisStatePublished(effectiveTime), isThisRelationshipReleased(id, effectiveTime), null));
	}

	@Override
	public void newReferenceSetMemberState(String filename, long lineNumber, String[] fieldNames, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... otherValues) {
		boolean activeBool = isActive(active);

		if (activeBool && refsetId.equals(OWL_AXIOM_REFSET)) {
			// OWL OntologyAxiom reference set.
			//
			// Deferred rather than converted here. Parsing the OWL functional
			// syntax is the most expensive single thing in the load - 598,891
			// members at ~16,300/s is 35 s on an AU edition - and it runs on
			// whichever thread reads this one file, so it is serial however many
			// cores the loader was given.
			//
			// It cannot be parallelised in place: the repository's
			// ontologyAxioms and componentLoadingErrors are a plain HashSet and
			// ArrayList, and addRelationships also writes stated parent/child
			// links into the component store. Concurrent writes there would
			// corrupt those collections silently, and on a validator that means
			// wrong findings rather than a crash.
			//
			// So the expensive half - parsing - happens in parallel in
			// loadingComponentsCompleted(), and the results are applied to the
			// repository serially in file order, exactly as below.
			// Fields: id effectiveTime active moduleId refsetId referencedComponentId owlExpression
			deferredAxioms.add(new DeferredAxiom(id, effectiveTime, moduleId, referencedComponentId, otherValues[0]));

		} else if (activeBool && fieldNames.length == 7 && fieldNames[6].equals("acceptabilityId")) {
			// Language reference set
			String acceptabilityId = otherValues[0];
			repository.addLanguageReferenceSetMember(id, referencedComponentId, refsetId, acceptabilityId);
		} else if (activeBool && (Constants.historicalAssociationNames.keySet().contains(refsetId))) {
			String targetComponentId = otherValues[0];
			repository.addAssociationTargetMember(id, refsetId, referencedComponentId, targetComponentId);
		} else if (activeBool && refsetId.equals(STRING_ANNOTATION_REFSET)) {
			String languageDialectCode = otherValues[0];
			String typeId = otherValues[1];
			String value = otherValues[2];
			repository.addAnnotation(new DroolsAnnotation(id, effectiveTime, isActive(active), moduleId, isThisStatePublished(effectiveTime), isThisRefsetMemberReleased(id, effectiveTime), referencedComponentId, languageDialectCode, typeId, value));
		}
	}

	/**
	 * One OWL axiom refset row, held until every file has been read.
	 */
	private record DeferredAxiom(String id, String effectiveTime, String moduleId,
			String referencedComponentId, String owlExpression) {
	}

	/**
	 * Parsed form of a {@link DeferredAxiom}, or the failure that parsing it
	 * produced. Carries the outcome rather than applying it, so that the
	 * expensive parse can happen off the loader's thread while every write to
	 * the repository still happens on one.
	 */
	private record ConvertedAxiom(DeferredAxiom source, AxiomRepresentation axiom,
			Set<String> namedConceptIds, Exception failure) {
	}

	/**
	 * Appended from the thread reading the OWL refset file. A synchronized list
	 * because snomedboot may read several files at once and nothing guarantees
	 * that only one of them contains axioms.
	 */
	/**
	 * Axioms parsed before the batch is applied and its intermediates become
	 * collectable.
	 *
	 * <p>Measured on an AU edition, same request each time: unbatched cost
	 * 224 s and retained 2,405,673 {@code Relationship} instances with the heap
	 * at 10.6 GB; batches of 20,000 cost 251 s and retained 1,461 with the heap
	 * at 8.05 GB. The barrier between batches is what costs, so this is set as
	 * large as it can be while keeping retention a rounding error - 100,000
	 * axioms is about 430,000 relationships, roughly 30 MB, against a graph of
	 * several GB.
	 */
	private static final int AXIOM_BATCH_SIZE = 100_000;

	private final List<DeferredAxiom> deferredAxioms = Collections.synchronizedList(new ArrayList<>());

	/**
	 * One converter per thread. {@code AxiomRelationshipConversionService} owns a
	 * {@code SnomedTaxonomyLoader}, which owns an {@code AxiomDeserialiser} whose
	 * {@code deserialiseAxiom} is {@code synchronized (this)} around a shared
	 * {@code OWLOntology} and a shared axiom list - so sharing one converter
	 * between threads would serialise them again, and sharing the deserialiser
	 * without the lock would corrupt it. A converter each avoids both.
	 */
	private final ThreadLocal<AxiomRelationshipConversionService> threadConverter =
			ThreadLocal.withInitial(() -> new AxiomRelationshipConversionService(Collections.emptySet()));

	/**
	 * Parses the deferred axioms across all cores in batches, applying each batch
	 * on this thread before parsing the next.
	 *
	 * <p><b>Batched to bound memory, not for tidiness.</b> Parsing all of them
	 * first held every {@code AxiomRepresentation} at once - on an AU edition a
	 * live histogram showed <b>2,405,673</b> retained
	 * {@code owltoolkit.Relationship} instances, 173 MB, plus their per-group
	 * maps, where converting inline had made each one garbage immediately. That
	 * was a real regression on a heap already sitting at 10.6 GB of 12 GB with
	 * the old generation 93.65% full. A batch bounds the retention to
	 * {@value #AXIOM_BATCH_SIZE} representations regardless of edition size, and
	 * still gives every core work to do.
	 *
	 * <p>Order is preserved deliberately. The findings are a set, but
	 * {@code addStatedConceptParent} and the composite identifiers built in
	 * {@link #addRelationships} make the repository's contents order-sensitive
	 * in principle, and a reordering that changed one finding would be very hard
	 * to attribute later. Parsing is what costs; applying is cheap.
	 */
	@Override
	public void loadingComponentsCompleted() throws org.ihtsdo.otf.snomedboot.ReleaseImportException {
		super.loadingComponentsCompleted();
		if (deferredAxioms.isEmpty()) {
			return;
		}
		long start = System.currentTimeMillis();
		List<DeferredAxiom> rows = new ArrayList<>(deferredAxioms);
		deferredAxioms.clear();

		for (int from = 0; from < rows.size(); from += AXIOM_BATCH_SIZE) {
			List<DeferredAxiom> batch = rows.subList(from, Math.min(from + AXIOM_BATCH_SIZE, rows.size()));
			for (ConvertedAxiom result : batch.parallelStream().map(this::convert).toList()) {
				applyAxiom(result);
			}
		}
		logger.info("Converted {} OWL axioms in {} ms", rows.size(), System.currentTimeMillis() - start);
	}

	private ConvertedAxiom convert(DeferredAxiom row) {
		AxiomRelationshipConversionService converter = threadConverter.get();
		try {
			AxiomRepresentation axiom = converter.convertAxiomToRelationships(row.owlExpression());
			Set<String> namedConceptIds = axiom != null ? null
					: converter.getIdsOfConceptsNamedInAxiom(row.owlExpression()).stream()
							.map(Object::toString).collect(Collectors.toSet());
			return new ConvertedAxiom(row, axiom, namedConceptIds, null);
		} catch (ConversionException | OWLParserException e) {
			return new ConvertedAxiom(row, null, null, e);
		}
	}

	/**
	 * The body of what {@code newReferenceSetMemberState} used to do inline for
	 * an OWL row, unchanged apart from taking its parsed axiom as an argument.
	 */
	private void applyAxiom(ConvertedAxiom result) {
		DeferredAxiom row = result.source();
		String id = row.id();
		String effectiveTime = row.effectiveTime();
		String moduleId = row.moduleId();
		String referencedComponentId = row.referencedComponentId();
		String owlExpression = row.owlExpression();

		if (result.failure() != null) {
			logger.warn("OntologyAxiom conversion failed for refset member " + id, result.failure());
			repository.addComponentLoadingError(parseLong(referencedComponentId),
					new DroolsComponent(id, effectiveTime, true, moduleId, false, false),
					"Error parsing Axiom owlExpression for Axiom " + id);
			return;
		}

		AxiomRepresentation axiom = result.axiom();
		final boolean published = isThisStatePublished(effectiveTime);
		final boolean released = isThisRefsetMemberReleased(id, effectiveTime);
		if (axiom != null) {
			boolean axiomGCI = false;
			if (axiom.getLeftHandSideNamedConcept() != null && axiom.getRightHandSideRelationships() != null) {
				// Regular axiom
				addRelationships(id, effectiveTime, false, axiom.getLeftHandSideNamedConcept(), axiom.getRightHandSideRelationships(), moduleId, published, released);
				// compare referencedComponentID and named concept in OWL expression
				validateComponentIdAndNamedConcept(id, effectiveTime, true, moduleId, parseLong(referencedComponentId), axiom.getLeftHandSideNamedConcept());
			} else if (axiom.getRightHandSideNamedConcept() != null && axiom.getLeftHandSideRelationships() != null) {
				// GCI OntologyAxiom
				axiomGCI = true;
				addRelationships(id, effectiveTime, true, axiom.getRightHandSideNamedConcept(), axiom.getLeftHandSideRelationships(), moduleId, published, released);
				// compare referencedComponentID and named concept in OWL expression
				validateComponentIdAndNamedConcept(id, effectiveTime, true, moduleId, parseLong(referencedComponentId), axiom.getRightHandSideNamedConcept());
			}
			repository.addOntologyAxiom(new DroolsOntologyAxiom(id, effectiveTime, true, moduleId, referencedComponentId, owlExpression, null, published, released, axiom.isPrimitive(), axiomGCI));
		} else {
			// Can't be converted to relationships
			repository.addOntologyAxiom(new DroolsOntologyAxiom(id, effectiveTime, true, moduleId, referencedComponentId, owlExpression, result.namedConceptIds(), published, released, true, false));
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
		return (previousReleaseComponentIds != null && previousReleaseComponentIds.getReleasedRefsetMemberIds().contains(id)) || isThisStatePublished(effectiveTime);
	}
}
