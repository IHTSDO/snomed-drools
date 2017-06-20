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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DroolsRF2Validator {

	private Logger logger = LoggerFactory.getLogger(DroolsRF2Validator.class);

	private List<InvalidContent> validateSnapshot(InputStream snomedRf2EditionZip, String directoryOfRuleSetsPath, Set<String> ruleSetNamesToRun) throws ReleaseImportException {
		long start = new Date().getTime();
		Assert.isTrue(new File(directoryOfRuleSetsPath).isDirectory(), "The rules directory is not accessible.");
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

		RuleExecutor ruleExecutor = new RuleExecutor(directoryOfRuleSetsPath);
		Collection<DroolsConcept> concepts = repository.getConcepts();
		logger.info("Running tests");
		List<InvalidContent> invalidContents = ruleExecutor.execute(ruleSetNamesToRun, concepts, conceptService, descriptionService, relationshipService, true, false);
		logger.info("Tests complete. Total run time {} seconds", (new Date().getTime() - start) / 1000);
		logger.info("invalidContent count {}", invalidContents.size());
		return invalidContents;
	}

	public static void main(String[] args) throws FileNotFoundException, ReleaseImportException {
		String releaseFilePath = "/Users/kai/release/xSnomedCT_InternationalRF2_ALPHA_20170731T120000Z.zip";
		String directoryOfRuleSetsPath = "../snomed-drools-rules";
		HashSet<String> ruleSetNamesToRun = Sets.newHashSet("common-authoring");
		List<InvalidContent> invalidContents = new DroolsRF2Validator().validateSnapshot(new FileInputStream(releaseFilePath), directoryOfRuleSetsPath, ruleSetNamesToRun);

		// Some extra output when running this main method in development -
		int outputSize = invalidContents.size() > 50 ? 50 : invalidContents.size();
		System.out.println("First 50 failures:");
		for (InvalidContent invalidContent : invalidContents.subList(0, outputSize)) {
			System.out.println(invalidContent);
		}

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
		}
		for (String errorMessage : failureCounts.keySet()) {
			System.out.println(failureCounts.get(errorMessage).toString() + " - " + errorMessage);
		}
	}

}
