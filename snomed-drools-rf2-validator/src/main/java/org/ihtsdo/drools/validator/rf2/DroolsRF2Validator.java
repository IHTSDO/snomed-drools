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

}
