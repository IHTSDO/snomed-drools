package org.ihtsdo.drools.helper;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DescriptionHelper {

	public static final Pattern TAG_PATTERN = Pattern.compile("^.*\\((.*)\\)$");
	public static final Pattern FULL_TAG_PATTERN = Pattern.compile("^.*(\\s\\([^\\)]+\\))$");
	public static final Pattern FIRST_WORD_PATTERN = Pattern.compile("([^\\s]*).*$");

	private DescriptionHelper () {}
	
	public static Collection<Description> filterByActiveTypeAndDialectPreferred(Concept concept, boolean active,
			String typeId, String dialectPreferred) {
		Collection<Description> descriptions = new HashSet<>();
		for (Description description : concept.getDescriptions()) {
			if (description.isActive() == active && typeId.equals(description.getTypeId())
					&& Constants.ACCEPTABILITY_PREFERRED
							.equals(description.getAcceptabilityMap().get(dialectPreferred))) {
				descriptions.add(description);
			}
		}
		return descriptions;
	}

	public static boolean isMoreThanOneAcceptabilityPerDialect(Concept concept, boolean active, String typeId, String acceptability) {
		List<String> dialects = new ArrayList<String>();
		dialects.add(Constants.US_EN_LANG_REFSET);
		dialects.add(Constants.GB_EN_LANG_REFSET);
		
		for (Description description : concept.getDescriptions()) {
			if (description.isActive() == active && typeId.equals(description.getTypeId())) {
				for (String key : description.getAcceptabilityMap().keySet()) {
					if(!dialects.contains(key)) {
						dialects.add(key);
					}
				}				
			}
		}
		
		for(String dialect : dialects) {			
			List<Description> descriptions = concept.getDescriptions().stream()
					.filter(desc -> desc.isActive() == active
							&& typeId.equals(desc.getTypeId())
							&& acceptability.equals(desc.getAcceptabilityMap().get(dialect)))
					.collect(Collectors.toList());
			if(descriptions.size() > 1) {
				return true;
			}
		}
		
		return false;
	}
	
	public static Collection<Description> filterByActiveAndType(Concept concept, boolean active, String typeId) {
		Collection<Description> descriptions = new HashSet<>();
		for (Description description : concept.getDescriptions()) {
			if (description.isActive() == active && typeId.equals(description.getTypeId())) {
				descriptions.add(description);
			}
		}
		return descriptions;
	}

	/**
	 * Boolean check for whether description has defined acceptability map
	 * 
	 * @param description
	 *            the description
	 * @return true if acceptability map exists and has at least one dialect,
	 *         false otherwise
	 */
	public static boolean hasAcceptabilityMap(Description description) {
		return description.getAcceptabilityMap() != null && description.getAcceptabilityMap().size() > 0;
	}

	/**
	 * Boolean check for whether a definition has a specified acceptability
	 * value in any dialect
	 * 
	 * @param description
	 *            the description
	 * @param acceptabilityValue
	 *            the acceptability value
	 * @return true if acceptability map exists and value present
	 */
	public static boolean isAcceptabilityValuePresentOnDescription(Description description, String acceptabilityValue) {

		return (description.isActive() && description.getAcceptabilityMap() != null
				&& description.getAcceptabilityMap().containsValue(acceptabilityValue));
	}

	public static boolean isUniqueWithinConcept(Description description, Concept concept) {
		final String term = description.getTerm();
		for (Description otherDescription : concept.getDescriptions()) {
			if (!description.getId().equals(otherDescription.getId()) && term.equals(otherDescription.getTerm())) {
				return false;
			}
		}
		return true;
	}

	public static boolean isPreferredInAnyDialect(Description description) {
		final Map<String, String> acceptabilityMap = description.getAcceptabilityMap();
		for (String lang : acceptabilityMap.keySet()) {
			if (lang != null && Constants.ACCEPTABILITY_PREFERRED.equals(acceptabilityMap.get(lang))) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSemanticTagEquivalentToAnother(String testTerm, Set<String> otherTerms) {
		return isSemanticTagEquivalentToAnother(testTerm, otherTerms, null);
	}

	public static boolean isAllParentSemanticTagMatchWithTerm(String testTerm, List<String> otherTerms) {
		String tag = getTag(testTerm);		
		if (tag != null) {
			for (String otherTerm : otherTerms) {
				String parentTag = getTag(otherTerm);
				if (!tag.equals(parentTag)) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public static boolean isSemanticTagEquivalentToAnother(String testTerm, Set<String> otherTerms,
														   String[][] acceptablePairs) {
		String tag = getTag(testTerm);
		if (tag != null) {
			for (String otherTerm : otherTerms) {
				String parentTag = getTag(otherTerm);
				if (tag.equals(parentTag)) {
					return true;
				}
				if (acceptablePairs != null) {
					for (String[] acceptablePair : acceptablePairs) {
						if (acceptablePair.length == 2
								&& (acceptablePair[0].equals("*") || tag.equals(acceptablePair[0]))
								&& acceptablePair[1].equals(parentTag)) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	/**
	 * Boolean check for case significance match between preferred term and one
	 * or more fsns
	 * 
	 * @param c
	 * @param d
	 * @return
	 */
	public static boolean isPreferredTermCaseSignificanceValid(Concept c, Description d) {
		// cycle over dialects for preferred values
		for (String dialect : d.getAcceptabilityMap().keySet()) {
			if (Constants.ACCEPTABILITY_PREFERRED.equals(d.getAcceptabilityMap().get(dialect))) {
				for (Description desc : c.getDescriptions()) {
					// if fsn in this dialect does not match case significance,
					// return false
					if (Constants.FSN.equals(desc.getTypeId())
							&& Constants.ACCEPTABILITY_PREFERRED.equals(desc.getAcceptabilityMap().get(dialect))) {
						if (!desc.getCaseSignificanceId().equals(d.getCaseSignificanceId())) {
							return false;
						}
					}
				}
			}
		}

		// return true if no fsn found or no mismatch
		return true;
	}

	/**
	 * Check if case significance is consistent between all term pairs
	 * 
	 * @param concept
	 *            the concept
	 * @return true if all pairs are valid, false if not
	 */
	@Deprecated
	public static boolean isCaseSignificanceValidBetweenTerms(Concept concept, Description description) {

		String fw1 = getFirstWord(description.getTerm());
		for (Description d : concept.getDescriptions()) {
			String fw2 = getFirstWord(d.getTerm());

			// if first words are equal and case significance not equal, return
			// false - exclude text definitions
			if (fw1 != null && fw2 != null && fw1.toLowerCase().equals(fw2.toLowerCase())
					&& !Constants.TEXT_DEFINITION.equals(d.getTypeId())
					&& !Constants.TEXT_DEFINITION.equals(description.getTypeId()) && d.getCaseSignificanceId() != null
					&& !d.getCaseSignificanceId().equals(description.getCaseSignificanceId())) {
				return false;
			}
		}

		return true;
	}

	public static String getTag(String term) {
		final Matcher matcher = TAG_PATTERN.matcher(term);
		if (matcher.matches()) {
			String result = matcher.group(1);
			if(result != null && (result.contains("(") || result.contains(")"))) {
				return null;
			}
			return result;
		}
		return null;
	}
	
	public static Set<String> getTags(List<String> terms) {
		Set<String> tags = new HashSet<>();
		for (String term : terms) {
			String semanticTag = getTag(term);
			if (semanticTag != null) {
				tags.add(semanticTag);
			}
		}
		return tags;
	}

	public static String getTagForConcept(Concept concept) {
		for (Description d : concept.getDescriptions()) {
			if (Constants.FSN.equals(d.getTypeId())) {
				return getTag(d.getTerm());
			}
		}
		return null;
	}

	public static String getFsnTerm(String term) {
		final Matcher matcher = FULL_TAG_PATTERN.matcher(term);
		if (matcher.matches()) {
			return term.replace(matcher.group(1), "");
		}
		return null;

	}

	@Deprecated
	/**
	 * TestResourceProvider should now be used to load the semantic tags into the DescriptionService implementation.
	 */
	public boolean hasSemanticTag(Description description) {
		return true;
	}

	public static String getFirstWord(String term) {
		final Matcher matcher = FIRST_WORD_PATTERN.matcher(term);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return "";
	}

	public static boolean hasMatchingDescriptionByTypeTermLanguage(Concept concept, Description description) {
		for (Description d : concept.getDescriptions()) {
			if (description.getTypeId().equals(d.getTypeId()) && description.getTerm().equals(d.getTerm())
					&& description.getLanguageCode() != null && description.getLanguageCode().equals(d.getLanguageCode())) {
				return true;
			}
		}
		return false;
	}

	public static String getCaseSensitiveWordsErrorMessage(Description description, Set<String> caseSignificantWords) {
		StringBuilder result = new StringBuilder();

		// return immediately if description null
		if (description == null) {
			return result.toString();
		}

		String[] words = description.getTerm().split("\\s+");

		for (String word : words) {

			// NOTE: Simple test to see if a case-sensitive term exists as
			// written. Original check for mis-capitalization, but false
			// positives, e.g. "oF" appears in list but spuriously reports "of"
			// Map preserved for lower-case matching in future
			if (caseSignificantWords.contains(word) && !Constants.ENTIRE_TERM_CASE_SENSITIVE.equals(description.getCaseSignificanceId())) {
				result.append("Description contains case-sensitive words but is not marked case sensitive: ")
						.append(caseSignificantWords.contains(word)).append(".\n");
			}
		}

		return result.toString();
	}

	public static String getLanguageSpecificErrorMessage(Description description, Map<String, String> usToGbTermMap) {
		StringBuilder errorMessage = new StringBuilder();

		// null checks
		if (description == null || description.getAcceptabilityMap() == null || description.getTerm() == null) {
			return errorMessage.toString();
		}

		// Only check active synonyms
		if (!description.getTypeId().equals(Constants.SYNONYM) || !description.isActive()) {
			return errorMessage.toString();
		}

		String[] words = description.getTerm().split("\\s+");

		String usAcc = description.getAcceptabilityMap().get(Constants.US_EN_LANG_REFSET);
		String gbAcc = description.getAcceptabilityMap().get(Constants.GB_EN_LANG_REFSET);

		// NOTE: Supports international only at this point
		for (String word : words) {

			// Step 1: Check en-us preferred synonyms for en-gb spellings
			if (usAcc != null && usToGbTermMap.containsValue(word.toLowerCase())) {
				errorMessage.append("Synonym is preferred in the en-us refset but refers to a word that has en-gb spelling: ").append(word).append("\n");
			}

			// Step 2: Check en-gb preferred synonyms for en-us spellings
			if (gbAcc != null && usToGbTermMap.containsKey(word.toLowerCase())) {
				errorMessage.append("Synonym is preferred in the en-gb refset but refers to a word that has en-us spelling: ").append(word).append("\n");
			}
		}
		return errorMessage.toString();
	}

}
