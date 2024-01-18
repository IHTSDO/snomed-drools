package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.response.Severity;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.ihtsdo.drools.validator.rf2.FileLoaderTestUtils.copyRF2RemovingComments;
import static org.junit.Assert.assertEquals;

public class IntegrationTest {

    private DroolsRF2Validator droolsRF2Validator;

    @Before
    public void setup() throws IOException {
        droolsRF2Validator = new DroolsRF2Validator("src/test/resources/dummy-assertions", false);
    }

    @Test
    public void testValidateAxiomUsingRelationshipAssertion() throws ReleaseImportException, IOException {
		final String extractedRF2FilesDirectory = copyRF2RemovingComments("src/test/resources/dummy-snomed-content/SnomedCT_MiniRF2_Base_snapshot");

		List<InvalidContent> invalidContentsReport = droolsRF2Validator.validateRF2Files(
				Collections.singleton(extractedRF2FilesDirectory),
                null,
                Collections.singleton("unit-test"),
                null,
                "20190131",
                null,
				false);

        invalidContentsReport.forEach(System.out::println);

        assertEquals(8, invalidContentsReport.size());

        int index = 0;
        assertEquals(Severity.WARNING, invalidContentsReport.get(index).getSeverity());
        assertEquals("Test resources were not available so assertions like case significance and US specific terms checks will not have run.",
                invalidContentsReport.get(index).getMessage());

        index++;
        assertEquals(Severity.ERROR, invalidContentsReport.get(index).getSeverity());
        assertEquals("Concepts must not have relationships to inactive concepts.", invalidContentsReport.get(index).getMessage());
        assertEquals("1b6427e7-23de-476f-8f25-423306f180ac/Group_0/Type_100104001/Destination_100107001", invalidContentsReport.get(index).getComponentId());
        assertEquals("100105001", invalidContentsReport.get(index).getConceptId());

        index++;
        assertEquals(Severity.ERROR, invalidContentsReport.get(index).getSeverity());
        assertEquals("ReferencedComponentId 404684003 does not match named concept 100105001 in Axiom 4291bcd4-a002-4256-b15e-4a2f8b2c27c5", invalidContentsReport.get(index).getMessage());
        assertEquals("4291bcd4-a002-4256-b15e-4a2f8b2c27c5", invalidContentsReport.get(index).getComponentId());
        assertEquals("404684003", invalidContentsReport.get(index).getConceptId());

        index++;
        assertEquals(Severity.ERROR, invalidContentsReport.get(index).getSeverity());
        assertEquals("ReferencedComponentId 404684003 does not match named concept 100105001 in Axiom abf66bf1-708c-4ae2-b969-1e98eec7d250", invalidContentsReport.get(index).getMessage());
        assertEquals("abf66bf1-708c-4ae2-b969-1e98eec7d250", invalidContentsReport.get(index).getComponentId());
        assertEquals("404684003", invalidContentsReport.get(index).getConceptId());

        index++;
        assertEquals(Severity.ERROR, invalidContentsReport.get(index).getSeverity());
        assertEquals("Relationship 53b64d8e-ec40-49e5-bc07-decf7113c7f8/Group_0/Type_116680003/Destination_100102001 references conceptId 123 which does not exist", invalidContentsReport.get(index).getMessage());
        assertEquals("53b64d8e-ec40-49e5-bc07-decf7113c7f8/Group_0/Type_116680003/Destination_100102001", invalidContentsReport.get(index).getComponentId());
        assertEquals("123", invalidContentsReport.get(index).getConceptId());

        index++;
        assertEquals(Severity.ERROR, invalidContentsReport.get(index).getSeverity());
        assertEquals("Relationship 53b64d8e-ec40-49e5-bc07-decf7113c7f8/Group_0/Type_100104001/Destination_100108001 references conceptId 123 which does not exist", invalidContentsReport.get(index).getMessage());
        assertEquals("53b64d8e-ec40-49e5-bc07-decf7113c7f8/Group_0/Type_100104001/Destination_100108001", invalidContentsReport.get(index).getComponentId());
        assertEquals("123", invalidContentsReport.get(index).getConceptId());

        index++;
        assertEquals(Severity.ERROR, invalidContentsReport.get(index).getSeverity());
        assertEquals("Axiom Refset 53b64d8e-ec40-49e5-bc07-decf7113c7f8 references conceptId 123 which does not exist", invalidContentsReport.get(index).getMessage());
        assertEquals("53b64d8e-ec40-49e5-bc07-decf7113c7f8", invalidContentsReport.get(index).getComponentId());
        assertEquals("123", invalidContentsReport.get(index).getConceptId());

        index++;
        assertEquals(Severity.ERROR, invalidContentsReport.get(index).getSeverity());
        assertEquals("Error parsing Axiom owlExpression for Axiom 990f6779-1475-41ff-aacd-4b1924d4ab9f", invalidContentsReport.get(index).getMessage());
        assertEquals("990f6779-1475-41ff-aacd-4b1924d4ab9f", invalidContentsReport.get(index).getComponentId());
        assertEquals("100105001", invalidContentsReport.get(index).getConceptId());
    }

}
