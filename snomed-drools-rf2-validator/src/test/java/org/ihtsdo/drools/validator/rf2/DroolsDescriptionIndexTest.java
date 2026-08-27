package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DroolsDescriptionIndexTest {

	private static final String CONCEPT_ID = "100101001";
	private static final String MODULE_ID = "900000000000207008";
	private static final String SYNONYM = "900000000000013009";

	private SnomedDroolsComponentRepository repository;

	@Before
	public void setup() {
		repository = new SnomedDroolsComponentRepository();
		repository.addConcept(new DroolsConcept(CONCEPT_ID, "20260731", true, MODULE_ID, "900000000000074008", true, true));
	}

	@Test
	public void findsActiveDescriptionByExactTerm() {
		addDescription("1000011", "Heart structure", true);

		DroolsDescriptionIndex index = new DroolsDescriptionIndex(repository);

		assertEquals(Set.of("1000011"), index.findMatchedDescriptionTerm("Heart structure", true));
	}

	@Test
	public void separatesActiveFromInactiveDescriptionsSharingATerm() {
		addDescription("1000011", "Heart structure", true);
		addDescription("1000029", "Heart structure", false);

		DroolsDescriptionIndex index = new DroolsDescriptionIndex(repository);

		assertEquals(Set.of("1000011"), index.findMatchedDescriptionTerm("Heart structure", true));
		assertEquals(Set.of("1000029"), index.findMatchedDescriptionTerm("Heart structure", false));
	}

	@Test
	public void returnsEveryDescriptionSharingATerm() {
		addDescription("1000011", "Heart structure", true);
		addDescription("1000037", "Heart structure", true);
		addDescription("1000045", "Heart structure", true);

		DroolsDescriptionIndex index = new DroolsDescriptionIndex(repository);

		assertEquals(Set.of("1000011", "1000037", "1000045"),
				index.findMatchedDescriptionTerm("Heart structure", true));
	}

	@Test
	public void matchesTheWholeTermRatherThanAnyWordWithinIt() {
		addDescription("1000011", "Heart structure", true);

		DroolsDescriptionIndex index = new DroolsDescriptionIndex(repository);

		// A search engine would tokenise the term and match on "Heart"; this
		// lookup is exact, which is the behaviour the rules depend on.
		assertTrue(index.findMatchedDescriptionTerm("Heart", true).isEmpty());
		assertTrue(index.findMatchedDescriptionTerm("heart structure", true).isEmpty());
		assertTrue(index.findMatchedDescriptionTerm("Heart structures", true).isEmpty());
	}

	@Test
	public void returnsNothingForAnUnknownTermOrAfterCleanup() {
		addDescription("1000011", "Heart structure", true);

		DroolsDescriptionIndex index = new DroolsDescriptionIndex(repository);
		assertTrue(index.findMatchedDescriptionTerm("Lung structure", true).isEmpty());

		index.cleanup();
		assertTrue(index.findMatchedDescriptionTerm("Heart structure", true).isEmpty());
	}

	private void addDescription(String descriptionId, String term, boolean active) {
		repository.addDescription(new DroolsDescription(descriptionId, "20260731", active, MODULE_ID,
				CONCEPT_ID, "en", SYNONYM, term, "900000000000448009", false, true, true));
	}
}
