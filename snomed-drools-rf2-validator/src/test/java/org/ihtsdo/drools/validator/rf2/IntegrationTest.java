package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.response.Severity;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snomed.otf.snomedboot.testutil.ZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class IntegrationTest {

    private DroolsRF2Validator droolsRF2Validator;
    private File snomedRF2Zip;

    @Before
    public void setup() throws IOException {
        droolsRF2Validator = new DroolsRF2Validator("src/test/resources/dummy-assertions");
        snomedRF2Zip = ZipUtil.zipDirectoryRemovingCommentsAndBlankLines("src/test/resources/dummy-snomed-content/SnomedCT_MiniRF2_Base_snapshot");
        snomedRF2Zip.deleteOnExit();
    }

    @Test
    public void testValidateAxiomUsingRelationshipAssertion() throws IOException, ReleaseImportException {
        List<InvalidContent> invalidContentsReport = droolsRF2Validator.validateSnapshotStreams(
                Collections.singleton(new FileInputStream(snomedRF2Zip)),
                null,
                Collections.singleton("unit-test"),
                "20190131",
                null);

        invalidContentsReport.forEach(invalidContent -> System.out.println(invalidContent));

        assertEquals(2, invalidContentsReport.size());

        assertEquals(Severity.WARNING, invalidContentsReport.get(0).getSeverity());
        assertEquals("Test resources were not available so assertions like case significance and US specific terms checks will not have run.",
                invalidContentsReport.get(0).getMessage());

        assertEquals(Severity.ERROR, invalidContentsReport.get(1).getSeverity());
        assertEquals("Concepts must not have relationships to inactive concepts.", invalidContentsReport.get(1).getMessage());
        assertEquals("1b6427e7-23de-476f-8f25-423306f180ac/Group_0/Type_100104001/Destination_100107001", invalidContentsReport.get(1).getComponentId());
        assertEquals("100105001", invalidContentsReport.get(1).getConceptId());
    }

}
