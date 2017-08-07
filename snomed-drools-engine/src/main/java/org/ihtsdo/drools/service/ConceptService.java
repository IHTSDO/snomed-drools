package org.ihtsdo.drools.service;

import org.ihtsdo.drools.domain.Concept;
import java.util.Set;

public interface ConceptService {

	boolean isActive(String conceptId);
	Set<Concept> getAllTopLevelHierachies();
	Set<Concept> findAllStatedAncestorsOfConcept(Concept concept);
	Set<Concept> findAllTopLevelHierachiesOfConcept(Concept concept);
}
