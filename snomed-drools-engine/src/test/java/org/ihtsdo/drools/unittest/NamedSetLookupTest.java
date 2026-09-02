package org.ihtsdo.drools.unittest;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.service.DescriptionService;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;

/**
 * The guarantee that makes {@link DescriptionService#isInNamedSet} safe to add:
 * an implementation written before it existed keeps compiling, and answers in a
 * way that leaves a rule using it behaving exactly as it did before.
 *
 * <p>{@link LegacyDescriptionService} below is such an implementation - it
 * implements every method the interface had previously and does not mention the
 * new one. That this file compiles is half the test; the assertions are the
 * other half.
 */
public class NamedSetLookupTest {

	private final DescriptionService legacy = new LegacyDescriptionService();

	@Test
	public void anImplementationThatDoesNotOverrideItAnswersFalse() {
		assertFalse(legacy.isInNamedSet("redundant-isa-exempt-modules", "32506021000036107"));
	}

	@Test
	public void theDefaultDoesNotDistinguishBetweenKeys() {
		// "No configuration present" has to be the answer for every key, not only
		// for keys nobody has defined, or a rule would behave differently
		// depending on which implementation it ran against.
		assertFalse(legacy.isInNamedSet("anything-at-all", "any-value"));
		assertFalse(legacy.isInNamedSet("", ""));
	}

	@Test
	public void theDefaultToleratesNulls() {
		assertFalse(legacy.isInNamedSet(null, "32506021000036107"));
		assertFalse(legacy.isInNamedSet("redundant-isa-exempt-modules", null));
		assertFalse(legacy.isInNamedSet(null, null));
	}

	/**
	 * Stands in for an implementation outside this project - the authoring
	 * platform's, in particular. Only the methods the interface required before
	 * this change are declared.
	 */
	private static final class LegacyDescriptionService implements DescriptionService {

		@Override
		public Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<Description> findActiveDescriptionByExactTerm(String exactTerm) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<Description> findInactiveDescriptionByExactTerm(String exactTerm) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<Description> findMatchingDescriptionInHierarchy(Concept concept, Description description) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLanguageSpecificErrorMessage(Description description) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getCaseSensitiveWordsErrorMessage(Description description) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> findParentsNotContainingSemanticTag(Concept concept, String termSematicTag, String... languageRefsetIds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isRecognisedSemanticTag(String termSemanticTag, String language) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isSemanticTagCompatibleWithinHierarchy(String term, Set<String> topLevelSemanticTags) {
			throw new UnsupportedOperationException();
		}
	}
}
