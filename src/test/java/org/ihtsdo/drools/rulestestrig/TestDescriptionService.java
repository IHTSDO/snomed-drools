package org.ihtsdo.drools.rulestestrig;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.service.DescriptionService;

import java.util.List;

public class TestDescriptionService implements DescriptionService {

	private List<Concept> testConcepts;

	@Override
	public boolean isUniqueActiveTerm(String searchTerm) {
		for (Concept testConcept : testConcepts) {
			for (Description description : testConcept.getDescriptions()) {
				if (description.isActive() && description.getTerm().equals(searchTerm)) {
					return false;
				}
			}
		}
		return true;
	}

	public void addTestConcepts(List<Concept> testConcepts) {
		this.testConcepts = testConcepts;
	}
}
