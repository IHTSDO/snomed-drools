package org.ihtsdo.drools;

import org.ihtsdo.drools.exception.RuleExecutorException;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class RuleExecutorFactory {

	private static final String RULE_FILENAME_EXTENSION = ".drl";

	private KieServices kieServices;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Map<String, KieContainer> assertionGroupContainers;
	private Map<String, Integer> assertionGroupRuleCounts;
	private String unitTestGroup;


	public RuleExecutor createRuleExecutor(String directoryOfAssertionGroups) {
		return createRuleExecutor(directoryOfAssertionGroups, null);
	}

	public RuleExecutor createRuleExecutor(String directoryOfAssertionGroups, String unitTestGroup) {
		// Create a new instance of the factory so we can use instance fields without changing the original factory
		RuleExecutorFactory ruleExecutorFactory = new RuleExecutorFactory();
		ruleExecutorFactory.unitTestGroup = unitTestGroup;
		return ruleExecutorFactory.doCreateRuleExecutor(directoryOfAssertionGroups);
	}

	private RuleExecutor doCreateRuleExecutor(String directoryOfAssertionGroups) {
		// Load assertions
		assertionGroupContainers = new HashMap<>();
		assertionGroupRuleCounts = new HashMap<>();
		kieServices = KieServices.Factory.get();

		loadAssertions(directoryOfAssertionGroups);

		return new RuleExecutor(assertionGroupContainers, assertionGroupRuleCounts);
	}

	private void loadAssertions(String directoryOfAssertionGroups) {
		// Load assertion groups
		final File groupsDir = new File(directoryOfAssertionGroups);
		File[] groupDirs = groupsDir.listFiles();
		if (!groupsDir.isDirectory() || groupDirs == null) {
			String message = String.format("Rules directory does not exist: %s", groupsDir.getAbsolutePath());
			logger.error(message);
			throw new RuleExecutorException(message);
		}
		if (unitTestGroup != null) {
			attemptLoadAssertionGroup(groupsDir, unitTestGroup);
		} else {
			for (File groupDir : groupDirs) {
				attemptLoadAssertionGroup(groupDir, groupDir.getName());
			}
		}
	}

	private void attemptLoadAssertionGroup(File groupDir, String assertionGroupName) {
		if (groupDir.isDirectory() && !groupDir.isHidden()) {
			logger.info("Loading Drools assertion group {}", assertionGroupName);
			addAssertionGroup(assertionGroupName, groupDir);
		}
	}

	private void addAssertionGroup(String assertionGroupName, File ruleSetDirectory) throws RuleExecutorException {
		// Create the in-memory File System and add the resources files  to it
		KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

		try {
			final RuleLoader ruleLoader = new RuleLoader(kieFileSystem);
			Files.walkFileTree(ruleSetDirectory.toPath(), ruleLoader);
			int rulesLoadedForGroup = ruleLoader.getRulesLoaded();
			if (rulesLoadedForGroup == 0) {
				logger.warn("No rules loaded for group {}. Rules directory: {}", assertionGroupName, ruleSetDirectory.getAbsolutePath());
			} else {
				logger.info("{} rules loaded for group {}.", rulesLoadedForGroup, assertionGroupName);
			}
			assertionGroupRuleCounts.put(assertionGroupName, rulesLoadedForGroup);
		} catch (IOException e) {
			throw new RuleExecutorException("Failed to load rule set " + assertionGroupName, e);
		}

		// Create the builder for the resources of the File System
		KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);

		// Build the KieBases
		kieBuilder.buildAll();

		// Check for errors
		if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
			throw new RuleExecutorException(kieBuilder.getResults().toString());
		}

		// Get the Release ID (mvn style: groupId, artifactId,version)
		ReleaseId relId = kieBuilder.getKieModule().getReleaseId();

		// Create the Container, wrapping the KieModule with the given ReleaseId
		assertionGroupContainers.put(assertionGroupName, kieServices.newKieContainer(relId));
	}

	private static final class RuleLoader extends SimpleFileVisitor<Path> {

		private final KieFileSystem kieFileSystem;
		private int rulesLoaded;

		RuleLoader(KieFileSystem kieFileSystem) {
			this.kieFileSystem = kieFileSystem;
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
			File file = path.toFile();
			if (file.isFile() && file.getName().endsWith(RULE_FILENAME_EXTENSION)) {
				kieFileSystem.write(ResourceFactory.newFileResource(file));
				rulesLoaded++;
			}

			return FileVisitResult.CONTINUE;
		}

		int getRulesLoaded() {
			return rulesLoaded;
		}
	}

}
