package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.otf.snomedboot.factory.ImpotentComponentFactory;

import java.util.Set;
import java.util.TreeSet;

public class PreviousReleaseComponentFactory extends ImpotentComponentFactory {

    private Set<String> releasedConcepts = new TreeSet<>();
    private Set<String> releasedDescriptions = new TreeSet<>();
    private Set<String> releasedRelationships = new TreeSet<>();
    private Set<String> releaseRefsetMembers = new TreeSet<>();
    private static final String INFERRED_RELATIONSHIP = "900000000000011006";

    @Override
    public void newConceptState(String conceptId, String effectiveTime, String active, String moduleId, String definitionStatusId) {
        releasedConcepts.add(conceptId);
    }

    @Override
    public void newDescriptionState(String id, String effectiveTime, String active, String moduleId, String conceptId, String languageCode, String typeId, String term, String caseSignificanceId) {
        releasedDescriptions.add(id);
    }

    @Override
    public void newRelationshipState(String id, String effectiveTime, String active, String moduleId, String sourceId, String destinationId, String relationshipGroup, String typeId, String characteristicTypeId, String modifierId) {
        if (!characteristicTypeId.equals(INFERRED_RELATIONSHIP)) {
            releasedRelationships.add(id);
        }
    }

    @Override
    public void newReferenceSetMemberState(String[] fieldNames, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... otherValues) {
        releaseRefsetMembers.add(refsetId + "_" + id);
    }

    public Set<String> getReleasedConcepts() {
        return releasedConcepts;
    }

    public Set<String> getReleasedDescriptions() {
        return releasedDescriptions;
    }

    public Set<String> getReleasedRelationships() {
        return releasedRelationships;
    }

    public Set<String> getReleaseRefsetMembers() {
        return releaseRefsetMembers;
    }

    public void cleanup() {
        releasedConcepts.clear();
        releasedDescriptions.clear();
        releasedRelationships.clear();
        releaseRefsetMembers.clear();
    }
}
