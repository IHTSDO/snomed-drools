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
	public static final Set<String> caseSignificantWordsOriginal = new HashSet<>();
	public static final Set<String> caseSignificantWordsLowerCase = new HashSet<>();
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
				caseSignificantWordsOriginal.add(words[0]);
				caseSignificantWordsLowerCase.add(words[0].toLowerCase());
			}
			fileReader.close();
			logger.info("Loaded " + caseSignificantWordsOriginal.size() + " case sensitive words into cache");
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
	public boolean isTermUniqueWithinHierarchy(String exactTerm, String semanticTag, boolean isActive) {
		
		for (Concept concept : concepts.values()) {
			String conceptTag = null;
			// find the FSN
			for (Description description : concept.getDescriptions()) {
				if (description.isActive() == isActive && description.getTypeId().equals(Constants.FSN)){
					final Matcher matcher = Pattern.compile("^.*\\((.*)\\)$").matcher(description.getTerm());
					if (matcher.matches()) {
						conceptTag = matcher.group(1);
					}
				}
			}
			
			
			for (Description description : concept.getDescriptions()) {
				if (description.isActive() == isActive && description.getTerm().equals(exactTerm) && semanticTag.equals(conceptTag)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean hasCaseSignificantWord(String term) {
		String[] words = term.split("\\s+");
		for (String word : words) {
			// if lower case match and not original word match
			if (caseSignificantWordsLowerCase.contains(word.toLowerCase())
					&& !caseSignificantWordsOriginal.contains(word)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getLanguageSpecificErrorMessage(Description description) {
		String[] words = description.getTerm().split("\\s+");
		for (String refsetId : description.getAcceptabilityMap().keySet()) {
			for (String word : words) {
			if (refsetToLanguageSpecificWordsMap.get(refsetId).contains(word)) {
				return "Synonym is prefered in the GB Language refset but refers to a word has en-us spelling: " + word;
			}
			}
		}
		return null;
	}
}
