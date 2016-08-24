package org.ihtsdo.drools.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;

import java.util.Set;

public interface DescriptionService {

	Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds);

	Set<Description> findActiveDescriptionByExactTerm(String exactTerm);

	Set<Description> findInactiveDescriptionByExactTerm(String exactTerm);

	Set<Concept> findConceptsByActiveExactTerm(String exactTerm, boolean active);

	boolean hasCaseSignificantWord(String term);
	
}
