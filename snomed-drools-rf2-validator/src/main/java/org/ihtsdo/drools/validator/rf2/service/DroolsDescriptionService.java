package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.helper.DescriptionHelper;
import org.ihtsdo.drools.service.DescriptionService;
import org.ihtsdo.drools.service.TestResourceProvider;
import org.ihtsdo.drools.validator.rf2.DroolsDescriptionIndex;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class DroolsDescriptionService implements DescriptionService {

	private static final String FULLY_SPECIFIED_NAME = "900000000000003001";
	private static final String PREFERRED_ACCEPTABILITY = "900000000000548007";
	private final SnomedDroolsComponentRepository repository;
	private final DroolsDescriptionIndex droolsDescriptionIndex;
	private final TestResourceProvider testResourceProvider;
	private final DroolsConceptService conceptService;

	public DroolsDescriptionService(SnomedDroolsComponentRepository repository, DroolsConceptService conceptService, TestResourceProvider testResourceProvider) {
		this.repository = repository;
		this.droolsDescriptionIndex = new DroolsDescriptionIndex(repository);
		this.testResourceProvider = testResourceProvider;
		this.conceptService = conceptService;
	}


	@Override
	public Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds) {
		Set<String> fsns = new HashSet<>();
		for (String conceptId : conceptIds) {
			findFSNFromConcept(languageRefsetIds, conceptId, fsns);
		}
		return fsns;
	}

	private void findFSNFromConcept(String[] languageRefsetIds, String conceptId, Set<String> fsns) {
		DroolsConcept concept = repository.getConcept(conceptId);
		if(concept != null) {
			Collection<DroolsDescription> descriptions = concept.getDescriptions();
			for (DroolsDescription description : descriptions) {
				findFSNFromDescription(languageRefsetIds, fsns, description);
			}
		}
	}

	private void findFSNFromDescription(String[] languageRefsetIds, Set<String> fsns, DroolsDescription description) {
		if (description.isActive() && description.getTypeId().equals(FULLY_SPECIFIED_NAME)) {
			if(languageRefsetIds != null && languageRefsetIds.length > 0) {
				for (String languageRefsetId : languageRefsetIds) {
					if (PREFERRED_ACCEPTABILITY.equals(description.getAcceptabilityMap().get(languageRefsetId))) {
						fsns.add(description.getTerm());
					}
				}
			} else {
				fsns.add(description.getTerm());
			}
		}
	}

	@Override
	public Set<Description> findActiveDescriptionByExactTerm(String exactTerm) {
		if(exactTerm == null || exactTerm.trim().isEmpty()) return Collections.emptySet();
		Set<String> idSet = droolsDescriptionIndex.findMatchedDescriptionTerm(exactTerm,true);
		Set<Description> descriptions = new HashSet<>();
		for (String id : idSet) {
			DroolsDescription droolsDescription = repository.getDescription(id);
			descriptions.add(droolsDescription);
		}
		return descriptions;
	}

	@Override
	public Set<Description> findInactiveDescriptionByExactTerm(String exactTerm) {
		if(exactTerm == null || exactTerm.trim().isEmpty()) return Collections.emptySet();
		Set<String> idSet = droolsDescriptionIndex.findMatchedDescriptionTerm(exactTerm,false);
		Set<Description> descriptions = new HashSet<>();
		for (String id : idSet) {
			DroolsDescription droolsDescription = repository.getDescription(id);
			descriptions.add(droolsDescription);
		}
		return descriptions;
	}

	@Override
	public Set<Description> findMatchingDescriptionInHierarchy(Concept concept, Description description) {
		if(concept == null || concept.getId().equals(Constants.ROOT_CONCEPT)) {
			return Collections.emptySet();
		}
		String languageCode = description.getLanguageCode();
		String term = description.getTerm();
		if(term == null || term.trim().isEmpty()) return Collections.emptySet();

		Set<org.ihtsdo.drools.domain.Description> matchingDescriptions = findActiveDescriptionByExactTerm(description.getTerm())
				.stream().filter(d -> d.getLanguageCode().equals(languageCode)).collect(Collectors.toSet());

		if (!matchingDescriptions.isEmpty()) {
			// Filter matching descriptions by hierarchy

			// Find root for this concept
			Set<String> conceptHierarchyRootIds = conceptService.findTopLevelHierarchiesOfConcept(concept);
			if (conceptHierarchyRootIds != null) {
				return matchingDescriptions.stream().filter(d -> {
					Set<String> statedAncestors = conceptService.findStatedAncestorsOfConcepts(Collections.singletonList(d.getConceptId()));
					return statedAncestors.stream().anyMatch(conceptHierarchyRootIds::contains);
				}).collect(Collectors.toSet());
			}
		}

		return Collections.emptySet();
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
	public Set<String> findParentsNotContainingSemanticTag(Concept concept, String termSematicTag, String... languageRefsetIds) {
		Set<String> conceptIds = new HashSet<>();
		for (Relationship relationship : concept.getRelationships()) {
			if (validIsaStatedRelationship(relationship)) {
				Concept parent = repository.getConcept(relationship.getDestinationId());
				parent.getDescriptions().forEach(description -> {
					if (description.isActive() && Constants.FSN.equals(description.getTypeId())) {
						boolean matchedAcceptability = isMatchedAcceptability(languageRefsetIds, description);

						if((languageRefsetIds == null || matchedAcceptability) && !termSematicTag.equals(DescriptionHelper.getTag(description.getTerm()))) {
							conceptIds.add(relationship.getDestinationId());
						}
					}
				});
			}
		}
		return conceptIds;
	}

	private boolean validIsaStatedRelationship(Relationship relationship) {
		return relationship.isActive()
				&& Constants.IS_A.equals(relationship.getTypeId())
				&& !relationship.isAxiomGCI()
				&& Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId());
	}

	private boolean isMatchedAcceptability(String[] languageRefsetIds, Description description) {
		boolean matchedAcceptability = false;
		if(languageRefsetIds != null) {
			for (String languageRefsetId : languageRefsetIds) {
				if (PREFERRED_ACCEPTABILITY.equals(description.getAcceptabilityMap().get(languageRefsetId))) {
					matchedAcceptability = true;
					break;
				}
			}
		}
		return matchedAcceptability;
	}

	@Override
	public boolean isRecognisedSemanticTag(String termSemanticTag, String language) {
		return testResourceProvider.getSemanticTagsByLanguage(Collections.singleton(language)).contains(termSemanticTag);
	}

	@Override
	public boolean isSemanticTagCompatibleWithinHierarchy(String testTerm, Set<String> topLevelSemanticTags) {
		String tag = getTag(testTerm);
		Map <String, Set <String>> semanticTagMap = testResourceProvider.getSemanticHierarchyMap();
		if (tag != null) {
			for (String topLevelSemanticTag : topLevelSemanticTags) {
				Set<String> compatibleSemanticTags = semanticTagMap.get(topLevelSemanticTag);
				if (!CollectionUtils.isEmpty(compatibleSemanticTags) && compatibleSemanticTags.contains(tag)) {
					return true;
				}
			}
		}

		return false;
	}

	private static String getTag(String term) {
		final Matcher matcher = DescriptionHelper.TAG_PATTERN.matcher(term);
		if (matcher.matches()) {
			String result = matcher.group(1);
			if(result != null && (result.contains("(") || result.contains(")"))) {
				return null;
			}
			return result;
		}
		return null;
	}

	public DroolsDescriptionIndex getDroolsDescriptionIndex() {
		return droolsDescriptionIndex;
	}
}
