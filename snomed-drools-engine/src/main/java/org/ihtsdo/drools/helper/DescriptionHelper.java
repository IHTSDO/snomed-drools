package org.ihtsdo.drools.helper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.slf4j.LoggerFactory;

public class DescriptionHelper {

	public static final Pattern TAG_PATTERN = Pattern.compile("^.*\\((.*)\\)$");
	public static final Pattern FULL_TAG_PATTERN = Pattern.compile("^.*(\\s\\([^\\)]+\\))$");
	public static final Pattern FIRST_WORD_PATTERN = Pattern.compile("([^\\s]*).*$");

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
			return matcher.group(1);
		}
		return null;
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
	
	public static boolean hasSemanticTag(Description description) {
		return description.getTerm() != null && Constants.SEMANTIC_TAGS.contains(getTag(description.getTerm().toLowerCase()));
	}

	private static String getFirstWord(String term) {
		final Matcher matcher = FIRST_WORD_PATTERN.matcher(term);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
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

}