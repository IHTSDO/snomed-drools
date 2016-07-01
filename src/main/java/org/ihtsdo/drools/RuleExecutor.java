package org.ihtsdo.drools;

import org.ihtsdo.drools.domain.*;
import org.ihtsdo.drools.exception.BadRequestRuleExecutorException;
import org.ihtsdo.drools.exception.RuleExecutorException;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.service.ConceptService;
import org.ihtsdo.drools.service.DescriptionService;
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

	private Map<String, KieContainer> ruleSetContainers;
	private int totalRulesLoaded = 0;

	private Logger logger = LoggerFactory.getLogger(getClass());
	private boolean failedToInitialize;
	private final KieServices kieServices;

	public RuleExecutor() {
		ruleSetContainers = new HashMap<>();
		kieServices = KieServices.Factory.get();
	}

	public RuleExecutor(String directoryOfRuleSets) {
		this();

		final File rulesDir = new File(directoryOfRuleSets);
		if (!rulesDir.isDirectory()) {
			failedToInitialize = true;
			logger.error("Rules directory does not exist: {}", rulesDir.getAbsolutePath());
		} else {
			for (File file : rulesDir.listFiles()) {
				if (file.isDirectory() && !file.isHidden()) {
					final String ruleSetName = file.getName();
					logger.info("Loading Drools rule set {}", ruleSetName);
					addRuleSet(file.getName(), file);
				}
			}
		}
	}

	public void addRuleSet(String ruleSetName, File ruleSetDirectory) throws RuleExecutorException {
		// Create the in-memory File System and add the resources files  to it
		KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

		try {
			final RuleLoader ruleLoader = new RuleLoader(kieFileSystem);
			Files.walkFileTree(ruleSetDirectory.toPath(), ruleLoader);
			int rulesLoaded = ruleLoader.getRulesLoaded();
			if (rulesLoaded == 0) {
				logger.warn("No rules loaded. Rules directory: {}", ruleSetDirectory.getAbsolutePath());
			} else {
				logger.info("{} rules loaded.", ruleLoader.getRulesLoaded());
				totalRulesLoaded += rulesLoaded;
			}
		} catch (IOException e) {
			throw new RuleExecutorException("Failed to load rule set " + ruleSetName, e);
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
		ruleSetContainers.put(ruleSetName, kieServices.newKieContainer(relId));
	}

	/**
	 * Validate a concept using drools rules available to this executor.
	 * A temporary identifier should be assigned to the concepts or any of it's descriptions and relationships
	 * if the component is new and does not yet have an SCTID. The identifier of the component is used to identify invalid content.
	 *
	 * Passing services in with every invocation of this method allows the implementation to capture content context. For example
	 * services relevant to the content branch being worked on.
	 * @param ruleSetNames The rule sets to use during validation.
	 * @param concept The concept to be validated.
	 * @param conceptService An implementation of the ConceptService class for use in validation rules.
	 * @param descriptionService An implementation of the DescriptionService class for use in validation rules.
	 * @param relationshipService An implementation of the RelationshipService class for use in validation rules.
	 * @param includePublishedComponents Include the published components of the given concept in results if found to be invalid.
	 *                                   Published content will be used during validation regardless just not returned.
	 * @param includeInferredRelationships Include the inferred relationships of the given concept during validation and
	 *                                     in results if found to be invalid.
	 * @return A list of content found to be invalid is returned.
	 */
	public List<InvalidContent> execute(Set<String> ruleSetNames, Concept concept, ConceptService conceptService, DescriptionService descriptionService, RelationshipService relationshipService,
			boolean includePublishedComponents, boolean includeInferredRelationships) {

		if (failedToInitialize) throw new RuleExecutorException("Unable to complete request: rule engine failed to initialize.");

		assertComponentIdsPresent(concept);

		Date start = new Date();
		Set<Component> components = new HashSet<>();

		// Load components into working set
		addConcept(components, concept, includeInferredRelationships);

		final List<InvalidContent> invalidContent = new ArrayList<>();
		for (String ruleSetName : ruleSetNames) {
			final KieContainer kieContainer = ruleSetContainers.get(ruleSetName);
			if (kieContainer == null) {
				throw new RuleExecutorException("Rule set not found for name '" + ruleSetName + "'");
			}
			final StatelessKieSession session = kieContainer.newStatelessKieSession();

			session.setGlobal("invalidContent", invalidContent);
			session.setGlobal("conceptService", conceptService);
			session.setGlobal("descriptionService", descriptionService);
			session.setGlobal("relationshipService", relationshipService);


			// Execute rules on working set
			session.execute(components);
			logger.debug("execute took {} milliseconds", new Date().getTime() - start.getTime());
		}

		if (!includePublishedComponents) {
			Set<InvalidContent> publishedInvalidContent = new HashSet<>();
			for (InvalidContent invalidContentItem : invalidContent) {
				if (!invalidContentItem.isIgnorePublishedCheck() && invalidContentItem.isPublished()) {
					publishedInvalidContent.add(invalidContentItem);
				}
			}
			invalidContent.removeAll(publishedInvalidContent);
		}

		return invalidContent;
	}

	private void assertComponentIdsPresent(Concept concept) {
		final String conceptId = concept.getId();
		if (conceptId == null || conceptId.isEmpty()) {
			throw new BadRequestRuleExecutorException("For validation concepts must have an SCTID or some temporary ID. " +
					"This also applies to the descriptions and relationships.");
		}
		for (Description description : concept.getDescriptions()) {
			if (description.getId() == null || description.getId().isEmpty()) {
				throw new BadRequestRuleExecutorException("For validation descriptions must have an SCTID or some temporary ID. " +
						"This also applies to the concept and relationships.");
			}
			if (!conceptId.equals(description.getConceptId())) {
				throw new BadRequestRuleExecutorException("For validation description conceptId must be the ID of the concept.");
			}
		}
		for (Relationship relationship : concept.getRelationships()) {
			if (relationship.getId() == null || relationship.getId().isEmpty()) {
				throw new BadRequestRuleExecutorException("For validation relationships must have an SCTID or some temporary ID. " +
						"This also applies to the concept and descriptions.");
			}
			if (!conceptId.equals(relationship.getSourceId())) {
				throw new BadRequestRuleExecutorException("For validation relationship sourceId must be the ID of the concept.");
			}
		}
	}

	private static void addConcept(Set<Component> components, Concept concept, boolean includeInferredRelationships) {
		components.add(concept);
		for (Description description : concept.getDescriptions()) {
			components.add(description);
		}
		for (Relationship relationship : concept.getRelationships()) {
			if (includeInferredRelationships || !Constants.INFERRED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				components.add(relationship);
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

	@SuppressWarnings("unused")
	public int getTotalRulesLoaded() {
		return totalRulesLoaded;
	}

}
