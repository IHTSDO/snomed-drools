package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.validator.rf2.FileLoaderTestUtils;
import org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentRepository;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;

import java.io.FileNotFoundException;

public class BaseServiceTest {

    protected SnomedDroolsComponentRepository repository = new SnomedDroolsComponentRepository();

    protected void loadConceptsIntoRepository() throws FileNotFoundException {
        repository.addConcept(FileLoaderTestUtils.fileToObject("data/138875005.json", DroolsConcept.class));
        repository.addConcept(FileLoaderTestUtils.fileToObject("data/1263005.json", DroolsConcept.class));
        repository.addConcept(FileLoaderTestUtils.fileToObject("data/91832008.json", DroolsConcept.class));
        repository.addConcept(FileLoaderTestUtils.fileToObject("data/123037004.json", DroolsConcept.class));
        repository.addConcept(FileLoaderTestUtils.fileToObject("data/4421005.json", DroolsConcept.class));
        repository.addConcept(FileLoaderTestUtils.fileToObject("data/67185001.json", DroolsConcept.class));
        repository.addConcept(FileLoaderTestUtils.fileToObject("data/91723000.json", DroolsConcept.class));
        repository.addConcept(FileLoaderTestUtils.fileToObject("data/442083009.json", DroolsConcept.class));
        repository.addConcept(new DroolsConcept("12345678", "20250501",true,"900000000000207008", "900000000000073002",true,true));
    }

    protected void loadDescriptionsIntoRepository() {
        for (DroolsConcept droolsConcept : repository.getConcepts()) {
            for (DroolsDescription droolsDescription : droolsConcept.getDescriptions()) {
                repository.addDescription(droolsDescription);
            }
        }
    }

}
