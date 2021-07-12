package org.ihtsdo.drools.validator.rf2;

import com.google.common.collect.Sets;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.ihtsdo.drools.response.Severity.ERROR;
import static org.ihtsdo.drools.validator.rf2.FileLoaderTestUtils.copyRF2RemovingComments;

public class IntegrationManualTest {
    
    public static void main(String[] args) throws IOException, ReleaseImportException {
        String releaseFilePath = "/path/to/all-files";
        String directoryOfRuleSetsPath = "path/to/rules";
        Set<String> ruleSetNamesToRun = Sets.newHashSet("common-authoring,int-authoring".split(","));
		final String currentEffectiveTime = "20200131";
        Set<String> includedModulesSet = Sets.newHashSet();

		List<InvalidContent> invalidContents = new DroolsRF2Validator(directoryOfRuleSetsPath, false)
				.validateRF2Files(Collections.singleton(copyRF2RemovingComments(releaseFilePath)), null, ruleSetNamesToRun, currentEffectiveTime, includedModulesSet, false);

        // Some extra output when running this main method in development -
        int outputSize = Math.min(invalidContents.size(), 50);
        System.out.println("First 50 failures:");
        for (InvalidContent invalidContent : invalidContents.subList(0, outputSize)) {
            System.out.println(invalidContent);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("errors.txt"))) {
            writer.write("Severity\tMessage\tConceptId\tComponentId");
            writer.newLine();

            System.out.println();
            System.out.println("Failure counts by assertion:");
            Map<String, AtomicInteger> failureCounts = new HashMap<>();
            for (InvalidContent invalidContent : invalidContents) {
                String message = invalidContent.getSeverity().toString() + " - " + invalidContent.getMessage();
                AtomicInteger atomicInteger = failureCounts.get(message);
                if (atomicInteger == null) {
                    atomicInteger = new AtomicInteger();
                    failureCounts.put(message, atomicInteger);
                }
                atomicInteger.incrementAndGet();

                // also write out all errors
                if (invalidContent.getSeverity() == ERROR) {
                    writer.write(
                            invalidContent.getSeverity()
                                    + "\t" + invalidContent.getMessage()
                                    + "\t" + invalidContent.getConceptId()
                                    + "\t" + invalidContent.getComponentId()
                    );
                    writer.newLine();
                }
            }
            for (String errorMessage : failureCounts.keySet()) {
                System.out.println(failureCounts.get(errorMessage).toString() + " - " + errorMessage);
            }
        }
    }



}
