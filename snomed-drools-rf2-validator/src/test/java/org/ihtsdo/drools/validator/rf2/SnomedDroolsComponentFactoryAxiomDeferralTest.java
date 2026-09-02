package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.drools.validator.rf2.domain.DroolsOntologyAxiom;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ComponentStore;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * OWL axioms are parsed once every file has been read, not inline.
 *
 * <p>Parsing the functional syntax is the most expensive part of loading a
 * release, and inline it runs on whichever thread happens to be reading the OWL
 * refset - so it is serial regardless of how many threads the loader was given.
 * Deferring lets the whole set be parsed across all cores.
 *
 * <p>These tests pin the observable half of that: nothing reaches the repository
 * until {@code loadingComponentsCompleted}, and everything reaches it once that
 * has run. They fail against inline conversion, which is the point.
 */
public class SnomedDroolsComponentFactoryAxiomDeferralTest {

	private static final String OWL_AXIOM_REFSET = "733073007";
	private static final String MODULE = "900000000000207008";

	/** A real row from the mini RF2 fixture. */
	private static final String AXIOM_ID = "4291bcd4-a002-4256-b15e-4a2f8b2c27c5";
	private static final String CONCEPT_ID = "100105001";
	private static final String OWL_EXPRESSION =
			"EquivalentClasses(:100105001 ObjectIntersectionOf(:100102001 ObjectSomeValuesFrom(:100104001 :100108001)))";

	private static final String[] AXIOM_FIELD_NAMES =
			{"id", "effectiveTime", "active", "moduleId", "refsetId", "referencedComponentId", "owlExpression"};

	private SnomedDroolsComponentRepository repository;
	private SnomedDroolsComponentFactory factory;

	@Before
	public void setup() {
		repository = new SnomedDroolsComponentRepository();
		factory = new SnomedDroolsComponentFactory(new ComponentStore(), repository, "20260831", null);
	}

	private void readAxiomRow() {
		factory.newReferenceSetMemberState(
				"sct2_sRefset_OWLExpressionSnapshot_INT_20190131.txt", 1, AXIOM_FIELD_NAMES,
				AXIOM_ID, "20190131", "1", MODULE, OWL_AXIOM_REFSET, CONCEPT_ID, OWL_EXPRESSION);
	}

	@Test
	public void anAxiomRowIsNotAppliedWhileTheFilesAreStillBeingRead() {
		readAxiomRow();

		assertTrue("axioms must not reach the repository until every file has been read, "
						+ "otherwise they are parsed one at a time on the reading thread",
				repository.getOntologyAxioms().isEmpty());
	}

	@Test
	public void everyDeferredAxiomIsAppliedOnceLoadingCompletes() throws ReleaseImportException {
		readAxiomRow();

		factory.loadingComponentsCompleted();

		Set<DroolsOntologyAxiom> axioms = repository.getOntologyAxioms();
		assertEquals(1, axioms.size());
		DroolsOntologyAxiom axiom = axioms.iterator().next();
		assertEquals(CONCEPT_ID, axiom.getReferencedComponentId());
		assertEquals(OWL_EXPRESSION, axiom.getOwlExpression());
	}

	/**
	 * The relationships an axiom expands into have to arrive too - they are what
	 * most of the axiom rules actually match on, so losing them would quietly
	 * reduce findings rather than fail anything.
	 */
	@Test
	public void theRelationshipsInsideAnAxiomAreAppliedAsWell() throws ReleaseImportException {
		// addRelationship attaches to the source concept, so it has to exist.
		factory.newConceptState("sct2_Concept_Snapshot_INT_20190131.txt", 1, CONCEPT_ID,
				"20190131", "1", MODULE, "900000000000074008");
		readAxiomRow();
		assertTrue(repository.getConcept(CONCEPT_ID).getRelationships().isEmpty());

		factory.loadingComponentsCompleted();

		assertFalse("an axiom's relationships are what most axiom rules match on, "
						+ "so losing them would quietly reduce findings",
				repository.getConcept(CONCEPT_ID).getRelationships().isEmpty());
	}

	/**
	 * An unparseable expression must still be reported, and reported once. It is
	 * recorded as a component loading error rather than thrown, so a single bad
	 * axiom does not abandon the release.
	 */
	@Test
	public void anUnparseableAxiomIsRecordedAsALoadingError() throws ReleaseImportException {
		factory.newReferenceSetMemberState(
				"sct2_sRefset_OWLExpressionSnapshot_INT_20190131.txt", 1, AXIOM_FIELD_NAMES,
				AXIOM_ID, "20190131", "1", MODULE, OWL_AXIOM_REFSET, CONCEPT_ID, "This is not OWL");

		factory.loadingComponentsCompleted();

		assertFalse("a bad axiom must be reported, not dropped",
				repository.getComponentLoadingErrors().isEmpty());
	}

	@Test
	public void completingWithNoAxiomsIsHarmless() throws ReleaseImportException {
		factory.loadingComponentsCompleted();

		assertTrue(repository.getOntologyAxioms().isEmpty());
		assertTrue(repository.getComponentLoadingErrors().isEmpty());
	}

	/**
	 * Inactive rows were never converted and must still not be.
	 */
	@Test
	public void anInactiveAxiomRowIsIgnored() throws ReleaseImportException {
		factory.newReferenceSetMemberState(
				"sct2_sRefset_OWLExpressionSnapshot_INT_20190131.txt", 1, AXIOM_FIELD_NAMES,
				AXIOM_ID, "20190131", "0", MODULE, OWL_AXIOM_REFSET, CONCEPT_ID, OWL_EXPRESSION);

		factory.loadingComponentsCompleted();

		assertTrue(repository.getOntologyAxioms().isEmpty());
	}
}
