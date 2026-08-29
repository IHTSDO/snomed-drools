package org.ihtsdo.drools.service;

import java.util.Set;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;

public interface DescriptionService {

	Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds);

	Set<Description> findActiveDescriptionByExactTerm(String exactTerm);

	Set<Description> findInactiveDescriptionByExactTerm(String exactTerm);

	Set<Description> findMatchingDescriptionInHierarchy(Concept concept, Description description);
	
	String getLanguageSpecificErrorMessage(Description description);

	String getCaseSensitiveWordsErrorMessage(Description description);
	
	Set<String> findParentsNotContainingSemanticTag(Concept concept, String termSematicTag, String... languageRefsetIds);

	boolean isRecognisedSemanticTag(String termSemanticTag, String language);

	boolean isSemanticTagCompatibleWithinHierarchy(String term, Set<String> topLevelSemanticTags);

	/**
	 * Is {@code value} a member of the named set {@code setKey} in the test
	 * resources?
	 *
	 * <p>semantic-tag-hierarchies.txt is loaded as a generic
	 * {@code key=value,value,value} store, and
	 * {@link #isSemanticTagCompatibleWithinHierarchy} is currently the only way a
	 * rule can read it - which restricts it to values that are semantic tags
	 * extracted from a term. This exposes the same store for values that are not,
	 * so that a rule can be driven by per-edition configuration such as a set of
	 * module ids.
	 *
	 * <p>Defaulted rather than abstract so that existing implementations outside
	 * this project keep compiling. The default answers {@code false} for every
	 * key, which means "no configuration present" - a rule written against this
	 * must therefore behave, when it gets {@code false}, exactly as it did before
	 * the configuration existed.
	 *
	 * @param setKey the key on the left of the {@code =} in the resource file
	 * @param value  the value to look for among that key's members
	 */
	default boolean isInNamedSet(String setKey, String value) {
		return false;
	}

}
