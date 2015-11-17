package org.ihtsdo.drools.helper;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Relationship;

public class RelationshipHelper {

	public static int countRelationshipsInGroup(Concept concept, int group) {
		int count = 0;
		for (Relationship relationship : concept.getRelationships()) {
			if (relationship.getRelationshipGroup() == group) {
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

}
