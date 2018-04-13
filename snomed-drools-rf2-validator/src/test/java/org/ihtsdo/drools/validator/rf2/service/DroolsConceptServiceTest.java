package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.validator.rf2.FileLoaderTestUtils;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

public class DroolsConceptServiceTest {

    private SnomedDroolsComponentRepository repository;

    private DroolsConceptService droolsConceptService;

    @Before
    public void setup() {
        if(repository == null) {
            repository = new SnomedDroolsComponentRepository();
            FileLoaderTestUtils.loadReleaseFile("D:\\SNOMED\\Int_20180131\\SnomedCT_InternationalRF2_PRODUCTION_20180131T120000Z_PROD.zip", repository);
            droolsConceptService = new DroolsConceptService(repository);
        }
    }

    @Test
    public void testGetAllTopLevelHierachies() {
        Set<String> results = droolsConceptService.getAllTopLevelHierachies();
        results.stream().forEach(result -> System.out.println(result));
    }

    @Test
    public void testFindStatedAncestorsOfConcept() {
        Concept concept = new DroolsConcept("239221000",true,"900000000000207008", "900000000000073002",true,true);
        Set<String> results = droolsConceptService.findStatedAncestorsOfConcept(concept);
        results.stream().forEach(result -> System.out.println(result));
    }

    @Test
    public void testFindTopLevelHierachiesOfConcept() {
        Concept concept = new DroolsConcept("239221000",true,"900000000000207008", "900000000000073002",true,true);
        Set<String> results = droolsConceptService.findTopLevelHierachiesOfConcept(concept);
        results.stream().forEach(result -> System.out.println(result));
    }


}
