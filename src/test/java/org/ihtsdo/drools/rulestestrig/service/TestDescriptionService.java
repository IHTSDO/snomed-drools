package org.ihtsdo.drools.rulestestrig.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.service.DescriptionService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestDescriptionService implements DescriptionService {

	private final Map<String, Concept> concepts;

	// Static block of sample case significant words
	// In non-dev environments, this should initialize on startup
	public static final Set<String> caseSignificantWordsOriginal = new HashSet<>();
	public static final Set<String> caseSignificantWordsLowerCase = new HashSet<>();
	static {
		
		// add the original, case-sensitive terms to static set
		caseSignificantWordsOriginal.add("Greenfield");
		caseSignificantWordsOriginal.add("Nes");
		caseSignificantWordsOriginal.add("MCKD");
		caseSignificantWordsOriginal.add("Frazier");
		caseSignificantWordsOriginal.add("Plendil");
		caseSignificantWordsOriginal.add("Serevent");
		caseSignificantWordsOriginal.add("Zyflo");
		caseSignificantWordsOriginal.add("Invirase");
		caseSignificantWordsOriginal.add("Arimidex");
		caseSignificantWordsOriginal.add("Fomivirsen");
		caseSignificantWordsOriginal.add("CROM2");
		caseSignificantWordsOriginal.add("Dalteparin");
		
		// convert terms to lower case and store
		for (String word : caseSignificantWordsOriginal) {
			caseSignificantWordsLowerCase.add(word.toLowerCase());
		}
	}

	public TestDescriptionService(Map<String, Concept> concepts) {
		this.concepts = concepts;
	}

	@Override
	public Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds) {
		Set<String> fsns = new HashSet<>();
		for (String conceptId : conceptIds) {
			final Concept concept = concepts.get(conceptId);
			for (Description description : concept.getDescriptions()) {
				if (description.isActive() && Constants.FSN.equals(description.getTypeId())) {
					for (String languageRefsetId : languageRefsetIds) {
						Constants.ACCEPTABILITY_PREFERRED
								.equals(description.getAcceptabilityMap().get(languageRefsetId));
						fsns.add(description.getTerm());
					}
				}
			}
		}
		return fsns;
	}

	@Override
	public Set<Description> findActiveDescriptionByExactTerm(String exactTerm) {
		
		Set<Description> matches = new HashSet<>();
		for (Concept concept : concepts.values()) {
			for (Description description : concept.getDescriptions()) {
				if (description.isActive() && description.getTerm().equals(exactTerm)) {
					matches.add(description);
				}
			}
		}
		return matches;
	}

	@Override
	public Set<Description> findInactiveDescriptionByExactTerm(String exactTerm) {
		Set<Description> matches = new HashSet<>();
		for (Concept concept : concepts.values()) {
			for (Description description : concept.getDescriptions()) {
				if (!description.isActive() && description.getTerm().equals(exactTerm)) {
					matches.add(description);
				}
			}
		}
		return matches;
	}

	@Override
	public Set<Concept> findConceptsByActiveExactTerm(String exactTerm, boolean active) {
		Set<Concept> matches = new HashSet<>();
		for (Concept concept : concepts.values()) {
			for (Description description : concept.getDescriptions()) {
				if (description.isActive() == active && description.getTerm().equals(exactTerm)) {
					matches.add(concept);
				}
			}
		}
		return matches;
	}

	@Override
	public boolean hasCaseSignificantWord(String term) {
		String[] words = term.split("\\s+");
		for (String word : words) {
			// if lower case match and not original word match
			if (caseSignificantWordsLowerCase.contains(word.toLowerCase()) && !caseSignificantWordsOriginal.contains(word)) {
				return true;
			}
		}
		return false;
	}
}
