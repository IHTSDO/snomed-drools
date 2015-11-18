package org.ihtsdo.drools;

import org.ihtsdo.drools.domain.*;
import org.ihtsdo.drools.exception.BadRequestRuleExecutorException;
import org.ihtsdo.drools.exception.RuleExecutorException;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.service.ConceptService;
import org.ihtsdo.drools.service.RelationshipService;
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
	private int rulesLoaded = 0;

	private Logger logger = LoggerFactory.getLogger(getClass());
	private boolean failedToInitialize;

	public RuleExecutor(String rulesDirectory) {
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
				rulesLoaded = ruleLoader.getRulesLoaded();
				if (rulesLoaded == 0) {
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

	/**
	 * Validate a concept using drools rules available to this executor.
	 * A temporary identifier should be assigned to the concepts or any of it's descriptions and relationships
	 * if the component is new and does not yet have an SCTID. The identifier of the component is used to identify invalid content.
	 *
	 * Passing services in with every invocation of this method allows the implementation to capture content context. For example
	 * services relevant to the content branch being worked on.
	 * @param concept The concept to be validated.
	 * @param conceptService An implementation of the ConceptService class for use in validation rules.
	 * @param relationshipService An implementation of the RelationshipService class for use in validation rules.
	 * @param includePublishedComponents Include the published components of the given concept in results if found to be invalid.
	 *                                   Published content will be used during validation regardless just not returned.
	 * @param includeInferredRelationships Include the inferred relationships of the given concept during validation and
	 *                                     in results if found to be invalid.
	 * @return A list of content found to be invalid is returned.
	 */
	public List<InvalidContent> execute(Concept concept, ConceptService conceptService, RelationshipService relationshipService,
			boolean includePublishedComponents, boolean includeInferredRelationships) {
		if (failedToInitialize) throw new RuleExecutorException("Unable to complete request: rule engine failed to initialize.");

		assertComponentIdsPresent(concept);

		final StatelessKieSession session = kieContainer.newStatelessKieSession();

		final List<InvalidContent> invalidContent = new ArrayList<>();
		session.setGlobal("invalidContent", invalidContent);
		session.setGlobal("conceptService", conceptService);
		session.setGlobal("relationshipService", relationshipService);

		Date start = new Date();
		Set<Object> content = new HashSet<>();
		addConcept(concept, content, includeInferredRelationships);
		session.execute(content);
		logger.debug("execute took {} milliseconds", new Date().getTime() - start.getTime());

		if (!includePublishedComponents) {
			Set<InvalidContent> publishedInvalidContent = new HashSet<>();
			for (InvalidContent invalidContentItem : invalidContent) {
				if (invalidContentItem.isPublished()) {
					publishedInvalidContent.add(invalidContentItem);
				}
			}
			invalidContent.removeAll(publishedInvalidContent);
		}

		return invalidContent;
	}

	private void assertComponentIdsPresent(Concept concept) {
		assertComponentIdPresent(concept);
		for (Description description : concept.getDescriptions()) {
			assertComponentIdPresent(description);
		}
		for (Relationship relationship : concept.getRelationships()) {
			assertComponentIdPresent(relationship);
		}
	}

	private void assertComponentIdPresent(Component component) {
		if (component.getId() == null || component.getId().isEmpty()) {
			throw new BadRequestRuleExecutorException("All components to be validated must have an SCTID or any temporary ID. " +
					"This includes the concept, descriptions and relationships.");
		}
	}

	@SuppressWarnings("unused")
	public int getRulesLoaded() {
		return rulesLoaded;
	}

	private static void addConcept(Concept concept, Set<Object> content, boolean includeInferredRelationships) {
		content.add(concept);
		for (Description description : concept.getDescriptions()) {
			content.add(description);
		}
		for (Relationship relationship : concept.getRelationships()) {
			if (includeInferredRelationships || !Constants.INFERRED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				content.add(relationship);
			}
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
