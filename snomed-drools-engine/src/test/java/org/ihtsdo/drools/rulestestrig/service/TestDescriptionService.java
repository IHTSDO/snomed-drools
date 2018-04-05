package org.ihtsdo.drools.rulestestrig.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.helper.DescriptionHelper;
import org.ihtsdo.drools.service.DescriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestDescriptionService implements DescriptionService {

	final static Logger logger = LoggerFactory.getLogger(TestDescriptionService.class);

	private final Map<String, Concept> concepts;

	// Static block of sample case significant words - lower case word -> exact
	// word as supplied
	// NOTE: This will not handle cases where the same word exists with
	// different capitalizations
	// However, at this time no further precision is required
	public static final Set<String> caseSignificantWords = new HashSet<>();
	static {

		String fileName = "src/test/resources/data/CSWordsSample.txt";

		File file = new File(fileName);
		FileReader fileReader;
		BufferedReader bufferedReader;
		try {
			fileReader = new FileReader(file);
			bufferedReader = new BufferedReader(fileReader);
			String line;
			// skip header line
			bufferedReader.readLine();
			while ((line = bufferedReader.readLine()) != null) {
				String[] words = line.split("\\s+");

				// format: 0: word, 1: type (unused)
				caseSignificantWords.add(words[0]);
			}
			fileReader.close();
			logger.info("Loaded " + caseSignificantWords.size() + " case sensitive words into cache from: " + fileName);
		} catch (IOException e) {
			logger.debug("Failed to retrieve case sensitive words file: " + fileName);

		}

	}

	// Static block of sample case significant words
	// In non-dev environments, this should initialize on startup
	public static final Map<String, Set<String>> refsetToLanguageSpecificWordsMap = new HashMap<>();
	static {
		loadRefsetSpecificWords(Constants.GB_EN_LANG_REFSET, "src/test/resources/data/gbTerms.txt");
		loadRefsetSpecificWords(Constants.US_EN_LANG_REFSET, "src/test/resources/data/usTerms.txt");

	}

	private static void loadRefsetSpecificWords(String refsetId, String fileName) {

		Set<String> words = new HashSet<>();

		File file = new File(fileName);
		FileReader fileReader;
		BufferedReader bufferedReader;
		try {
			fileReader = new FileReader(file);
			bufferedReader = new BufferedReader(fileReader);
			String line;
			// skip header line
			bufferedReader.readLine();
			while ((line = bufferedReader.readLine()) != null) {
				words.add(line); // assumed to be single-word lines
			}
			fileReader.close();
			refsetToLanguageSpecificWordsMap.put(refsetId, words);
			logger.info("Loaded " + words.size() + " language-specific spellings into cache for refset " + refsetId);

		} catch (IOException e) {
			logger.debug("Failed to retrieve language-specific terms for refset " + refsetId + " in file " + fileName);
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
		checkMinSearchLength(exactTerm);

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
		checkMinSearchLength(exactTerm);

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
	/**
	 * This primitive implementation just uses the direct parents to group given concepts into a 'hierarchy'.
	 */
	public Set<Description> findMatchingDescriptionInHierarchy(Concept concept, Description description) {
		checkMinSearchLength(description.getTerm());

		Set<Description> matchingDescription = new HashSet<>();
		Set<String> parents = getParents(concept);
		for (Concept otherConcept : concepts.values()) {
			for (String otherConceptParent : getParents(otherConcept)) {
				if (parents.contains(otherConceptParent)) {
					for (Description otherDescription : otherConcept.getDescriptions()) {
						if (description.getTerm().equals(otherDescription.getTerm())) {
							matchingDescription.add(otherDescription);
						}
					}
				}
			}
		}

		return matchingDescription;
	}

	private Set<String> getParents(Concept concept) {
		final Set<String> parents = new HashSet<>();
		for (Relationship relationship : concept.getRelationships()) {
			if (Constants.IS_A.equals(relationship.getTypeId())) {
				parents.add(relationship.getDestinationId());
			}
		}
		return parents;
	}

	private void checkMinSearchLength(String exactTerm) {
		if (exactTerm.length() < 2) {
			throw new IllegalArgumentException("Term search requires at least two characters");
		}
	}

	@Override
	public String getCaseSensitiveWordsErrorMessage(Description description) {
		String result = "";

		// return immediately if description null
		if (description == null) {
			return result;
		}

		String[] words = description.getTerm().split("\\s+");

		for (String word : words) {

			// NOTE: Simple test to see if a case-sensitive term exists as
			// written. Original check for mis-capitalization, but false
			// positives, e.g. "oF" appears in list but spuriously reports "of"
			// Map preserved for lower-case matching in future
			if (caseSignificantWords.contains(word)
					&& !Constants.ENTIRE_TERM_CASE_SENSITIVE.equals(description.getCaseSignificanceId())) {
				result += "Description contains case-sensitive words but is not marked case sensitive: "
						+ caseSignificantWords.contains(word) + ".\n";

			}
		}

		return result;
	}

	@Override
	public String getLanguageSpecificErrorMessage(Description description) {

		String errorMessage = "";

		// null checks
		if (description == null || description.getAcceptabilityMap() == null || description.getTerm() == null) {
			return errorMessage;
		}

		String[] words = description.getTerm().split("\\s+");

		// convenience variables
		String usAcc = description.getAcceptabilityMap().get(Constants.US_EN_LANG_REFSET);
		String gbAcc = description.getAcceptabilityMap().get(Constants.GB_EN_LANG_REFSET);

		// NOTE: Supports international only at this point
		// Only check active synonyms
		if (description.isActive() && Constants.SYNONYM.equals(description.getTypeId())) {
			for (String word : words) {

				// Step 1: Check en-us preferred synonyms for en-gb spellings
				if (Constants.ACCEPTABILITY_PREFERRED.equals(usAcc) && refsetToLanguageSpecificWordsMap
						.get(Constants.GB_EN_LANG_REFSET).contains(word.toLowerCase())) {
					errorMessage += "Synonym is preferred in the en-us refset but refers to a word that has en-gb spelling: "
							+ word + "\n";
				}

				// Step 2: Check en-gb preferred synonyms for en-us spellings
				if (Constants.ACCEPTABILITY_PREFERRED.equals(gbAcc) && refsetToLanguageSpecificWordsMap
						.get(Constants.US_EN_LANG_REFSET).contains(word.toLowerCase())) {
					errorMessage += "Synonym is preferred in the en-gb refset but refers to a word that has en-us spelling: "
							+ word + "\n";
				}
			}
		}

		return errorMessage;
	}
	
	@Override
	public Set<String> findParentsNotContainSematicTag (Concept concept, String termSematicTag)
	{
		Set<String> conceptIds = new HashSet<String>();
		for (Relationship relationship : concept.getRelationships()) {
			if (Constants.IS_A.equals(relationship.getTypeId()) 
				&& relationship.isActive() 
				&& Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				Concept parent = concepts.get(relationship.getDestinationId());
				for (Description description : parent.getDescriptions()) {
					if (description.isActive() && Constants.FSN.equals(description.getTypeId())) {
						if(!termSematicTag.equals(DescriptionHelper.getTag(description.getTerm()))) {
							conceptIds.add(relationship.getDestinationId());
						}
					}
				}
			}
		}
		
		return conceptIds;
	}
}
