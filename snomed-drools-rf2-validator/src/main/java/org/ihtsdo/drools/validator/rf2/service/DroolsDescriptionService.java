package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.helper.DescriptionHelper;
import org.ihtsdo.drools.service.ConceptService;
import org.ihtsdo.drools.service.DescriptionService;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DroolsDescriptionService implements DescriptionService {

	private static final String FULLY_SPECIFIED_NAME = "900000000000003001";
	private static final String PREFERRED_ACCEPTABILITY = "900000000000548007";
	private final SnomedDroolsComponentRepository repository;

	public DroolsDescriptionService(SnomedDroolsComponentRepository repository) {
		this.repository = repository;
	}

	@Override
	public Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds) {
		Set<String> fsns = new HashSet<>();
		for (String conceptId : conceptIds) {
			Collection<DroolsDescription> descriptions = repository.getConcept(conceptId).getDescriptions();
			for (DroolsDescription description : descriptions) {
				if (description.isActive() && description.getTypeId().equals(FULLY_SPECIFIED_NAME)) {
					for (String languageRefsetId : languageRefsetIds) {
						PREFERRED_ACCEPTABILITY.equals(description.getAcceptabilityMap().get(languageRefsetId));
						fsns.add(description.getTerm());
					}
				}
			}
		}
		return fsns;
	}

	@Override
	public Set<Description> findActiveDescriptionByExactTerm(String exactTerm) {
		if(exactTerm == null || exactTerm.trim().isEmpty()) return Collections.emptySet();
		return repository.getDescriptions().parallelStream().
				filter(description -> description.isActive() && exactTerm.equals(description.getTerm()))
				.collect(Collectors.toSet());
	}

	@Override
	public Set<Description> findInactiveDescriptionByExactTerm(String exactTerm) {
		if(exactTerm == null || exactTerm.trim().isEmpty()) return Collections.emptySet();
		return repository.getDescriptions().parallelStream().
				filter(description -> !description.isActive() && exactTerm.equals(description.getTerm()))
				.collect(Collectors.toSet());
	}

	@Override
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
	public String getLanguageSpecificErrorMessage(Description description) {
		// TODO: Add support for this. See TestDescriptionService. Maps to be loaded from external resources.
		return null;
	}

	@Override
	public String getCaseSensitiveWordsErrorMessage(Description description) {
		// TODO: Add support for this. See TestDescriptionService. Case significant words list to be loaded from external resources.
		return "";
	}

	@Override
	public Set<String> findParentsNotContainSematicTag(Concept concept, String termSematicTag, String... languageRefsetIds) {
		Set<String> conceptIds = new HashSet<String>();
		for (Relationship relationship : concept.getRelationships()) {
			if (relationship.isActive()
					&& Constants.IS_A.equals(relationship.getTypeId())
					&& Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				Concept parent = repository.getConcept(relationship.getDestinationId());
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
