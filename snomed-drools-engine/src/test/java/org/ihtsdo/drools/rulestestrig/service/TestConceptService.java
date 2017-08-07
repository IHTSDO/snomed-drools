package org.ihtsdo.drools.rulestestrig.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.service.ConceptService;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.domain.Constants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestConceptService implements ConceptService {

	private final Map<String, Concept> concepts;

	public TestConceptService(Map<String, Concept> concepts) {
		this.concepts = concepts;
	}

	@Override
	public boolean isActive(String conceptId) {
		return concepts.get(conceptId).isActive();
	}
	
	@Override
	public Set<Concept> getAllTopLevelHierachies(){
		Set<Concept> allTopLevelHierachies = new HashSet<Concept>();
		for (Concept concept : concepts.values()) {
			for (Relationship relationship : concept.getRelationships()) {
				if (Constants.IS_A.equals(relationship.getTypeId()) && Constants.ROOT_CONCEPT.equals(relationship.getDestinationId())) {
					allTopLevelHierachies.add(concept);
				}
			}
		}
		return allTopLevelHierachies;
	}
	
	@Override
	public Set<Concept> findAllStatedAncestorsOfConcept(Concept c){
		Set<Concept> statedAncestors = new HashSet<>();
		Set<Concept> directParent = getStatedParents(c);
		statedAncestors.addAll(directParent);
		
		for (Concept parent : directParent) {
			Set<Concept> parentOfParent = findAllStatedAncestorsOfConcept(parent);
			statedAncestors.addAll(parentOfParent);
		}
		
		return statedAncestors;
	}
	
	@Override
	public Set<Concept> findAllTopLevelHierachiesOfConcept(Concept c){
		Set<Concept> listTopLevelConcepts = new HashSet<Concept>();
		for (Relationship relationship : c.getRelationships()) {
			if (Constants.IS_A.equals(relationship.getTypeId())) {
				if (Constants.ROOT_CONCEPT.equals(relationship.getDestinationId())) {
					listTopLevelConcepts.add(concepts.get(relationship.getSourceId()));
				} else {
					listTopLevelConcepts.addAll(findAllTopLevelHierachiesOfConcept(concepts.get(relationship.getDestinationId())));
				}
			}
		}
		
		return listTopLevelConcepts;
	}
	
	private Set<Concept> getStatedParents(Concept concept) {
		final Set<Concept> parents = new HashSet<Concept>();
		for (Relationship relationship : concept.getRelationships()) {
			if (Constants.IS_A.equals(relationship.getTypeId()) && Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				parents.add(concepts.get(relationship.getDestinationId()));
			}
		}
		return parents;
	}
	
}
