package org.ihtsdo.drools.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ihtsdo.drools.domain.Concept;

public class ConceptHelper {
	
	public static boolean containsAnyConcept(List<Concept> concepts, String... restrictConcepts){
		List<String> topLevelConceptIds = collectConceptId(concepts);
		List<String> restrictList = new ArrayList<String>(Arrays.asList(restrictConcepts));
		
		return topLevelConceptIds.size() > 1 && topLevelConceptIds.size() == restrictList.size() && topLevelConceptIds.containsAll(restrictList);
	}
	
	public static boolean isMoreThanOneTopLevelHierarchy(List<Concept> concepts, String... restrictConcepts){
		List<String> topLevelConceptIds = collectConceptId(concepts);
		List<String> restrictList = new ArrayList<String>(Arrays.asList(restrictConcepts));
		
		if (topLevelConceptIds.size() > 1) {
			if (topLevelConceptIds.size() > restrictList.size()) {
				return true;
			} else if (topLevelConceptIds.size() == restrictList.size()) {
				return !topLevelConceptIds.containsAll(restrictList);
			} else {
				return false;
			}
		}
		
		return false;
	}
	
	private static List<String> collectConceptId(List<Concept> concepts) {
		List<String> conceptIds = new ArrayList<String>();
		for(Concept c : concepts){
			if (!conceptIds.contains(c.getId())) {
				conceptIds.add(c.getId());
			}
		}
		
		return conceptIds;
	}
}
