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
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.util.Assert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DroolsRF2Validator {

	public static final ManualResourceConfiguration BLANK_RESOURCES_CONFIGURATION =
			new ManualResourceConfiguration(true, false, new ResourceConfiguration.Local("classpath:blank-resource-files"), null);
	private static final String TAB = "\t";
	public static final String SCTIDS_REGEX = "[0-9,]+";

	public static final LoadingProfile LOADING_PROFILE = LoadingProfile.complete
			.withoutAllRefsets()
			.withIncludedReferenceSetFilenamePattern(".*_cRefset_Language.*")
			.withIncludedReferenceSetFilenamePattern(".*_OWL.*");

	private final RuleExecutor ruleExecutor;
	private final TestResourceProvider testResourceProvider;
	private final Logger logger = LoggerFactory.getLogger(DroolsRF2Validator.class);

	public static void main(String[] args) throws IOException {
		if (args.length != 4 && args.length != 5 && args.length != 6) {
			// NOTE - Keep in sync with README.md file.
			System.out.println("Usage: java -jar snomed-drools-rf2*.jar <snomedDroolsRulesPath> <assertionGroup1,assertionGroup2,etc> " +
					"<extractedRF2FilesDirectories> <currentEffectiveTime> <includedModules(optional)> <previousRf2Directory(optional)>");
			System.exit(1);
		}

		// Resolve mandatory arguments
		String directoryOfRuleSetsPath = args[0];
		String commaSeparatedAssertionGroups = args[1];
		Set<String> assertionGroups = Sets.newHashSet(commaSeparatedAssertionGroups.split(","));
		Set<String> extractedRF2FilesDirectories = Sets.newHashSet(args[2].split(","));
		String currentEffectiveTime = args[3];
		if (!currentEffectiveTime.matches("\\d{8}")) {
			System.out.println("Expecting <currentEffectiveTime> using format yyyymmdd");
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
				.validate(assertionGroups, extractedRF2FilesDirectories, currentEffectiveTime, includedModuleSets, previousReleaseDirectories);
	}

	private static boolean isDirectories(String arg) {
		if (arg != null && !arg.isEmpty()) {
			final String[] dirs = arg.split(",");
			for (String dir : dirs) {
				if (!new File(dir).isDirectory()) {
					System.err.printf("Path '%s' is not a directory.%n", dir);
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

	private void validate(Set<String> assertionGroups, Set<String> extractedRF2FilesDirectories,
			String currentEffectiveTime, Set<String> includedModuleSets, Set<String> previousReleaseDirectories) {

		File report = new File("validation-report-" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".txt");
		try {
			// Load test resources from public S3 location

			// Run assertions
			List<InvalidContent> invalidContents = validateRF2Files(extractedRF2FilesDirectories, previousReleaseDirectories, assertionGroups, currentEffectiveTime,
					includedModuleSets, false);

			// Write report
			report.createNewFile();
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
			e.printStackTrace();
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
	 * @param currentEffectiveTime			The current effectiveTime of the latest published files, used to determine the published flag on components.
	 * @param includedModules				Optional filter to validate components only in specific modules.
	 * @param activeConceptsOnly            Optional filter to return invalid content only for active concepts (ignore inactive concepts).
	 * @return								A collections of invalid content according to the rules within the assertion groups selected.
	 * @throws ReleaseImportException		Exception thrown when application fails to load the RF2 files to be validated.
	 */
	public List<InvalidContent> validateRF2Files(Set<String> extractedRF2FilesDirectories, Set<String> previousReleaseDirectories, Set <String> ruleSetNamesToRun,
			String currentEffectiveTime,
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
		DroolsDescriptionService descriptionService = new DroolsDescriptionService(repository, testResourceProvider);
		DroolsRelationshipService relationshipService = new DroolsRelationshipService(repository);

		Collection<DroolsConcept> concepts = repository.getConcepts();
		logger.info("Running tests");
		List<InvalidContent> invalidContents = ruleExecutor.execute(ruleSetNamesToRun, concepts, conceptService, descriptionService, relationshipService, true, false);
		invalidContents.addAll(repository.getComponentLoadingErrors());

		//Filter only invalid components that are in the specified modules list, if modules list is not specified, return all invalid components
		if(includedModules != null && !includedModules.isEmpty()) {
			logger.info("Filtering invalid contents for included module ids: {}", String.join(",", includedModules));
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
		importer.loadEffectiveSnapshotReleaseFiles(previousReleaseDirectories, LOADING_PROFILE, componentFactory);
		return componentFactory;
	}

	private SnomedDroolsComponentRepository loadComponentsFromRF2(Set<String> extractedRF2FilesDirectories, String currentEffectiveTime,
			PreviousReleaseComponentFactory previousReleaseComponentIds) throws ReleaseImportException {

		ReleaseImporter importer = new ReleaseImporter();

		boolean loadDelta = anyDeltaFilesPresent(extractedRF2FilesDirectories);
		if (loadDelta) {
			logger.info("Delta files detected, validating combination of snapshot and delta.");
		} else {
			logger.info("No delta files detected, validating snapshot.");
		}

		SnomedDroolsComponentRepository repository = new SnomedDroolsComponentRepository();
		SnomedDroolsComponentFactory componentFactory = new SnomedDroolsComponentFactory(repository, currentEffectiveTime, previousReleaseComponentIds);
		if (loadDelta) {
			importer.loadEffectiveSnapshotAndDeltaReleaseFiles(extractedRF2FilesDirectories, LOADING_PROFILE, componentFactory);
		} else {
			importer.loadEffectiveSnapshotReleaseFiles(extractedRF2FilesDirectories, LOADING_PROFILE, componentFactory);
		}

		return repository;
	}

	private boolean anyDeltaFilesPresent(Set<String> extractedRF2FilesDirectories) throws ReleaseImportException {
		boolean loadDelta = false;
		for (String extractedRF2FilesDirectory : extractedRF2FilesDirectories) {
			try (final Stream<Path> pathStream = Files.find(new File(extractedRF2FilesDirectory).toPath(), 50,
					(path, basicFileAttributes) -> path.toFile().getName().matches("x?(sct|rel)2_Concept_[^_]*Delta_.*.txt"))) {
				loadDelta = pathStream.findFirst().isPresent();
			} catch (IOException e) {
				throw new ReleaseImportException("Error while searching input files.", e);
			}
		}
		return loadDelta;
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

			// This uses anonymous access
			return new ResourceManager(testResourcesConfiguration, new SimpleStorageResourceLoader(AmazonS3ClientBuilder.standard().withRegion("us-east-1").build()));
		} else {
			return new ResourceManager(BLANK_RESOURCES_CONFIGURATION, null);
		}
	}

}
