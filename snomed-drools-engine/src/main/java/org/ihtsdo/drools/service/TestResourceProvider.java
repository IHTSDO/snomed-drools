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

	private static final String SEMANTIC_TAG_FILENAME = "semantic-tags.txt";
	private static final String CASE_SIGNIFICANT_WORDS_FILENAME = "cs_words.txt";
	private static final String US_TO_GB_TERMS_MAP_FILENAME = "us-to-gb-terms-map.txt";

	private final ResourceManager resourceManager;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Set<String> semanticTags;
	private Set<String> caseSignificantWords;
	private Map<String, String> usToGbTermMap;

	public TestResourceProvider(ResourceManager resourceManager) throws IOException {
		this.resourceManager = resourceManager;
		semanticTags = getStringsFromFile(SEMANTIC_TAG_FILENAME);
		caseSignificantWords = getStringsFromFile(CASE_SIGNIFICANT_WORDS_FILENAME);
		usToGbTermMap = doGetUsToGbTermMap();
	}

	public boolean isAnyResourcesLoaded() {
		return (semanticTags != null && !semanticTags.isEmpty())
				|| (caseSignificantWords != null && !caseSignificantWords.isEmpty())
				|| (usToGbTermMap != null && !usToGbTermMap.isEmpty());
	}

	/**
	 * Returns cached semantic tags list.
	 * @return Set of semantic tags.
	 */
	public Set<String> getSemanticTags() {
		return semanticTags;
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
		return usToGbTermMap;
	}

	private Set<String> getStringsFromFile(String filename) throws IOException {
		Set<String> terms = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceManager.readResourceStream(filename)))) {
			String line;
			while ((line = reader.readLine()) != null) {
				terms.add(line.trim());
			}
		}
		return terms;
	}
}
