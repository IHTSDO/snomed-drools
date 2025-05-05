package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.validator.rf2.FileLoaderTestUtils;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsRelationship;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.Set;
import java.util.stream.Collectors;

public class DroolsConceptServiceTest extends BaseServiceTest {

    private DroolsConceptService droolsConceptService;

    @Before
    public void setup() throws FileNotFoundException {
        loadConceptsIntoRepository();
        droolsConceptService = new DroolsConceptService(repository, null);
    }

    @Test
    public void testIsConceptActive() {
        Assert.assertTrue(droolsConceptService.isActive("1263005"));
    }

    @Test
    public void testGetAllTopLevelHierachies() throws FileNotFoundException {
        DroolsConcept rootConcept = FileLoaderTestUtils.fileToObject("data/138875005.json", DroolsConcept.class);
        Set<String> expectedRootChildren = rootConcept.getActiveInboundStatedRelationships().stream()
                .filter(relationship -> relationship.isActive() && Constants.STATED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())
                        && Constants.IS_A.equals(relationship.getTypeId())).map(DroolsRelationship::getSourceId).collect(Collectors.toSet());
        Set<String> results = droolsConceptService.getAllTopLevelHierarchies();
        Assert.assertEquals(expectedRootChildren.size(), results.size());
        for (String expectedRootChild : expectedRootChildren) {
            Assert.assertTrue("Actual result does not contains concept " + expectedRootChild, results.contains(expectedRootChild));
        }
    }

    @Test
    public void testFindStatedAncestorsOfConcept() {
        Concept concept = new DroolsConcept("1263005", "20250501", true, "900000000000207008", "900000000000073002", true, true);
        Set<String> results = droolsConceptService.findStatedAncestorsOfConcept(concept);
        Assert.assertEquals(7, results.size());

        concept = new DroolsConcept("91832008", "20250501", true, "900000000000207008", "900000000000073002", true, true);
        results = droolsConceptService.findStatedAncestorsOfConcept(concept);
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testFindTopLevelHierachiesOfConcept() {
        Concept concept = new DroolsConcept("1263005", "20250501", true, "900000000000207008", "900000000000073002", true, true);
        Set<String> results = droolsConceptService.findTopLevelHierarchiesOfConcept(concept);
        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.contains("123037004"));
    }

}
