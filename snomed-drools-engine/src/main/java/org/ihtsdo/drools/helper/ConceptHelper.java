package org.ihtsdo.drools.helper;

import org.ihtsdo.drools.domain.Concept;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConceptHelper {
	
	public static boolean containsAnyConcept(List<Concept> source, String... restrictConcepts){
		List<String> sourceId = new ArrayList<String>();
		for(Concept c : source){
			sourceId.add(c.getId());
		}
		return sourceId.containsAll(new ArrayList<String>(Arrays.asList(restrictConcepts)));
	}
	
	public static boolean isMoreThanOneTopLevelHierarchy(List<Concept> concepts, String... restrictConcepts){
		List<String> sourceId = new ArrayList<String>();
		for(Concept c : concepts){
			if  (!sourceId.contains(c.getId())) {
				sourceId.add(c.getId());
			}
		}
		return (sourceId.size() > 1);
	}
}
