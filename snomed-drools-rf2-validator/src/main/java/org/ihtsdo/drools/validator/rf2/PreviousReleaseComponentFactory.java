package org.ihtsdo.drools.validator.rf2;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.ihtsdo.otf.snomedboot.factory.ImpotentComponentFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.lang.Long.parseLong;

public class PreviousReleaseComponentFactory extends ImpotentComponentFactory {

    private final Set<Long> releasedConceptIds = Collections.synchronizedSet(new LongOpenHashSet());
    private final Set<Long> releasedDescriptionIds = Collections.synchronizedSet(new LongOpenHashSet());
    private final Set<Long> releasedRelationshipIds = Collections.synchronizedSet(new LongOpenHashSet());
    private final Set<String> releaseRefsetMemberIds = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void newConceptState(String conceptId, String effectiveTime, String active, String moduleId, String definitionStatusId) {
        releasedConceptIds.add(parseLong(conceptId));
    }

    @Override
    public void newDescriptionState(String id, String effectiveTime, String active, String moduleId, String conceptId, String languageCode, String typeId, String term, String caseSignificanceId) {
        releasedDescriptionIds.add(parseLong(id));
    }

    @Override
    public void newRelationshipState(String id, String effectiveTime, String active, String moduleId, String sourceId, String destinationId, String relationshipGroup, String typeId, String characteristicTypeId, String modifierId) {
		releasedRelationshipIds.add(parseLong(id));
    }

	@Override
	public void newConcreteRelationshipState(String id, String effectiveTime, String active, String moduleId, String sourceId, String value, String relationshipGroup, String typeId, String characteristicTypeId, String modifierId) {
		releasedRelationshipIds.add(parseLong(id));
	}

    @Override
    public void newReferenceSetMemberState(String[] fieldNames, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... otherValues) {
        releaseRefsetMemberIds.add(id);
    }

	public Set<Long> getReleasedConceptIds() {
		return releasedConceptIds;
	}

	public Set<Long> getReleasedDescriptionIds() {
		return releasedDescriptionIds;
	}

	public Set<Long> getReleasedRelationshipIds() {
		return releasedRelationshipIds;
	}

	public Set<String> getReleaseRefsetMemberIds() {
		return releaseRefsetMemberIds;
	}
}
