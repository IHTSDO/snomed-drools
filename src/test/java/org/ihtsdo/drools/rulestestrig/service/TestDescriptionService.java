package org.ihtsdo.drools.rulestestrig.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.service.DescriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDescriptionService implements DescriptionService {

	final static Logger logger = LoggerFactory.getLogger(TestDescriptionService.class);

	private final Map<String, Concept> concepts;

	// Static block of sample case significant words
	// In non-dev environments, this should initialize on startup
	public static final Map<String, String> caseSignificantWordsMap = new HashMap<>();
	static {

		File file = new File("src/test/resources/data/CSWordsSample.txt");
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
				caseSignificantWordsMap.put(words[0].toLowerCase(), words[0]);
			}
			fileReader.close();
			logger.info("Loaded " + caseSignificantWordsMap.size() + " case sensitive words into cache");
		} catch (IOException e) {
			logger.debug("Failed to retrieve case significant words file -- tests will be skipped");

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
	public boolean isActiveDescriptionUniqueWithinHierarchy(Description description, String semanticTag) {
		for (Concept concept : concepts.values()) {
			if (concept.isActive()) {

				String conceptTag = null;
				// find the FSN
				for (Description d : concept.getDescriptions()) {
					if (d.isActive() && d.getTypeId().equals(Constants.FSN)) {
						final Matcher matcher = Pattern.compile("^.*\\((.*)\\)$").matcher(d.getTerm());
						if (matcher.matches()) {
							conceptTag = matcher.group(1);
						}
					}
				}

				for (Description d : concept.getDescriptions()) {
					if (d.isActive() && !d.getId().equals(description.getId())
							&& d.getTerm().equals(description.getTerm())
							&& d.getLanguageCode().equals(description.getLanguageCode())
							&& semanticTag.equals(conceptTag)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public String getCaseSensitiveWordsErrorMessage(Description description) {
		String result = "";

		// return immediately if description null
		if (description == null) {
			return result;
		}

		System.out
				.println("Checking description " + description.getTerm() + ", " + description.getCaseSignificanceId());

		String[] words = description.getTerm().split("\\s+");

		for (String word : words) {

			System.out.println("  Checking word " + word);

			if (caseSignificantWordsMap.containsKey(word.toLowerCase())) {

				// Check 1: term containing case-sensitive words should not be
				// entire-term-case-insensitive
				if (Constants.ENTIRE_TERM_CASE_INSENSITIVE.equals(description.getCaseSignificanceId())) {
					result += "Description marked case insensitive should not contain case-sensitive word: "
							+ caseSignificantWordsMap.get(word.toLowerCase()) + ".\n";
				}

				// Check 2: term marked ONLY_INITIAL_CHARACTER_CASE_INSENSITIVE
				// should not start with case sensitive word
				// TODO Confirm this
				else if (Constants.ONLY_INITIAL_CHARACTER_CASE_INSENSITIVE.equals(description.getCaseSignificanceId())
						&& description.getTerm().startsWith(word)) {
					result += "Description marked only initial character case insensitive should not start with a case-sensitive word: " + word + ".\n";
				}
				// Check 3: term containing case-sensitive word with invalid
				// case
				else if (caseSignificantWordsMap.containsKey(word.toLowerCase())
						&& !caseSignificantWordsMap.get(word.toLowerCase()).equals(word)) {
					result += "Description contains case-sensitive word with improper case: " + word + " should be "
							+ caseSignificantWordsMap.get(word.toLowerCase()) + ".\n";
				}
			}
		}
		if (result.length() > 0) {
			System.out.println(result);
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

				// Step 2: Check en-gb preferred synonyms for en-en spellings
				if (Constants.ACCEPTABILITY_PREFERRED.equals(gbAcc) && refsetToLanguageSpecificWordsMap
						.get(Constants.US_EN_LANG_REFSET).contains(word.toLowerCase())) {
					errorMessage += "Synonym is preferred in the en-gb refset but refers to a word that has en-us spelling: "
							+ word + "\n";
				}
			}
		}

		return errorMessage;
	}
}
