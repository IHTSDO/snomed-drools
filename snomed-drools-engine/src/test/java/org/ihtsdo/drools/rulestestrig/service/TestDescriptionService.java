package org.ihtsdo.drools.rulestestrig.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.helper.DescriptionHelper;
import org.ihtsdo.drools.service.DescriptionService;
import org.ihtsdo.drools.service.TestResourceProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestDescriptionService implements DescriptionService {

	private final Map<String, Concept> concepts;
	private final TestResourceProvider testResourceProvider;

	public TestDescriptionService(Map<String, Concept> concepts, TestResourceProvider testResourceProvider) {
		this.concepts = concepts;
		this.testResourceProvider = testResourceProvider;
	}

	@Override
	public Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds) {
		Set<String> fsns = new HashSet<>();
		for (String conceptId : conceptIds) {
			final Concept concept = concepts.get(conceptId);
			for (Description description : concept.getDescriptions()) {
				if (description.isActive() && Constants.FSN.equals(description.getTypeId())) {
					for (String languageRefsetId : languageRefsetIds) {
						if (Constants.ACCEPTABILITY_PREFERRED.equals(description.getAcceptabilityMap().get(languageRefsetId))) {
							fsns.add(description.getTerm());
						}
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
	/*
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
		return DescriptionHelper.getCaseSensitiveWordsErrorMessage(description, testResourceProvider.getCaseSignificantWords());
	}

	@Override
	public String getLanguageSpecificErrorMessage(Description description) {
		return DescriptionHelper.getLanguageSpecificErrorMessage(description, testResourceProvider.getUsToGbTermMap());
	}

	@Override
	public Set<String> findParentsNotContainingSemanticTag(Concept concept, String termSemanticTag, String... languageRefsetIds) {
		Set<String> conceptIds = new HashSet<>();
		for (Relationship relationship : concept.getRelationships()) {
			if (Constants.IS_A.equals(relationship.getTypeId()) 
				&& relationship.isActive() 
				&& !relationship.isAxiomGCI()
				&& Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				Concept parent = concepts.get(relationship.getDestinationId());
				for (Description description : parent.getDescriptions()) {
					if (description.isActive() && Constants.FSN.equals(description.getTypeId())) {
						if(!termSemanticTag.equals(DescriptionHelper.getTag(description.getTerm()))) {
							conceptIds.add(relationship.getDestinationId());
						}
					}
				}
			}
		}
		
		return conceptIds;
	}

	@Override
	public boolean isRecognisedSemanticTag(String termSemanticTag, String language) {
		return testResourceProvider.getSemanticTagsByLanguage(Collections.singleton(language)).contains(termSemanticTag);
	}
}
