package org.ihtsdo.drools.validator.rf2;

import com.google.common.collect.Sets;
import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.service.DroolsConceptService;
import org.ihtsdo.drools.validator.rf2.service.DroolsDescriptionService;
import org.ihtsdo.drools.validator.rf2.service.DroolsRelationshipService;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.ihtsdo.drools.response.Severity.ERROR;

public class DroolsRF2Validator {

	private final RuleExecutor ruleExecutor;
	private final Logger logger = LoggerFactory.getLogger(DroolsRF2Validator.class);

	public DroolsRF2Validator(String directoryOfRuleSetsPath) {
		Assert.isTrue(new File(directoryOfRuleSetsPath).isDirectory(), "The rules directory is not accessible.");
		ruleExecutor = new RuleExecutor(directoryOfRuleSetsPath);
	}

	public List<InvalidContent> validateSnapshot(InputStream snomedRf2EditionZip, Set<String> ruleSetNamesToRun) throws ReleaseImportException {
		long start = new Date().getTime();
		Assert.isTrue(ruleSetNamesToRun != null && !ruleSetNamesToRun.isEmpty(), "The name of at least one rule set must be specified.");

		ReleaseImporter importer = new ReleaseImporter();
		SnomedDroolsComponentRepository repository = new SnomedDroolsComponentRepository();
		logger.info("Loading components from RF2");
		LoadingProfile loadingProfile = LoadingProfile.complete;
		loadingProfile.getIncludedReferenceSetFilenamePatterns().add(".*_cRefset_Language.*");
		importer.loadSnapshotReleaseFiles(snomedRf2EditionZip, loadingProfile, new SnomedDroolsComponentFactory(repository));
		logger.info("Components loaded");

		DroolsConceptService conceptService = new DroolsConceptService(repository);
		DroolsDescriptionService descriptionService = new DroolsDescriptionService(repository);
		DroolsRelationshipService relationshipService = new DroolsRelationshipService(repository);

		Collection<DroolsConcept> concepts = repository.getConcepts();
		logger.info("Running tests");
		List<InvalidContent> invalidContents = ruleExecutor.execute(ruleSetNamesToRun, concepts, conceptService, descriptionService, relationshipService, true, false);
		logger.info("Tests complete. Total run time {} seconds", (new Date().getTime() - start) / 1000);
		logger.info("invalidContent count {}", invalidContents.size());
		return invalidContents;
	}

	public RuleExecutor getRuleExecutor() {
		return ruleExecutor;
	}

	public static void main(String[] args) throws IOException, ReleaseImportException {
		String releaseFilePath = "/Users/kai/release/SnomedCT_InternationalRF2_Production_20180216T020000Z.zip";
		String directoryOfRuleSetsPath = "../snomed-drools-rules";
		HashSet<String> ruleSetNamesToRun = Sets.newHashSet("common-authoring");
		List<InvalidContent> invalidContents = new DroolsRF2Validator(directoryOfRuleSetsPath).validateSnapshot(new FileInputStream(releaseFilePath), ruleSetNamesToRun);

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
