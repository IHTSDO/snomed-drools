package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.service.DescriptionService;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
		// TODO: Add support for this - maybe using a Lucene index?
		return Collections.emptySet();
	}

	@Override
	public Set<Description> findInactiveDescriptionByExactTerm(String exactTerm) {
		// TODO: Add support for this - maybe using a Lucene index?
		return Collections.emptySet();
	}

	@Override
	public Set<Description> findMatchingDescriptionInHierarchy(Concept concept, Description description) {
		// TODO: Add support for this - maybe using a Lucene index?
		return Collections.emptySet();
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
	public Set<String> findParentsNotContainSematicTag(Concept concept, String termSematicTag) {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}
}
