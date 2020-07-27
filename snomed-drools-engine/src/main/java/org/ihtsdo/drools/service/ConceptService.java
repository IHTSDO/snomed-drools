package org.ihtsdo.drools.service;

import org.ihtsdo.drools.domain.Concept;

import java.util.List;
import java.util.Set;

public interface ConceptService {

	boolean isActive(String conceptId);
	Concept findById(String conceptId);
	Set<String> getAllTopLevelHierarchies();
	Set<String> findStatedAncestorsOfConcept(Concept concept);
	Set<String> findTopLevelHierarchiesOfConcept(Concept concept);
	Set<String> findStatedAncestorsOfConcepts(List<String> conceptIds);
	Set<String> findConceptsHavingMultipleTopHierarchies(String... topHierarchies);
}
