package org.ihtsdo.drools.validator.rf2;

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
import org.ihtsdo.otf.snomedboot.domain.Concept;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ComponentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.script.dao.SimpleStorageResourceLoader;
import org.springframework.util.Assert;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DroolsRF2Validator {

	public static final ManualResourceConfiguration BLANK_RESOURCES_CONFIGURATION =
			new ManualResourceConfiguration(true, false, new ResourceConfiguration.Local("classpath:blank-resource-files"), null);
	private static final String TAB = "\t";
	public static final String SCTIDS_REGEX = "[0-9,]+";

	public static final LoadingProfile LOADING_PROFILE = LoadingProfile.complete
			.withoutAllRefsets()
			.withIncludedReferenceSetFilenamePattern(".*_cRefset_.*Language.*")
			.withIncludedReferenceSetFilenamePattern(".*_cRefset_.*Association.*")
			.withIncludedReferenceSetFilenamePattern(".*_sRefset_.*OWL.*")
			.withEffectiveComponentFilter();

	private final RuleExecutor ruleExecutor;
	private final TestResourceProvider testResourceProvider;
	private static final Logger logger = LoggerFactory.getLogger(DroolsRF2Validator.class);

	public static void main(String[] args) throws IOException {
		if (args.length != 4 && args.length != 5 && args.length != 6) {
			// NOTE - Keep in sync with README.md file.
			logger.info("Usage: java -jar snomed-drools-rf2-*executable.jar <snomedDroolsRulesPath> <assertionGroup1,assertionGroup2,etc> " +
					"<extractedRF2FilesDirectories> <currentEffectiveTime> <includedModules(optional)> <previousReleaseRf2Directories(optional)>");
			System.exit(1);
		}

		// Resolve mandatory arguments
		String directoryOfRuleSetsPath = args[0];
		String commaSeparatedAssertionGroups = args[1];
		Set<String> assertionGroups = Sets.newHashSet(commaSeparatedAssertionGroups.split(","));
		Set<String> extractedRF2FilesDirectories = Sets.newHashSet(args[2].split(","));
		String currentEffectiveTime = args[3];
		if (!currentEffectiveTime.matches("\\d{8}")) {
			logger.info("Expecting <currentEffectiveTime> using format yyyymmdd");
			System.exit(1);
		}

		// Resolve optional arguments
		Set<String> includedModuleSets = null;
		Set<String> previousReleaseDirectories = null;
		if (args.length > 4) {
			String arg4 = args[4];
			if (arg4.matches(SCTIDS_REGEX)) {
				includedModuleSets = toModules(arg4);
			} else if (isDirectories(arg4)) {
				previousReleaseDirectories = Sets.newHashSet(arg4.split(","));
			}
			if (args.length > 5) {
				String arg5 = args[4];
				if (arg5.matches(SCTIDS_REGEX)) {
					includedModuleSets = toModules(arg5);
				} else if (isDirectories(arg5)) {
					previousReleaseDirectories = Sets.newHashSet(arg5.split(","));
				}
			}
		}

		new DroolsRF2Validator(directoryOfRuleSetsPath, true)
				.validate(assertionGroups, null, extractedRF2FilesDirectories, currentEffectiveTime, includedModuleSets, previousReleaseDirectories);
	}

	private static boolean isDirectories(String arg) {
		if (arg != null && !arg.isEmpty()) {
			final String[] dirs = arg.split(",");
			for (String dir : dirs) {
				if (!new File(dir).isDirectory()) {
					logger.error("Path {} is not a directory.", dir);
					return false;
				}
			}
			return dirs.length > 0;
		}
		return false;
	}

	/**
	 * Creates a validator with assertions loaded from disk but without any test resource files.
	 * @param directoryOfAssertionGroups Directory of assertion groups to load.
	 * @param loadTestResources If static resource files should be loaded, used by some tests.
	 */
	public DroolsRF2Validator(String directoryOfAssertionGroups, boolean loadTestResources) throws IOException {
		this(directoryOfAssertionGroups, getTestResourceManager(loadTestResources));
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

	private void validate(Set<String> assertionGroups, Set<String> excludedRules, Set<String> extractedRF2FilesDirectories,
			String currentEffectiveTime, Set<String> includedModuleSets, Set<String> previousReleaseDirectories) {

		File report = new File("validation-report-" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".txt");
		try {
			// Load test resources from public S3 location

			// Run assertions
			List<InvalidContent> invalidContents = validateRF2Files(extractedRF2FilesDirectories, previousReleaseDirectories, assertionGroups, excludedRules, currentEffectiveTime,
					includedModuleSets, false);

			// Write report
			boolean isCreated = report.createNewFile();
			if (!isCreated) {
				logger.error("Failed to create report file.");
				return;
			}
			try (BufferedWriter reportWriter = new BufferedWriter(new FileWriter(report))) {
				reportWriter.write("conceptId\tcomponentId\tmessage\tseverity\tignorePublishedCheck");
				reportWriter.newLine();

				for (InvalidContent invalidContent : invalidContents) {
					reportWriter.write(invalidContent.getConceptId());
					reportWriter.write(TAB);
					reportWriter.write(invalidContent.getComponentId());
					reportWriter.write(TAB);
					reportWriter.write(invalidContent.getMessage().replace("\n", " "));
					reportWriter.write(TAB);
					reportWriter.write(invalidContent.getSeverity().toString());
					reportWriter.write(TAB);
					reportWriter.write(invalidContent.isIgnorePublishedCheck() + "");
					reportWriter.newLine();
				}
			}
		} catch (ReleaseImportException | IOException e) {
			logger.error("Failed to validate.", e);
		}
	}

	private static Set<String> toModules(String arg) {
		return Arrays.stream(arg.split(",")).map(String::trim).collect(Collectors.toSet());
	}

	/**
	 *
	 * @param extractedRF2FilesDirectories	Paths to directories containing all extracted RF2 files. This can be the snapshot files of a single or multiple code systems and
	 *                                         can also include delta files for the new release.
	 * @param previousReleaseDirectories	Path to directories containing extracted RF2 files from the previous release. These are used to determine the released status of
	 *                                         components, that is used in some assertions.
	 * @param ruleSetNamesToRun				The assertion groups to run the rules of.
	 * @param excludedRules 				List of UUIDs to be excluded from the validation
	 * @param currentEffectiveTime			The current effectiveTime of the latest published files, used to determine the published flag on components.
	 * @param includedModules				Optional filter to validate components only in specific modules.
	 * @param activeConceptsOnly            Optional filter to return invalid content only for active concepts (ignore inactive concepts).
	 * @return								A collections of invalid content according to the rules within the assertion groups selected.
	 * @throws ReleaseImportException		Exception thrown when application fails to load the RF2 files to be validated.
	 */
	public List<InvalidContent> validateRF2Files(Set<String> extractedRF2FilesDirectories, Set<String> previousReleaseDirectories, Set <String> ruleSetNamesToRun,
												 Set<String> excludedRules, String currentEffectiveTime,
			Set<String> includedModules, boolean activeConceptsOnly) throws ReleaseImportException {

		long start = new Date().getTime();
		Assert.isTrue(ruleSetNamesToRun != null && !ruleSetNamesToRun.isEmpty(), "The name of at least one rule set must be specified.");

		PreviousReleaseComponentFactory previousReleaseComponentFactory = null;
		if (previousReleaseDirectories != null) {
			previousReleaseComponentFactory = loadPreviousReleaseComponentIds(previousReleaseDirectories);
		}

		logger.info("Loading components from RF2");
		SnomedDroolsComponentRepository repository = loadComponentsFromRF2(extractedRF2FilesDirectories, currentEffectiveTime, previousReleaseComponentFactory);
		logger.info("Components loaded");

		DroolsConceptService conceptService = new DroolsConceptService(repository);
		DroolsDescriptionService descriptionService = new DroolsDescriptionService(repository, conceptService, testResourceProvider);
		DroolsRelationshipService relationshipService = new DroolsRelationshipService(repository);

		Collection<DroolsConcept> concepts = repository.getConcepts();
		logger.info("Running tests");
		List<InvalidContent> invalidContents = ruleExecutor.execute(ruleSetNamesToRun, excludedRules, concepts, conceptService, descriptionService, relationshipService, true, true);
		invalidContents.addAll(repository.getComponentLoadingErrors());

		//Filter only invalid components that are in the specified modules list, if modules list is not specified, return all invalid components
		if(includedModules != null && !includedModules.isEmpty()) {
			String includedModulesStr = String.join(",", includedModules);
			logger.info("Filtering invalid contents for included module ids: {}", includedModulesStr);
			invalidContents = invalidContents.stream().filter(content -> includedModules.contains(content.getComponent().getModuleId())).collect(Collectors.toList());
		}

		// Filter only active concepts
		if (activeConceptsOnly) {
			invalidContents = invalidContents.stream().filter(c -> conceptService.isActive(c.getConceptId())).collect(Collectors.toList());
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

	private PreviousReleaseComponentFactory loadPreviousReleaseComponentIds(Set<String> previousReleaseDirectories) throws ReleaseImportException {
		ReleaseImporter importer = new ReleaseImporter();
		final PreviousReleaseComponentFactory componentFactory = new PreviousReleaseComponentFactory();
		importer.loadEffectiveSnapshotReleaseFiles(previousReleaseDirectories, LOADING_PROFILE, componentFactory, true);
		return componentFactory;
	}

	private SnomedDroolsComponentRepository loadComponentsFromRF2(Set<String> extractedRF2FilesDirectories, String currentEffectiveTime,
			PreviousReleaseComponentFactory previousReleaseComponentIds) throws ReleaseImportException {

		ReleaseImporter releaseImporter = new ReleaseImporter();

		boolean loadDelta = RF2ReleaseFilesUtil.anyDeltaFilesPresent(extractedRF2FilesDirectories);
		if (loadDelta) {
			logger.info("Delta files detected, validating combination of snapshot and delta.");
		} else {
			logger.info("No delta files detected, validating snapshot.");
		}

		SnomedDroolsComponentRepository repository = new SnomedDroolsComponentRepository();
		SnomedDroolsComponentFactory componentFactory = new SnomedDroolsComponentFactory(new ComponentStore(), repository, currentEffectiveTime, previousReleaseComponentIds);

		if (loadDelta) {
			releaseImporter.loadEffectiveSnapshotAndDeltaReleaseFiles(extractedRF2FilesDirectories, LOADING_PROFILE, componentFactory, false);
		} else {
			releaseImporter.loadEffectiveSnapshotReleaseFiles(extractedRF2FilesDirectories, LOADING_PROFILE, componentFactory, false);
		}

		final Map<Long, ? extends Concept> conceptMap = componentFactory.getComponentStore().getConcepts();
		repository.getConcepts().forEach(item -> {
			Concept concept = conceptMap.get(Long.parseLong(item.getId()));
			if (concept != null) {
				item.setStatedAncestorIds(concept.getStatedAncestorIds());
			}
		});
		return repository;
	}


	public RuleExecutor getRuleExecutor() {
		return ruleExecutor;
	}

	private static ResourceManager getTestResourceManager(boolean loadTestResources) throws IOException {
		if (loadTestResources) {
			Properties properties = new Properties();
			// Load bucket and path for test resources
			properties.load(DroolsRF2Validator.class.getResourceAsStream("/aws.properties"));

			ManualResourceConfiguration testResourcesConfiguration = new ManualResourceConfiguration(true, true, null,
					new ResourceConfiguration.Cloud(properties.getProperty("test-resources.cloud.bucket"), properties.getProperty("test-resources.cloud.path")));
			S3Client s3Client = S3Client.builder().region(DefaultAwsRegionProviderChain.builder().build().getRegion())
					.credentialsProvider(AnonymousCredentialsProvider.create()).build();
			return new ResourceManager(testResourcesConfiguration, new SimpleStorageResourceLoader(s3Client), s3Client);
		} else {
			return new ResourceManager(BLANK_RESOURCES_CONFIGURATION, null);
		}
	}

}
