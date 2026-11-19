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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Long.parseLong;

public class SnomedDroolsComponentFactory extends ComponentStoreComponentFactoryImpl {

	private static final String TEXT_DEFINITION = "900000000000550004";
	private static final String OWL_AXIOM_REFSET = "733073007";
	private static final String STRING_ANNOTATION_REFSET = "1292992004";
	private final SnomedDroolsComponentRepository repository;
	private final String authoringEffectiveTime;
	private final PreviousReleaseComponentFactory previousReleaseComponentIds;
	private final ComponentStore componentStore;
	private final boolean parallelAxiomParsing;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * @param parallelAxiomParsing whether the OWL axioms may be parsed on
	 *                             several threads; {@code false} parses them on
	 *                             the calling thread, so that turning parallel
	 *                             loading off makes the whole load single-threaded
	 */
	SnomedDroolsComponentFactory(ComponentStore componentStore, SnomedDroolsComponentRepository repository, String authoringEffectiveTime,
			PreviousReleaseComponentFactory previousReleaseComponentIds, boolean parallelAxiomParsing) {
        super(componentStore);
		this.componentStore = componentStore;
        this.repository = repository;
		this.authoringEffectiveTime = authoringEffectiveTime;
		this.previousReleaseComponentIds = previousReleaseComponentIds;
		this.parallelAxiomParsing = parallelAxiomParsing;
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
			// OWL OntologyAxiom reference set. Deferred rather than converted
			// here: parsing the OWL functional syntax is the most expensive single
			// thing in the load, and here it would run on whichever thread reads
			// this one file. loadingComponentsCompleted() parses in parallel and
			// applies the results serially, in file order.
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
	 * Axioms parsed before the batch is applied and its intermediates become
	 * collectable.
	 *
	 * <p>Smaller batches cut retention further but run slower, because the
	 * barrier between batches is what costs; larger ones hold more parsed
	 * relationships at once. 100,000 axioms is about 430,000 relationships,
	 * roughly 30 MB, which is a rounding error against a graph of several GB.
	 */
	private static final int AXIOM_BATCH_SIZE = 100_000;

	/**
	 * Appended from the thread reading the OWL refset file. A synchronized list
	 * because snomedboot may read several files at once and nothing guarantees
	 * that only one of them contains axioms.
	 */
	private final List<DeferredAxiom> deferredAxioms = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Parses the deferred axioms in batches, across all cores unless parallel
	 * loading is off, applying each batch on this thread before parsing the next.
	 *
	 * <p><b>Batched to bound memory.</b> Parsing all of them first holds every
	 * {@code AxiomRepresentation} at once - for an edition-sized refset that is
	 * millions of retained {@code owltoolkit.Relationship} instances plus their
	 * per-group maps, where converting inline made each one garbage immediately.
	 * A batch bounds the retention to {@value #AXIOM_BATCH_SIZE} representations
	 * regardless of edition size, and still gives every core work to do.
	 *
	 * <p>Only the parse is parallel; results are applied on this thread in file
	 * order. Applying calls {@code addStatedConceptParent} and
	 * {@code addStatedConceptChild}, which write the component store's concepts
	 * without synchronization, and {@link #addRelationships} builds composite
	 * identifiers whose repository contents are order-sensitive in principle.
	 * Applying in file order keeps the result independent of thread scheduling.
	 * Parsing is what costs; applying is cheap.
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

		// One converter per concurrent parse. AxiomRelationshipConversionService
		// owns a SnomedTaxonomyLoader, which owns an AxiomDeserialiser whose
		// deserialiseAxiom is synchronized (this) around a shared OWLOntology and
		// a shared axiom list - so sharing one converter between threads would
		// serialise them again, and sharing the deserialiser without the lock
		// would corrupt it. A thread borrows a converter for one parse and returns
		// it, so at most one is built per thread that takes part. The pool is
		// local to this call: the converters become garbage when it returns,
		// rather than staying reachable from the common pool's threads.
		Deque<AxiomRelationshipConversionService> converters = new ConcurrentLinkedDeque<>();
		for (int from = 0; from < rows.size(); from += AXIOM_BATCH_SIZE) {
			List<DeferredAxiom> batch = rows.subList(from, Math.min(from + AXIOM_BATCH_SIZE, rows.size()));
			Stream<DeferredAxiom> stream = parallelAxiomParsing ? batch.parallelStream() : batch.stream();
			for (ConvertedAxiom result : stream.map(row -> convert(row, converters)).toList()) {
				applyAxiom(result);
			}
		}
		logger.info("Converted {} OWL axioms in {} ms", rows.size(), System.currentTimeMillis() - start);
	}

	private static ConvertedAxiom convert(DeferredAxiom row, Deque<AxiomRelationshipConversionService> converters) {
		AxiomRelationshipConversionService converter = converters.poll();
		if (converter == null) {
			// The set of ungrouped attributes is not needed to convert axioms to relationships.
			converter = new AxiomRelationshipConversionService(Collections.emptySet());
		}
		try {
			AxiomRepresentation axiom = converter.convertAxiomToRelationships(row.owlExpression());
			Set<String> namedConceptIds = axiom != null ? null
					: converter.getIdsOfConceptsNamedInAxiom(row.owlExpression()).stream()
							.map(Object::toString).collect(Collectors.toSet());
			return new ConvertedAxiom(row, axiom, namedConceptIds, null);
		} catch (ConversionException | OWLParserException e) {
			return new ConvertedAxiom(row, null, null, e);
		} finally {
			converters.push(converter);
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
