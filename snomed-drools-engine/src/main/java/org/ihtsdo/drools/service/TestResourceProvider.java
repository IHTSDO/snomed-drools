package org.ihtsdo.drools.service;

import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides test resources from disk or S3 depending on ResourceManager configuration.
 * Caches files for future use.
 */
public class TestResourceProvider {

	private static final String SEMANTIC_TAG_FILENAME_PREFIX = "semantic-tags";
	private static final String CASE_SIGNIFICANT_WORDS_FILENAME = "cs_words.txt";
	private static final String US_TO_GB_TERMS_MAP_FILENAME = "us-to-gb-terms-map.txt";

	private final ResourceManager resourceManager;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<String, Set<String>> semanticTagsMap;
	private final Set<String> caseSignificantWords;
	private final Map<String, String> usToGbTermMap;

	public TestResourceProvider(ResourceManager resourceManager) throws IOException {
		this.resourceManager = resourceManager;
		semanticTagsMap = loadSemanticTagsMap();
		caseSignificantWords = loadCaseSignificantWords();
		usToGbTermMap = doGetUsToGbTermMap();
	}

	public boolean isAnyResourcesLoaded() {
		return (semanticTagsMap != null && !semanticTagsMap.isEmpty())
				|| (caseSignificantWords != null && !caseSignificantWords.isEmpty())
				|| (usToGbTermMap != null && !usToGbTermMap.isEmpty());
	}

	/**
	 * Returns cached semantic tags list.
	 * @return Set of semantic tags.
	 */
	public Set<String> getSemanticTags() {
		Set<String> allTags = new HashSet<>();
		semanticTagsMap.values().forEach(value -> allTags.addAll(value));
		return  allTags;
	}

	/**
	 * Returns cached semantic tags list by languages.
	 * @return Set of semantic tags.
	 */
	public Set<String> getSemanticTagsByLanguage(Set<String> language) {
		Set<String> semanticTags = new HashSet<>();
		semanticTagsMap.entrySet().forEach(entry -> {
			if (language.contains(entry.getKey())) {
				semanticTags.addAll(entry.getValue());
			}
		});
		return  semanticTags;
	}

	/**
	 * Returns cached case significant words list.
	 * @return Set of significant words.
	 */
	public Set<String> getCaseSignificantWords() {
		return caseSignificantWords;
	}

	/**
	 * Returns cached US to GB terms map.
	 * @return US to GB terms map.
	 */
	public Map<String, String> getUsToGbTermMap() {
		return usToGbTermMap;
	}

	private Map<String, Set<String>> loadSemanticTagsMap() throws IOException {
		Map<String, Set<String>> tagsMap = new HashMap<>();
		Set<String> allTags = new HashSet<>();
		Set<String> filenames = resourceManager.listFilenames(SEMANTIC_TAG_FILENAME_PREFIX);
		for (String filename : filenames) {
			String language = filename.equalsIgnoreCase("semantic-tags.txt") ? "en" : filename.substring(filename.indexOf("_") + 1, filename.indexOf("."));
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceManager.readResourceStream(filename)))) {
				String line;
				Set<String> tags = new HashSet<>();
				while ((line = reader.readLine()) != null) {
					tags.add(line.trim());
				}
				if (!tags.isEmpty()) {
					tagsMap.put(language, tags);
				}
				allTags.addAll(tags);
			}
		}

		logger.info("{} semantic tags loaded", allTags.size());
		return tagsMap;
	}

	private Set<String> loadCaseSignificantWords() throws IOException {
		Set<String> terms = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceManager.readResourceStream(CASE_SIGNIFICANT_WORDS_FILENAME)))) {
			String line;
			reader.readLine();// Discard header line
			while ((line = reader.readLine()) != null) {
				if (line.endsWith("1")) {
					terms.add(line.trim());
				}
			}
		}
		logger.info("{} case significant words loaded", terms.size());
		return terms;
	}

	private Map<String, String> doGetUsToGbTermMap() throws IOException {
		Map<String, String> usToGbTermMap = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceManager.readResourceStream(US_TO_GB_TERMS_MAP_FILENAME)))) {
			String line;
			int lineNum = 0;
			reader.readLine();// Discard header line
			while ((line = reader.readLine()) != null) {
				lineNum++;
				if (!line.isEmpty()) {
					String[] split = line.split("\\t");
					if (split.length != 2) {
						logger.warn("Line {} in test resource file {} should contain 2 columns but contains {}", lineNum, US_TO_GB_TERMS_MAP_FILENAME, split.length);
					} else {
						usToGbTermMap.put(split[0].trim(), split[1].trim());
					}
				}
			}
		}
		logger.info("{} US to GB term map entries loaded", usToGbTermMap.size());
		return usToGbTermMap;
	}

}
