package org.ihtsdo.drools;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.service.DescriptionService;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
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
import java.util.*;

public class RuleExecutor {

	public static final String RULE_FILENAME_EXTENSION = ".drl";

	private final KieContainer kieContainer;
	private final DescriptionService descriptionService;

	private Logger logger = LoggerFactory.getLogger(getClass());
	private boolean failedToInitialize;

	public RuleExecutor(String rulesDirectory, DescriptionService descriptionService) {
		this.descriptionService = descriptionService;
		KieServices kieServices = KieServices.Factory.get();

		// Create the in-memory File System and add the resources files  to it
		KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
		final File rulesDir = new File(rulesDirectory);
		if (!rulesDir.isDirectory()) {
			failedToInitialize = true;
			logger.error("Rules directory does not exist: {}", rulesDir.getAbsolutePath());
		} else {
			try {
				final RuleLoader ruleLoader = new RuleLoader(kieFileSystem);
				Files.walkFileTree(rulesDir.toPath(), ruleLoader);
				if (ruleLoader.getRulesLoaded() == 0) {
					logger.warn("No rules loaded. Rules directory: {}", rulesDir.getAbsolutePath());
				} else {
					logger.info("{} rules loaded.", ruleLoader.getRulesLoaded());
				}
			} catch (IOException e) {
				throw new RuleExecutorException("Failed to load rules.", e);
			}
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
		kieContainer = kieServices.newKieContainer(relId);
	}

	public List<InvalidContent> execute(Concept concept) {
		if (failedToInitialize) throw new RuleExecutorException("Unable to complete request: rule engine failed to initialize.");

		final StatelessKieSession session = kieContainer.newStatelessKieSession();

		final List<InvalidContent> invalidContent = new ArrayList<>();
		session.setGlobal("invalidContent", invalidContent);
		session.setGlobal("descriptionService", descriptionService);

		Date start = new Date();
		Set<Object> content = new HashSet<>();
		addConcept(concept, content);
		session.execute(content);
		logger.debug("execute took {} milliseconds", new Date().getTime() - start.getTime());

		return invalidContent;
	}

	private static void addConcept(Concept concept, Set<Object> content) {
		content.add(concept);
		for (Description description : concept.getDescriptions()) {
			content.add(description);
		}
		for (Relationship relationship : concept.getRelationships()) {
			content.add(relationship);
		}
	}

	private static final class RuleLoader extends SimpleFileVisitor<Path> {

		private final KieFileSystem kieFileSystem;
		private int rulesLoaded;

		public RuleLoader(KieFileSystem kieFileSystem) {
			this.kieFileSystem = kieFileSystem;
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
			File file = path.toFile();
			if (file.isFile() && file.getName().endsWith(RULE_FILENAME_EXTENSION)) {
				kieFileSystem.write(ResourceFactory.newFileResource(file));
				rulesLoaded++;
			}

			return FileVisitResult.CONTINUE;
		}

		public int getRulesLoaded() {
			return rulesLoaded;
		}

	}
}
