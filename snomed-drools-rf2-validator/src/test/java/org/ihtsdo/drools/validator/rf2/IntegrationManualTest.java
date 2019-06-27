package org.ihtsdo.drools.validator.rf2;

import com.google.common.collect.Sets;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.ihtsdo.drools.response.Severity.ERROR;

public class IntegrationManualTest {
    
    public static void main(String[] args) throws IOException, ReleaseImportException {
        String releaseFilePath = "/path/to/release1,/path/to/release2";
        String prevReleasePath = null;
        String deltaDirectoryPath = "path/to/delta";
        String directoryOfRuleSetsPath = "path/to/rules";
        HashSet<String> ruleSetNamesToRun = Sets.newHashSet("common-authoring,int-authoring".split(","));
        String includedModules = "";
        Set<String> includedModulesSet = includedModules.isEmpty() ? new HashSet<>() : Sets.newHashSet(includedModules.split(","));
        Set<InputStream> inputStreams = new HashSet<>();
        HashSet<String> releasesFiles = Sets.newHashSet(releaseFilePath.split(","));
        for (String releasesFile : releasesFiles) {
            inputStreams.add(new FileInputStream(releasesFile));
        }
        InputStream deltaInputStream = null; //new FileInputStream(deltaDirectoryPath);

        InputStream prevReleaseStream = null;
        if(prevReleasePath != null) {
            prevReleaseStream = new FileInputStream(prevReleasePath);
        }

        List<InvalidContent> invalidContents = new DroolsRF2Validator(directoryOfRuleSetsPath).validateSnapshotStreams(inputStreams, prevReleaseStream, prevReleaseStream, ruleSetNamesToRun, "20200131", includedModulesSet);

        // Some extra output when running this main method in development -
        int outputSize = invalidContents.size() > 50 ? 50 : invalidContents.size();
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
