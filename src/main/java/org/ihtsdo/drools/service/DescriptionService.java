package org.ihtsdo.drools.service;

import java.util.Set;

import org.ihtsdo.drools.domain.Description;

public interface DescriptionService {

	Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds);

	Set<Description> findActiveDescriptionByExactTerm(String exactTerm);

	Set<Description> findInactiveDescriptionByExactTerm(String exactTerm);

	boolean isTermUniqueWithinHierarchy(String exactTerm, String semanticTag, boolean isActive);
	
	String getLanguageSpecificErrorMessage(Description description);

	boolean hasCaseSignificantWord(String term);
	
}
