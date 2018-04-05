package org.ihtsdo.drools.service;

import org.ihtsdo.drools.domain.Concept;

import java.util.List;
import java.util.Set;

public interface ConceptService {

	boolean isActive(String conceptId);
	Set<String> getAllTopLevelHierachies();
	Set<String> findStatedAncestorsOfConcept(Concept concept);
	Set<String> findTopLevelHierachiesOfConcept(Concept concept);
	Set<String> findSematicTagOfAncestors(List<String> conceptIds);
}
