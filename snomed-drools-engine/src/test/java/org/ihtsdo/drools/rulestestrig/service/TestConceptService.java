package org.ihtsdo.drools.rulestestrig.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.helper.DescriptionHelper;
import org.ihtsdo.drools.service.ConceptService;

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
	public Set<String> getAllTopLevelHierachies(){
		Set<String> allTopLevelHierachies = new HashSet<String>();
		for (Concept concept : concepts.values()) {
			for (Relationship relationship : concept.getRelationships()) {
				if (relationship.isActive()
					&& Constants.IS_A.equals(relationship.getTypeId()) 
					&& Constants.ROOT_CONCEPT.equals(relationship.getDestinationId())) {
					allTopLevelHierachies.add(concept.getId());
				}
			}
		}
		return allTopLevelHierachies;
	}
	
	@Override
	public Set<String> findStatedAncestorsOfConcept(Concept c){
		Set<String> statedAncestors = new HashSet<String>();
		Set<String> directParent = getStatedParents(c);
		statedAncestors.addAll(directParent);
		
		for (String parentId : directParent) {
			Set<String> parentOfParent = findStatedAncestorsOfConcept(concepts.get(parentId));
			statedAncestors.addAll(parentOfParent);
		}
		
		return statedAncestors;
	}
	
	@Override
	public Set<String> findTopLevelHierachiesOfConcept(Concept c){
		Set<String> topLevelConcepts = new HashSet<String>();
		for (Relationship relationship : c.getRelationships()) {
			if (relationship.isActive()
				&& Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())	 
				&& Constants.IS_A.equals(relationship.getTypeId())) {
				if (Constants.ROOT_CONCEPT.equals(relationship.getDestinationId())) {
					topLevelConcepts.add(relationship.getSourceId());
				} else {
					topLevelConcepts.addAll(findTopLevelHierachiesOfConcept(concepts.get(relationship.getDestinationId())));
				}
			}
		}
		
		return topLevelConcepts;
	}
	
	private Set<String> getStatedParents(Concept concept) {
		if(concept == null || concept.getId().equals(Constants.ROOT_CONCEPT)) {
			return Collections.emptySet();
		}
		final Set<String> parents = new HashSet<String>();
		for (Relationship relationship : concept.getRelationships()) {
			if (relationship.isActive() 
				&& Constants.IS_A.equals(relationship.getTypeId()) 
				&& Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				parents.add(relationship.getDestinationId());
			}
		}
		return parents;
	}

	@Override 
	public Set<String> findSematicTagOfAncestors(List<String> conceptIds) {
		Set<String> ancestorIds = new HashSet<>();
		for (String id : conceptIds) {
			ancestorIds.addAll(findStatedAncestorsOfConcept(concepts.get(id)));
		}
		Set<String> tags = new HashSet<>();
		if(!ancestorIds.isEmpty()) {
			for (String ancestorId : ancestorIds) {
				Concept c = concepts.get(ancestorId);
				for (Description d : c.getDescriptions()) {
					if(d.isActive() && Constants.FSN.equals(d.getTypeId())) {
						tags.add(DescriptionHelper.getTag(d.getTerm()));
					}
				}
			}	
		}
		return tags;
	}
	
}
