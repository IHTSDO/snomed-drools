package org.ihtsdo.drools.rulestestrig.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.service.DescriptionService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestDescriptionService implements DescriptionService {

	private final Map<String, Concept> concepts;

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
						Constants.ACCEPTABILITY_PREFERRED.equals(description.getAcceptabilityMap().get(languageRefsetId));
						fsns.add(description.getTerm());
					}
				}
			}
		}
		return fsns;
	}
}
