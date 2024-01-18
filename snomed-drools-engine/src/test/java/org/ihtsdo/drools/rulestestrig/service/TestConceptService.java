package org.ihtsdo.drools.rulestestrig.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.service.ConceptService;

public class TestConceptService implements ConceptService {

	private final Map<String, Concept> concepts;

	public TestConceptService(Map<String, Concept> concepts) {
		this.concepts = concepts;
	}

	@Override
	public boolean isActive(String conceptId) {
		Concept concept = concepts.get(conceptId);
		return concept != null && concept.isActive();
	}

	@Override
	public boolean isInactiveConceptSameAs(String inactiveConceptId, String conceptId) {
		Concept inactiveConcept = concepts.get(inactiveConceptId);
		Concept activeConcept = concepts.get(conceptId);
		String sameAsAssociationText = Constants.historicalAssociationNames.get(Constants.REFSET_SAME_AS_ASSOCIATION);
		return inactiveConcept != null && activeConcept != null
				&& !inactiveConcept.getAssociationTargets().isEmpty()
				&& inactiveConcept.getAssociationTargets().containsKey(sameAsAssociationText)
				&& inactiveConcept.getAssociationTargets().get(sameAsAssociationText).contains(conceptId);
	}

	@Override
	public Concept findById(String conceptId) {
		return concepts.get(conceptId);
	}

	@Override
	public Set<String> getAllTopLevelHierarchies(){
		Set<String> allTopLevelHierarchies = new HashSet<>();
		for (Concept concept : concepts.values()) {
			for (Relationship relationship : concept.getRelationships()) {
				if (relationship.isActive()
					&& Constants.IS_A.equals(relationship.getTypeId()) 
					&& Constants.ROOT_CONCEPT.equals(relationship.getDestinationId())) {
					allTopLevelHierarchies.add(concept.getId());
				}
			}
		}
		return allTopLevelHierarchies;
	}
	
	@Override
	public Set<String> findStatedAncestorsOfConcept(Concept c){
		Set<String> directParent = getStatedParents(c);
		Set<String> statedAncestors = new HashSet<>(directParent);
		
		for (String parentId : directParent) {
			Set<String> parentOfParent = findStatedAncestorsOfConcept(concepts.get(parentId));
			statedAncestors.addAll(parentOfParent);
		}
		
		return statedAncestors;
	}
	
	@Override
	public Set<String> findTopLevelHierarchiesOfConcept(Concept c){
		Set<String> topLevelConcepts = new HashSet<>();
		for (Relationship relationship : c.getRelationships()) {
			if (relationship.isActive()
				&& Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())	 
				&& Constants.IS_A.equals(relationship.getTypeId())) {
				if (Constants.ROOT_CONCEPT.equals(relationship.getDestinationId())) {
					topLevelConcepts.add(relationship.getSourceId());
				} else {
					topLevelConcepts.addAll(findTopLevelHierarchiesOfConcept(concepts.get(relationship.getDestinationId())));
				}
			}
		}
		
		return topLevelConcepts;
	}
	
	private Set<String> getStatedParents(Concept concept) {
		if(concept == null || concept.getId().equals(Constants.ROOT_CONCEPT)) {
			return Collections.emptySet();
		}
		final Set<String> parents = new HashSet<>();
		for (Relationship relationship : concept.getRelationships()) {
			if (relationship.isActive()
				&& !relationship.isAxiomGCI()
				&& Constants.IS_A.equals(relationship.getTypeId()) 
				&& Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				parents.add(relationship.getDestinationId());
			}
		}
		return parents;
	}

	@Override 
	public Set<String> findStatedAncestorsOfConcepts(List<String> conceptIds) {
		Set<String> ancestorIds = new HashSet<>();
		for (String id : conceptIds) {
			ancestorIds.addAll(findStatedAncestorsOfConcept(concepts.get(id)));
		}
		return ancestorIds;
	}
	
}
