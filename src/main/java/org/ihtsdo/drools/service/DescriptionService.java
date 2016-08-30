package org.ihtsdo.drools.service;

import java.util.Set;

import org.ihtsdo.drools.domain.Description;

public interface DescriptionService {

	Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds);

	Set<Description> findActiveDescriptionByExactTerm(String exactTerm);

	Set<Description> findInactiveDescriptionByExactTerm(String exactTerm);

	boolean isActiveDescriptionUniqueWithinHierarchy(Description description, String semanticTag);
	
	String getLanguageSpecificErrorMessage(Description description);

	String getCaseSensitiveWordsFromTerm(String term);
	
}
