package org.ihtsdo.drools.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
}
