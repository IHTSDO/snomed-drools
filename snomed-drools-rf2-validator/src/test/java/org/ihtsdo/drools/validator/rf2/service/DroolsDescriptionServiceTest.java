package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.validator.rf2.FileLoaderTestUtils;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

public class DroolsDescriptionServiceTest extends BaseServiceTest{


    private DroolsDescriptionService droolsDescriptionService;

    @Before
    public void setup() throws FileNotFoundException {
        loadConceptsIntoRepository();
        loadDescriptionsIntoRepository();
        droolsDescriptionService = new DroolsDescriptionService(repository);

    }

    @Test
    public void testFindFSNs() {
        Set<String> conceptsIds = new HashSet<>();
        conceptsIds.add("1263005");
        Set<String> results = droolsDescriptionService.getFSNs(conceptsIds,"900000000000548007");
        Assert.assertEquals(1, results.size());
        for (String result : results) {
            Assert.assertEquals("Distinctive arrangement of microtubules (cell structure)", result);
        }

    }

    @Test
    public void testFindActiveDescriptionByExactTerm() {
        Set<Description> descriptions = droolsDescriptionService.findActiveDescriptionByExactTerm("Distinctive arrangement of microtubules");
        Assert.assertEquals(1, descriptions.size());
        for (Description description : descriptions) {
            Assert.assertEquals("3229017", description.getId());
        }
    }

    @Test
    public void testFindMatchingDescriptionsInHierachy() throws FileNotFoundException {
        //load a dummy concept which has duplicated description term with its parent
        repository.addConcept(FileLoaderTestUtils.fileToObject("data/1234567890.json", DroolsConcept.class));
        DroolsConcept testConcept = repository.getConcept("1234567890");
        DroolsDescription testDescription = null;
        for (DroolsDescription droolsDescription : testConcept.getDescriptions()) {
            testDescription = droolsDescription;
        }
        Set<Description> results = droolsDescriptionService.findMatchingDescriptionInHierarchy(testConcept, testDescription);
        Assert.assertEquals(1, results.size());
        for (Description result : results) {
            Assert.assertEquals("1204236014", result.getId());
        }
    }


}
