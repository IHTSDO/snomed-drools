package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.helper.DescriptionHelper;
import org.ihtsdo.drools.service.ConceptService;
import org.ihtsdo.drools.service.DescriptionService;
import org.ihtsdo.drools.service.TestResourceProvider;
import org.ihtsdo.drools.validator.rf2.DroolsDescriptionIndex;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;

public class DroolsDescriptionService implements DescriptionService {

	private static final String FULLY_SPECIFIED_NAME = "900000000000003001";
	private static final String PREFERRED_ACCEPTABILITY = "900000000000548007";
	private final SnomedDroolsComponentRepository repository;
	private final DroolsDescriptionIndex droolsDescriptionIndex;
	private final TestResourceProvider testResourceProvider;

	public DroolsDescriptionService(SnomedDroolsComponentRepository repository, TestResourceProvider testResourceProvider) {
		this.repository = repository;
		this.droolsDescriptionIndex = new DroolsDescriptionIndex(repository);
		this.testResourceProvider = testResourceProvider;
	}


	@Override
	public Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds) {
		Set<String> fsns = new HashSet<>();
		for (String conceptId : conceptIds) {
			DroolsConcept concept = repository.getConcept(conceptId);
			if(concept != null) {
				Collection<DroolsDescription> descriptions = concept.getDescriptions();
				for (DroolsDescription description : descriptions) {
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
			}
		}
		return fsns;
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
	// FIXME: Currently only finds matching description in ancestors.
	// Should search all descendants of the second highest ancestor (the ancestor which is a direct child of root).
	public Set<Description> findMatchingDescriptionInHierarchy(Concept concept, Description description) {
		if(concept == null || concept.getId().equals(Constants.ROOT_CONCEPT)) {
			return Collections.emptySet();
		}
		Set<Description> resultSet = new HashSet<>();

		String languageCode = description.getLanguageCode();
		String term = description.getTerm();
		if(term == null || term.trim().isEmpty()) return Collections.emptySet();
		
		ConceptService conceptService = new DroolsConceptService(repository);
		Set<String> conceptAncestorIds = conceptService.findStatedAncestorsOfConcept(concept);
		for (String conceptAncestorId : conceptAncestorIds) {
			Concept conceptAncestor = repository.getConcept(conceptAncestorId);
			for (Description ancestorsDescription : conceptAncestor.getDescriptions()) {
				if(ancestorsDescription.isActive() && ancestorsDescription.getLanguageCode().equals(languageCode)
						&& ancestorsDescription.getTerm().equals(term)) {
					resultSet.add(ancestorsDescription);
				}
			}
		}
		return resultSet;
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
			if (relationship.isActive()
					&& Constants.IS_A.equals(relationship.getTypeId())
					&& !relationship.isAxiomGCI()
					&& Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				Concept parent = repository.getConcept(relationship.getDestinationId());
				for (Description description : parent.getDescriptions()) {
					boolean matchedAcceptability = false;
					if (description.isActive() && Constants.FSN.equals(description.getTypeId())) {
						if(languageRefsetIds != null && languageRefsetIds.length > 0) {
							for (String languageRefsetId : languageRefsetIds) {
								if (PREFERRED_ACCEPTABILITY.equals(description.getAcceptabilityMap().get(languageRefsetId))) {
									matchedAcceptability = true;
									break;
								}
							}
						}

						if((languageRefsetIds == null || matchedAcceptability) && !termSematicTag.equals(DescriptionHelper.getTag(description.getTerm()))) {
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
