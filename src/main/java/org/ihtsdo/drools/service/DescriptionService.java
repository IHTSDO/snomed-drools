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
	
}
