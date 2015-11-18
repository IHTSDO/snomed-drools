package org.ihtsdo.drools.helper;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Relationship;

import java.util.HashSet;
import java.util.Set;

public class RelationshipHelper {

	public static int countRelationshipsInGroup(Concept concept, int group) {
		int count = 0;
		for (Relationship relationship : concept.getRelationships()) {
			if (!Constants.INFERRED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())
					&& relationship.getRelationshipGroup() == group) {
				count++;
			}
		}
		return count;
	}

	public static boolean hasActiveRelationshipOfType(Concept concept, String typeId) {
		for (Relationship relationship : concept.getRelationships()) {
			if (relationship.isActive() && typeId.equals(relationship.getTypeId())) {
				return true;
			}
		}
		return false;
	}

	public static Set<String> getActiveStatedParentConceptIds(Concept concept) {
		Set<String> activeParentConceptIds = new HashSet<>();
		for (Relationship relationship : concept.getRelationships()) {
			if (relationship.isActive()
					&& !Constants.INFERRED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())
					&& Constants.IS_A.equals(relationship.getTypeId())) {
				activeParentConceptIds.add(relationship.getDestinationId());
			}
		}
		return activeParentConceptIds;
	}

}
