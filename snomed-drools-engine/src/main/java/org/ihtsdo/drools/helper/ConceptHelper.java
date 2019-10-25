package org.ihtsdo.drools.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Relationship;

public class ConceptHelper {
	
	public static boolean containsAnyConcept(List<String> conceptIdList, String... restrictConcepts){
		List<String> restrictList = new ArrayList<String>(Arrays.asList(restrictConcepts));		
		return conceptIdList.size() > 1 && conceptIdList.size() == restrictList.size() && conceptIdList.containsAll(restrictList);
	}
	
	public static boolean isMoreThanOneTopLevelHierarchy(List<String> conceptIdList, String... restrictConcepts){
		List<String> restrictList = new ArrayList<String>(Arrays.asList(restrictConcepts));
		
		if (conceptIdList.size() > 1) {
			if (conceptIdList.size() > restrictList.size()) {
				return true;
			} else if (conceptIdList.size() == restrictList.size()) {
				return !conceptIdList.containsAll(restrictList);
			} else {
				return false;
			}
		}
		
		return false;
	}

    public static boolean isAxiomEquivalent(Concept concept) {
        Map<String, List<Relationship>> axiomMap = new HashMap<>();
        concept.getRelationships().forEach(relationship -> {
            if (!relationship.isAxiomGCI()) {
                if (axiomMap.containsKey(relationship.getAxiomId())) {
                    axiomMap.get(relationship.getAxiomId()).add(relationship);
                } else {
                    List<Relationship> relationships = new ArrayList<>();
                    relationships.add(relationship);
                    axiomMap.put(relationship.getAxiomId(), relationships);
                }
            }
        });

        for (String key1 : axiomMap.keySet()) {
            for (String key2 : axiomMap.keySet()) {
                List<Relationship> relationships1 = axiomMap.get(key1);
                List<Relationship> relationships2 = axiomMap.get(key2);
                if (key1 != key2 && relationships1.size() == relationships2.size()) {
                    int count = 0;
                    for (Relationship relationship1 : relationships1) {
                        for (Relationship relationship2 : relationships2) {
                            //can not use 'equal' to compare 2 relationship objects as they always have different axiomId
                            if (relationship1.getTypeId() == relationship2.getTypeId()
                                    && relationship1.getDestinationId() == relationship2.getDestinationId()
                                    && relationship1.getRelationshipGroup() == relationship2.getRelationshipGroup()) {
                                count++;
                            }
                        }
                    }
                    if (count == relationships1.size()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
