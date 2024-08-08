package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.service.TestResourceProvider;
import org.ihtsdo.drools.validator.rf2.DroolsRF2Validator;
import org.ihtsdo.drools.validator.rf2.FileLoaderTestUtils;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.snomedboot.domain.ConceptConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DroolsDescriptionServiceTest extends BaseServiceTest{


    private DroolsDescriptionService droolsDescriptionService;

    @Before
    public void setup() throws IOException {
        loadConceptsIntoRepository();
        loadDescriptionsIntoRepository();
        ResourceManager resourceManager = new ResourceManager(DroolsRF2Validator.BLANK_RESOURCES_CONFIGURATION, null);
        DroolsConceptService droolsConceptService = new DroolsConceptService(repository);
        droolsDescriptionService = new DroolsDescriptionService(repository, droolsConceptService, new TestResourceProvider(resourceManager));
    }

    @Test
    public void testFindFSNs() {
        repository.getConcept("1263005").getDescriptions().stream()
                .filter(d -> ConceptConstants.FSN.equals(d.getTypeId()))
                .forEach(d -> d.getAcceptabilityMap().put(ConceptConstants.US_EN_LANGUAGE_REFERENCE_SET, Constants.ACCEPTABILITY_PREFERRED));

        Set<String> conceptsIds = new HashSet<>();
        conceptsIds.add("1263005");
        Set<String> results = droolsDescriptionService.getFSNs(conceptsIds, ConceptConstants.US_EN_LANGUAGE_REFERENCE_SET);
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
