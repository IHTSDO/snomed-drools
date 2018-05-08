package org.ihtsdo.drools.validator.rf2;

import com.google.common.collect.Sets;
import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.domain.Component;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.response.Severity;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.service.DroolsConceptService;
import org.ihtsdo.drools.validator.rf2.service.DroolsDescriptionService;
import org.ihtsdo.drools.validator.rf2.service.DroolsRelationshipService;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.ImpotentComponentFactory;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Long.parseLong;
import static org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentFactory.MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFSET;

public class DroolsRF2Validator {

	public static final String TAB = "\t";
	private final RuleExecutor ruleExecutor;
	private final Logger logger = LoggerFactory.getLogger(DroolsRF2Validator.class);

	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("Usage: java -jar snomed-drools-rf2*.jar <snomedDroolsRulesPath> <assertionGroup1,assertionGroup2,etc> <currentEffectiveTime> <rf2SnapshotDirectory>");
			System.exit(1);
		}

		String directoryOfRuleSetsPath = args[0];
		DroolsRF2Validator rf2Validator = new DroolsRF2Validator(directoryOfRuleSetsPath);
		String commaSeparatedAssertionGroups = args[1];
		String snomedSnapshotDirectory = args[2];
		String currentEffectiveTime = args[3];
		if (!currentEffectiveTime.matches("\\d{8}")) {
			System.out.println("Expecting <currentEffectiveTime> using format yyyymmdd");
			System.exit(1);
		}

		File report = new File("validation-report-" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".txt");
		try {
			HashSet<String> ruleSetNamesToRun = Sets.newHashSet(commaSeparatedAssertionGroups.split(","));
			List<InvalidContent> invalidContents = rf2Validator.validateSnapshot(snomedSnapshotDirectory, ruleSetNamesToRun, currentEffectiveTime);
			report.createNewFile();
			try (BufferedWriter reportWriter = new BufferedWriter(new FileWriter(report))) {
				reportWriter.write("conceptId\tcomponentId\tmessage\tseverity\tignorePublishedCheck");
				reportWriter.newLine();

				for (InvalidContent invalidContent : invalidContents) {
					reportWriter.write(invalidContent.getConceptId());
					reportWriter.write(TAB);
					reportWriter.write(invalidContent.getComponentId());
					reportWriter.write(TAB);
					reportWriter.write(invalidContent.getMessage());
					reportWriter.write(TAB);
					reportWriter.write(invalidContent.getSeverity().toString());
					reportWriter.write(TAB);
					reportWriter.write(invalidContent.isIgnorePublishedCheck() + "");
					reportWriter.newLine();
				}
			}
		} catch (ReleaseImportException | IOException e) {
			e.printStackTrace();
		}
	}

	public DroolsRF2Validator(String directoryOfRuleSetsPath) {
		Assert.isTrue(new File(directoryOfRuleSetsPath).isDirectory(), "The rules directory is not accessible.");
		ruleExecutor = new RuleExecutor(directoryOfRuleSetsPath);
	}

	public List<InvalidContent> validateSnapshot(InputStream snomedRf2EditionZip, Set<String> ruleSetNamesToRun, String currentEffectiveTime) throws ReleaseImportException {
		// Unzip RF2 archive for reuse
		String snapshotDirectoryPath = new ReleaseImporter().unzipRelease(snomedRf2EditionZip, ReleaseImporter.ImportType.SNAPSHOT).getAbsolutePath();
		return validateSnapshot(snapshotDirectoryPath, ruleSetNamesToRun, currentEffectiveTime);
	}

	public List<InvalidContent> validateSnapshot(String snomedRf2EditionDir, Set<String> ruleSetNamesToRun, String currentEffectiveTime) throws ReleaseImportException {
		long start = new Date().getTime();
		Assert.isTrue(ruleSetNamesToRun != null && !ruleSetNamesToRun.isEmpty(), "The name of at least one rule set must be specified.");

		logger.info("Loading components from RF2");

		ReleaseImporter importer = new ReleaseImporter();

		// Load ungrouped attribute set from MRCM file
		Set<Long> ungroupedAttributes = new HashSet<>();
		LoadingProfile justMRCM = new LoadingProfile()
				.withIncludedReferenceSetFilenamePattern(".*_MRCMAttributeDomain.*");
		importer.loadSnapshotReleaseFiles(snomedRf2EditionDir, justMRCM, new ImpotentComponentFactory() {
			@Override
			public void newReferenceSetMemberState(String[] fieldNames, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... otherValues) {
				if ("1".equals(active) && refsetId.equals(MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFSET)) {
					// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	domainId	grouped	attributeCardinality	attributeInGroupCardinality	ruleStrengthId	contentTypeId
					// 																otherValues .. 	0			1		2						3							4				5

					// Ungrouped attribute
					if ("0".equals(otherValues[1])) {
						ungroupedAttributes.add(parseLong(referencedComponentId));
					}
				}
			}
		});

		// Load all other components
		SnomedDroolsComponentRepository repository = new SnomedDroolsComponentRepository();
		repository.getUngroupedAttributes().addAll(ungroupedAttributes);

		LoadingProfile loadingProfile = LoadingProfile.complete
				.withoutAllRefsets()
				.withIncludedReferenceSetFilenamePattern(".*_cRefset_Language.*")
				.withIncludedReferenceSetFilenamePattern(".*_OWLAxiom.*");

		SnomedDroolsComponentFactory componentFactory = new SnomedDroolsComponentFactory(repository, currentEffectiveTime);
		importer.loadSnapshotReleaseFiles(snomedRf2EditionDir, loadingProfile, componentFactory);
		if (componentFactory.isAxiomParsingError()) {
			throw new ReleaseImportException("Failed to parse one or more OWL Axioms. Check logs for details.");
		}

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
