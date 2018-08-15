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
import org.ihtsdo.otf.snomedboot.factory.ImpotentComponentFactory;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentFactory.MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFSET;

public class DroolsRF2Validator {

	public static final String TAB = "\t";
	private final RuleExecutor ruleExecutor;
	private final Logger logger = LoggerFactory.getLogger(DroolsRF2Validator.class);

	public static void main(String[] args) {
		if (args.length != 4 && args.length != 5) {
			System.out.println("Usage: java -jar snomed-drools-rf2*.jar <snomedDroolsRulesPath> <assertionGroup1,assertionGroup2,etc> <rf2SnapshotDirectory> <currentEffectiveTime> <includedModules>");
			System.exit(1);
		}

		String directoryOfRuleSetsPath = args[0];
		DroolsRF2Validator rf2Validator = new DroolsRF2Validator(directoryOfRuleSetsPath);
		String commaSeparatedAssertionGroups = args[1];
		String commaSeparationSnomedSnapshotDirectory = args[2];
		String currentEffectiveTime = args[3];
		Set<String> includedModuleSets = null;
		try {
			String includedModules = args[4];
			if(includedModules != null && !includedModules.isEmpty()) {
				includedModuleSets = Sets.newHashSet(includedModules.split(","));
			}

		} catch (ArrayIndexOutOfBoundsException e) {
		}

		if (!currentEffectiveTime.matches("\\d{8}")) {
			System.out.println("Expecting <currentEffectiveTime> using format yyyymmdd");
			System.exit(1);
		}

		File report = new File("validation-report-" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".txt");
		try {
			HashSet<String> ruleSetNamesToRun = Sets.newHashSet(commaSeparatedAssertionGroups.split(","));
			HashSet<String> snomedSnapshotDirectories = Sets.newHashSet(commaSeparationSnomedSnapshotDirectory.split(","));
			List<InvalidContent> invalidContents = rf2Validator.validateSnapshots(snomedSnapshotDirectories, ruleSetNamesToRun, currentEffectiveTime, includedModuleSets);
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

	public List<InvalidContent> validateSnapshotStreams(Set<InputStream> snomedRf2EditionZips, Set<String> ruleSetNamesToRun, String currentEffectiveTime, Set<String> includedModules) throws ReleaseImportException {
		Set<String> directoryPaths = new HashSet<>();
		// Unzip RF2 archive for reuse
		for (InputStream snomedRf2EditionZip : snomedRf2EditionZips) {
			String snapshotDirectoryPath = new ReleaseImporter().unzipRelease(snomedRf2EditionZip, ReleaseImporter.ImportType.SNAPSHOT).getAbsolutePath();
			directoryPaths.add(snapshotDirectoryPath);
		}
		return validateSnapshots(directoryPaths, ruleSetNamesToRun, currentEffectiveTime, includedModules);
	}


	public List<InvalidContent> validateSnapshots(Set<String> snomedRf2EditionDir, Set<String> ruleSetNamesToRun, String currentEffectiveTime, Set<String> includedModules) throws ReleaseImportException {
		long start = new Date().getTime();
		Assert.isTrue(ruleSetNamesToRun != null && !ruleSetNamesToRun.isEmpty(), "The name of at least one rule set must be specified.");

		logger.info("Loading components from RF2");

		ReleaseImporter importer = new ReleaseImporter();

		// Load ungrouped attribute set from MRCM file
		Set<Long> ungroupedAttributes = new HashSet<>();
		LoadingProfile justMRCM = new LoadingProfile()
				.withIncludedReferenceSetFilenamePattern(".*_MRCMAttributeDomain.*");
		importer.loadEffectiveSnapshotReleaseFiles(snomedRf2EditionDir, justMRCM, new ImpotentComponentFactory() {
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
		importer.loadEffectiveSnapshotReleaseFiles(snomedRf2EditionDir, loadingProfile, componentFactory);
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
		//Free resources after getting validation results
		repository.cleanup();
		descriptionService.getDroolsDescriptionIndex().cleanup();
		logger.info("Tests complete. Total run time {} seconds", (new Date().getTime() - start) / 1000);

		//Filter only invalid components that are in the specified modules list, if modules list is not specified, return all invalid components
		if(includedModules != null && !includedModules.isEmpty()) {
			logger.info("Filtering invalid contents for included module ids: {}", String.join(",", includedModules));
			invalidContents = invalidContents.stream().filter(content -> includedModules.contains(content.getComponent().getModuleId())).collect(Collectors.toList());
		}
		logger.info("invalidContent count {}", invalidContents.size());
		return invalidContents;
	}

	public RuleExecutor getRuleExecutor() {
		return ruleExecutor;
	}

}
