package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.service.ConceptService;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsRelationship;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DroolsConceptService implements ConceptService {

	private final SnomedDroolsComponentRepository repository;

	private final String currentEffectiveTime;

	private Set<String> topLevelHierarchies;

	public DroolsConceptService(SnomedDroolsComponentRepository repository, String currentEffectiveTime) {
		this.repository = repository;
		this.currentEffectiveTime = currentEffectiveTime;
	}

	@Override
	public boolean isActive(String conceptId) {
		DroolsConcept concept = repository.getConcept(conceptId);
		return concept != null && concept.isActive();
	}

	@Override
	public boolean isInactiveConceptSameAs(String inactiveConceptId, String conceptId) {
		DroolsConcept inactiveConcept = repository.getConcept(inactiveConceptId);
		String sameAsAssociationText = Constants.historicalAssociationNames.get(Constants.REFSET_SAME_AS_ASSOCIATION);
		return inactiveConcept != null
				&& !inactiveConcept.getAssociationTargets().isEmpty()
				&& inactiveConcept.getAssociationTargets().containsKey(sameAsAssociationText)
				&& inactiveConcept.getAssociationTargets().get(sameAsAssociationText).contains(conceptId);
	}

	@Override
	public boolean isConceptModellingChanged(Concept concept) {
		DroolsConcept droolsConcept = repository.getConcept(concept.getId());
		boolean changed = droolsConcept.getOntologyAxioms().stream().anyMatch(axiom -> !axiom.isAxiomGCI() && axiom.getEffectiveTime().equals(this.currentEffectiveTime));
		if (!changed) {
			changed = droolsConcept.getRelationships().stream().anyMatch(relationship -> relationship.getAxiomId() == null && Constants.INFERRED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId()) && relationship.getEffectiveTime().equals(this.currentEffectiveTime));
		}
		return changed;
}

	@Override
	public Concept findById(String conceptId) {
		return repository.getConcept(conceptId);
	}

	@Override
	public Set<String> getAllTopLevelHierarchies() {
		if (!CollectionUtils.isEmpty(topLevelHierarchies)) return topLevelHierarchies;

		synchronized (this) {
			if (!CollectionUtils.isEmpty(topLevelHierarchies)) return topLevelHierarchies;
			
			DroolsConcept rootConcept = repository.getConcept(Constants.ROOT_CONCEPT);
			topLevelHierarchies = new HashSet<>();
			for (DroolsRelationship relationship : rootConcept.getActiveInboundStatedRelationships()) {
				if(Constants.IS_A.equals(relationship.getTypeId())) {
					topLevelHierarchies.add(relationship.getSourceId());
				}
			}
			return  topLevelHierarchies;
		}
	}

	@Override
	public Set<String> findStatedAncestorsOfConcept(Concept concept) {
		if(concept == null || concept.getId().equals(Constants.ROOT_CONCEPT)) {
			return Collections.emptySet();
		}

		DroolsConcept droolsConcept = this.repository.getConcept(concept.getId());
		if (droolsConcept != null && droolsConcept.getStatedAncestorIds() != null) {
			return droolsConcept.getStatedAncestorIds().stream().map(Object::toString).collect(Collectors.toSet());
		}

		return Collections.emptySet();
	}

	@Override
	public Set<String> findTopLevelHierarchiesOfConcept(Concept concept) {
		Set<String> statedAncestors = findStatedAncestorsOfConcept(concept);
		Set<String> allTopLevelHierarchies = getAllTopLevelHierarchies();
		statedAncestors.retainAll(allTopLevelHierarchies);

		return statedAncestors;
	}


	@Override
	public Set<String> findStatedAncestorsOfConcepts(List<String> conceptIds) {
		Set<String> resultSet = new HashSet<>();
		for (String conceptId : conceptIds) {
			resultSet.addAll(findStatedAncestorsOfConcept(repository.getConcept(conceptId)));
		}
		return resultSet;
	}

	@Override
	public Set<String> findLanguageReferenceSetByModule(String moduleId) {
		Set<String> resultSet = new HashSet<>();
		DroolsConcept languageTypeConcept = repository.getConcept(Constants.LANGUAGE_TYPE_CONCEPT);
		Set<String> children = new HashSet<>();
		for (DroolsRelationship relationship : languageTypeConcept.getActiveInboundStatedRelationships()) {
			if(Constants.IS_A.equals(relationship.getTypeId())) {
				children.add(relationship.getSourceId());
			}
		}
		for (String conceptId : children) {
			DroolsConcept concept = repository.getConcept(conceptId);
			if (concept.getModuleId().equals(moduleId)) {
				resultSet.add(conceptId);
			}
			for (DroolsRelationship relationship : concept.getActiveInboundStatedRelationships()) {
				if(Constants.IS_A.equals(relationship.getTypeId())) {
					DroolsConcept sourceConcept = repository.getConcept(relationship.getSourceId());
					if (sourceConcept.getModuleId().equals(moduleId)) {
						resultSet.add(sourceConcept.getId());
					}
				}
			}
		}
		return  resultSet;
	}
}
