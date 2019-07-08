package org.ihtsdo.drools.validator.rf2;

import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.collect.Sets;
import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.RuleExecutorFactory;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.service.TestResourceProvider;
import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.service.DroolsConceptService;
import org.ihtsdo.drools.validator.rf2.service.DroolsDescriptionService;
import org.ihtsdo.drools.validator.rf2.service.DroolsRelationshipService;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.ImpotentComponentFactory;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.util.Assert;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static org.ihtsdo.drools.validator.rf2.SnomedDroolsComponentFactory.MRCM_ATTRIBUTE_DOMAIN_INTERNATIONAL_REFSET;

public class DroolsRF2Validator {

	public static final ManualResourceConfiguration BLANK_RESOURCES_CONFIGURATION =
			new ManualResourceConfiguration(true, false, new ResourceConfiguration.Local("classpath:blank-resource-files"), null);
	private static final String TAB = "\t";
	private final RuleExecutor ruleExecutor;
	private final TestResourceProvider testResourceProvider;
	private final Logger logger = LoggerFactory.getLogger(DroolsRF2Validator.class);

	public static void main(String[] args) {
		if (args.length != 4 && args.length != 5 && args.length != 6) {
			// NOTE - Keep in sync with README.md file.
			System.out.println("Usage: java -jar snomed-drools-rf2*.jar <snomedDroolsRulesPath> <assertionGroup1,assertionGroup2,etc> <rf2SnapshotDirectory> <currentEffectiveTime> <includedModules(optional)> <previousRf2Directory(optional)>");
			System.exit(1);
		}

		String directoryOfRuleSetsPath = args[0];

		String commaSeparatedAssertionGroups = args[1];
		String verticalBarSeparatedSnapshotAndDeltaDirectories = args[2];
		String deltaDirectory = null;
		String[] snapshotsAndDelta = verticalBarSeparatedSnapshotAndDeltaDirectories.split("\\|");
		if(snapshotsAndDelta.length == 2) {
			deltaDirectory = snapshotsAndDelta[1];
		}
		String snapshotDirectories = snapshotsAndDelta[0];
		String currentEffectiveTime = args[3];
		Set<String> includedModuleSets = null;
		if (args.length > 4) {
			String includedModules = args[4];
			if(includedModules != null && !includedModules.isEmpty()) {
				includedModuleSets = Sets.newHashSet(includedModules.split(","));
			}
		}

		if (!currentEffectiveTime.matches("\\d{8}")) {
			System.out.println("Expecting <currentEffectiveTime> using format yyyymmdd");
			System.exit(1);
		}

		String prevRf2Release = null;
		if(args.length > 5) {
			prevRf2Release = args[5];
		}

		File report = new File("validation-report-" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".txt");
		try {
			// Load test resources from public S3 location
			ResourceManager testResourcesResourceManager = getTestResourceManager();

			DroolsRF2Validator rf2Validator = new DroolsRF2Validator(directoryOfRuleSetsPath, testResourcesResourceManager);

			HashSet<String> ruleSetNamesToRun = Sets.newHashSet(commaSeparatedAssertionGroups.split(","));
			HashSet<String> snomedSnapshotDirectories = Sets.newHashSet(snapshotDirectories.split(","));
			List<InvalidContent> invalidContents = rf2Validator.validateSnapshots(snomedSnapshotDirectories, deltaDirectory, prevRf2Release, ruleSetNamesToRun, currentEffectiveTime, includedModuleSets);
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

	/**
	 * Creates a validator with assertions loaded from disk but without any test resource files.
	 * @param directoryOfAssertionGroups Directory of assertion groups to load.
	 */
	public DroolsRF2Validator(String directoryOfAssertionGroups) {
		this(directoryOfAssertionGroups, new ResourceManager(BLANK_RESOURCES_CONFIGURATION, null));
	}

	/**
	 * Creates a validator with assertions loaded from disk and test resources loaded from the testResourcesResourceManager.
	 * @param directoryOfAssertionGroups Directory of assertion groups to load.
	 * @param testResourcesResourceManager ResourceManager to load test resources from.
	 */
	public DroolsRF2Validator(String directoryOfAssertionGroups, ResourceManager testResourcesResourceManager) {
		Assert.isTrue(new File(directoryOfAssertionGroups).isDirectory(), "The rules directory is not accessible.");
		ruleExecutor = new RuleExecutorFactory().createRuleExecutor(directoryOfAssertionGroups);
		testResourceProvider = ruleExecutor.newTestResourceProvider(testResourcesResourceManager);
	}

	public List<InvalidContent> validateSnapshotStreams(Set<InputStream> snomedRf2EditionZips, InputStream snomedRf2DeltaZip, InputStream prevRf2ReleaseZip, Set<String> ruleSetNamesToRun, String currentEffectiveTime, Set<String> includedModules) throws ReleaseImportException {
		Set<String> directoryPaths = new HashSet<>();
		// Unzip RF2 archive for reuse
		for (InputStream snomedRf2EditionZip : snomedRf2EditionZips) {
			String snapshotDirectoryPath = new ReleaseImporter().unzipRelease(snomedRf2EditionZip, ReleaseImporter.ImportType.SNAPSHOT).getAbsolutePath();
			directoryPaths.add(snapshotDirectoryPath);
		}
		String deltaDirectory = null;
		if(snomedRf2DeltaZip != null) {
			deltaDirectory = new ReleaseImporter().unzipRelease(snomedRf2DeltaZip, ReleaseImporter.ImportType.DELTA).getAbsolutePath();
		}
		String prevReleaseDirectory = null;
		if(prevRf2ReleaseZip != null) {
			prevReleaseDirectory = new ReleaseImporter().unzipRelease(prevRf2ReleaseZip, ReleaseImporter.ImportType.SNAPSHOT).getAbsolutePath();
		}
		return validateSnapshots(directoryPaths, deltaDirectory, prevReleaseDirectory, ruleSetNamesToRun, currentEffectiveTime, includedModules);
	}


	public List<InvalidContent> validateSnapshots(Set<String> snomedRf2EditionDir, String snomedRf2DeltaZip, String prevReleaseDir, Set<String> ruleSetNamesToRun, String currentEffectiveTime, Set<String> includedModules) throws ReleaseImportException {
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

		LoadingProfile snapshotLoadingProfile = LoadingProfile.complete
				.withoutAllRefsets()
				.withIncludedReferenceSetFilenamePattern(".*_cRefset_Language.*")
				.withIncludedReferenceSetFilenamePattern(".*_OWL.*");

		PreviousReleaseComponentFactory previousReleaseComponentFactory = null;
		if(prevReleaseDir != null) {
			previousReleaseComponentFactory = new PreviousReleaseComponentFactory();
			Set<String> prevRelease = new HashSet<>();
			prevRelease.add(prevReleaseDir);
			importer.loadEffectiveSnapshotReleaseFiles(prevRelease, snapshotLoadingProfile, previousReleaseComponentFactory);
		}

		SnomedDroolsComponentFactory componentFactory = new SnomedDroolsComponentFactory(repository, currentEffectiveTime, previousReleaseComponentFactory);
		importer.loadEffectiveSnapshotReleaseFiles(snomedRf2EditionDir, snapshotLoadingProfile, componentFactory);
		if (componentFactory.isAxiomParsingError()) {
			throw new ReleaseImportException("Failed to parse one or more OWL Axioms. Check logs for details.");
		}

		List<InvalidContent> componentLoadingErrors = repository.getComponentLoadingErrors();

		if(snomedRf2DeltaZip != null) {
			logger.info("Start loading delta file...");
			LoadingProfile deltaLoadingProfile = snapshotLoadingProfile.withInactiveRelationships()
					.withInactiveRefsetMembers()
					.withFullDescriptionObjects();
			importer.loadDeltaReleaseFiles(snomedRf2DeltaZip, deltaLoadingProfile, componentFactory);
			logger.info("Completed loading delta file");
		}
		logger.info("Components loaded");

		DroolsConceptService conceptService = new DroolsConceptService(repository);
		DroolsDescriptionService descriptionService = new DroolsDescriptionService(repository, testResourceProvider);
		DroolsRelationshipService relationshipService = new DroolsRelationshipService(repository);

		Collection<DroolsConcept> concepts = repository.getConcepts();
		logger.info("Running tests");
		List<InvalidContent> invalidContents = ruleExecutor.execute(ruleSetNamesToRun, concepts, conceptService, descriptionService, relationshipService, true, false);
		invalidContents.addAll(componentLoadingErrors);

		//Filter only invalid components that are in the specified modules list, if modules list is not specified, return all invalid components
		if(includedModules != null && !includedModules.isEmpty()) {
			logger.info("Filtering invalid contents for included module ids: {}", String.join(",", includedModules));
			invalidContents = invalidContents.stream().filter(content -> includedModules.contains(content.getComponent().getModuleId())).collect(Collectors.toList());
		}

		// Add concept FSN to invalid contents
		for (InvalidContent invalidContent : invalidContents) {
			if(invalidContent.getConceptId() != null && !invalidContent.getConceptId().isEmpty()) {
				Set<String> fsnSet = descriptionService.getFSNs(Sets.newHashSet(invalidContent.getConceptId()));
				if(!fsnSet.isEmpty()) {
					invalidContent.setConceptFsn(fsnSet.iterator().next());
				}
			}
		}

		//Free resources after getting validation results
		repository.cleanup();
		descriptionService.getDroolsDescriptionIndex().cleanup();
		logger.info("invalidContent count {}", invalidContents.size());
		logger.info("Tests complete. Total run time {} seconds", (new Date().getTime() - start) / 1000);
		return invalidContents;
	}

	public RuleExecutor getRuleExecutor() {
		return ruleExecutor;
	}

	private static ResourceManager getTestResourceManager() throws IOException {
		Properties properties = new Properties();
		// Load bucket and path for test resources
		properties.load(DroolsRF2Validator.class.getResourceAsStream("/aws.properties"));

		ManualResourceConfiguration testResourcesConfiguration = new ManualResourceConfiguration(true, true, null,
				new ResourceConfiguration.Cloud(properties.getProperty("test-resources.cloud.bucket"), properties.getProperty("test-resources.cloud.path")));

		// This uses anonymous access
		return new ResourceManager(testResourcesConfiguration, new SimpleStorageResourceLoader(AmazonS3ClientBuilder.standard().withRegion("us-east-1").build()));
	}

}
