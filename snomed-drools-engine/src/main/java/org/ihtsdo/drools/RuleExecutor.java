package org.ihtsdo.drools;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.drools.domain.*;
import org.ihtsdo.drools.exception.BadRequestRuleExecutorException;
import org.ihtsdo.drools.exception.RuleExecutorException;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.response.Severity;
import org.ihtsdo.drools.service.ConceptService;
import org.ihtsdo.drools.service.DescriptionService;
import org.ihtsdo.drools.service.RelationshipService;
import org.ihtsdo.drools.service.TestResourceProvider;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.script.dao.SimpleStorageResourceLoader;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RuleExecutor {


	private final Map<String, KieContainer> assertionGroupContainers;
	private final Map<String, Integer> assertionGroupRuleCounts;
	private boolean testResourcesEmpty;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected RuleExecutor(Map<String, KieContainer> assertionGroupContainers, Map<String, Integer> assertionGroupRuleCounts) {
		this.assertionGroupContainers = assertionGroupContainers;
		this.assertionGroupRuleCounts = assertionGroupRuleCounts;
	}

	/**
	 * TestResourceProvider should be created once and used by other services to load test resources such as the case significant words list.
	 * Calling this method again will load the resources again so can be useful if the resources change.
	 * @param resourceManager The resource manager to use to load the test resource files.
	 * @return A TestResourceProvider which uses the resourceManager.
	 * @throws RuleExecutorException if there is a problem loading the test resources.
	 */
	public TestResourceProvider newTestResourceProvider(ResourceManager resourceManager) throws RuleExecutorException {
		try {
			TestResourceProvider testResourceProvider = new TestResourceProvider(resourceManager);
			testResourcesEmpty = !testResourceProvider.isAnyResourcesLoaded();
			return testResourceProvider;
		} catch (IOException e) {
			testResourcesEmpty = true;
			throw new RuleExecutorException("Failed to load test resources.", e);
		}
	}

	public TestResourceProvider newTestResourceProvider(String awsKey, String awsSecretKey, String bucket, String path) throws RuleExecutorException {
		try {
			AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
					.withRegion("us-east-1")
					.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsKey, awsSecretKey)))
					.build();
			ManualResourceConfiguration resourceConfiguration = new ManualResourceConfiguration(true, true, null, new ResourceConfiguration.Cloud(bucket, path));
			ResourceManager resourceManager = new ResourceManager(resourceConfiguration, new SimpleStorageResourceLoader(amazonS3));
			TestResourceProvider testResourceProvider = new TestResourceProvider(resourceManager);
			testResourcesEmpty = !testResourceProvider.isAnyResourcesLoaded();
			return testResourceProvider;
		} catch (IOException e) {
			testResourcesEmpty = true;
			throw new RuleExecutorException("Failed to load test resources.", e);
		}
	}

	/**
	 * Validate a concept using drools rules available to this executor.
	 * A temporary identifier should be assigned to the concepts or any of it's descriptions and relationships
	 * if the component is new and does not yet have an SCTID. The identifier of the component is used to identify invalid content.
	 *
	 * Passing services in with every invocation of this method allows the implementation to capture content context. For example
	 * services relevant to the content branch being worked on.
	 * @param ruleSetNames The rule sets to use during validation.
	 * @param concepts The concepts to be validated.
	 * @param conceptService An implementation of the ConceptService class for use in validation rules.
	 * @param descriptionService An implementation of the DescriptionService class for use in validation rules.
	 * @param relationshipService An implementation of the RelationshipService class for use in validation rules.
	 * @param includePublishedComponents Include the published components of the given concept in results if found to be invalid.
	 *                                   Published content will be used during validation regardless just not returned.
	 * @param includeInferredRelationships Include the inferred relationships of the given concept during validation and
	 *                                     in results if found to be invalid.
	 * @return A list of content found to be invalid is returned.
	 */
	public List<InvalidContent> execute(
			Set<String> ruleSetNames,
			Collection<? extends Concept> concepts,
			ConceptService conceptService,
			DescriptionService descriptionService,
			RelationshipService relationshipService,
			boolean includePublishedComponents,
			boolean includeInferredRelationships) throws RuleExecutorException {

		for (Concept concept : concepts) {
			assertComponentIdsPresent(concept);
		}

		Date start = new Date();

		final List<List<InvalidContent>> sessionInvalidContent = new ArrayList<>();
		final List<InvalidContent> exceptionContents = new ArrayList<>();
		final Map<String, IntegrityIssueReport> integrityIssueReportMap = new HashMap<>();
		for (Concept concept : concepts) {
			IntegrityIssueReport report = findAllComponentsWithBadIntegrity(concept, conceptService);
			if (!report.isEmpty()) {
				integrityIssueReportMap.put(concept.getId(), report);
			}
		}
		if (!integrityIssueReportMap.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			for (String key : integrityIssueReportMap.keySet()) {
				IntegrityIssueReport report = integrityIssueReportMap.get(key);
				builder.append("Unable to find ");
				if (report.isRelationshipTypeNotFound() && report.isRelationshipTargetNotFound()) {
					builder.append("the relationship type ").append(report.getRelationshipWithNotFoundType()).append(" and relationship target ").append(report.getRelationshipWithNotFoundDestination());
				} else if (report.isRelationshipTypeNotFound()) {
					builder.append("the relationship type ").append(report.getRelationshipWithNotFoundType());
				} else if (report.isRelationshipTargetNotFound()) {
					builder.append("the relationship target ").append(report.getRelationshipWithNotFoundDestination());
				}
				builder.append(" for source concept ").append(key).append(". ");

			}
			throw new RuleExecutorException("Structural integrity issues. Details: " + builder.toString().trim());
		}
		for (String ruleSetName : ruleSetNames) {
			final KieContainer kieContainer = assertionGroupContainers.get(ruleSetName);
			if (kieContainer == null) {
				throw new RuleExecutorException("Rule set not found for name '" + ruleSetName + "'");
			}

			int threads = concepts.size() == 1 ? 1 : 10;
			ExecutorService executorService = Executors.newFixedThreadPool(threads);
			List<StatelessKieSession> sessions = new ArrayList<>();
			for (int s = 0; s < threads; s++) {
				ArrayList<InvalidContent> invalidContent = new ArrayList<>();// List per thread to avoid concurrency issues.
				sessionInvalidContent.add(invalidContent);
				sessions.add(newStatelessKieSession(kieContainer, conceptService, descriptionService, relationshipService, invalidContent));
			}
			List<Concept> conceptList = new ArrayList<>(concepts);
			List<Callable<String>> tasks = new ArrayList<>();
			String total = String.format("%,d", concepts.size());
			int i = 0;
			while (i < concepts.size()) {
				Set<Component> components = new HashSet<>();
				Concept concept = conceptList.get(i++);
				addConcept(components, concept, includeInferredRelationships);
				int sessionIndex = tasks.size();
				tasks.add(() -> {
					try {
						StatelessKieSession statelessKieSession = sessions.get(sessionIndex);
						statelessKieSession.execute(components);
						components.clear();
						// Clear memory use
						statelessKieSession.getKieBase().newKieSession();
					} catch (Exception e) {
						exceptionContents.add(new InvalidContent(concept.getId(),concept, "An error occurred while running concept validation. Technical detail: " + e.getMessage(), Severity.ERROR));
					}
					return null;
				});

				if (tasks.size() == threads) {
					runTasks(executorService, tasks);
					tasks.clear();
				}
				if (i % 10_000 == 0) {
					logger.info("Validated {} of {}", String.format("%,d", i), total);
				}
			}
			if (!tasks.isEmpty()) {
				runTasks(executorService, tasks);
			}
			executorService.shutdown();
			logger.info("Validated {} of {}", String.format("%,d", i), total);

			logger.info("Rule execution took {} seconds", (new Date().getTime() - start.getTime()) / 1000);
		}

		List<InvalidContent> invalidContent = sessionInvalidContent.stream().flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toList());
		invalidContent.addAll(exceptionContents);
		invalidContent = removeDuplicates(invalidContent);

		if (!includePublishedComponents) {
			Set<InvalidContent> publishedInvalidContent = new HashSet<>();
			for (InvalidContent invalidContentItem : invalidContent) {
				logger.info("invalidContentItem : {}, {}, {}, {}, {}", invalidContentItem.getConceptId(), invalidContentItem.isIgnorePublishedCheck(), invalidContentItem.isPublished(), invalidContentItem.getSeverity(), invalidContentItem.getMessage());
				if (!invalidContentItem.isIgnorePublishedCheck() && invalidContentItem.isPublished()) {
					publishedInvalidContent.add(invalidContentItem);
				}
			}
			invalidContent.removeAll(publishedInvalidContent);
		}

		if (testResourcesEmpty) {
			invalidContent.add(0, InvalidContent.getGeneralWarning("Test resources were not available so assertions like case significance and US specific terms " +
					"checks will not have run."));
		}

		return invalidContent;
	}

	private IntegrityIssueReport findAllComponentsWithBadIntegrity(Concept concept, ConceptService conceptService) {
		final List<String> relationshipWithNotFoundType = new ArrayList<>();
		final List<String> relationshipWithNotFoundDestination = new ArrayList<>();
		if (concept.isActive()) {
			for (Relationship relationship : concept.getRelationships()) {
				Concept typeConcept = conceptService.findById(relationship.getTypeId());
				if (typeConcept == null) {
					relationshipWithNotFoundType.add(relationship.getTypeId());
				}
				if (StringUtils.isEmpty(relationship.getConcreteValue())) {
					Concept destinationConcept = conceptService.findById(relationship.getDestinationId());
					if (destinationConcept == null) {
						relationshipWithNotFoundDestination.add(relationship.getDestinationId());
					}
				}
			}
		}
		return new IntegrityIssueReport(relationshipWithNotFoundType, relationshipWithNotFoundDestination);
	}

	private List<InvalidContent> removeDuplicates(List<InvalidContent> invalidContent) {
		List<InvalidContent> uniqueInvalidContent = new ArrayList<>();
		Map<String, Map<String, Set<String>>> conceptComponentMessageMap = new HashMap<>();
		for (InvalidContent content : invalidContent) {
			Map<String, Set<String>> componentMessages = conceptComponentMessageMap.computeIfAbsent(content.getConceptId(), s -> new HashMap<>());
			Set<String> messages = componentMessages.computeIfAbsent(content.getComponentId(), s -> new HashSet<>());
			if (messages.add(content.getMessage())) {
				uniqueInvalidContent.add(content);
			}
		}
		return uniqueInvalidContent;
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
		for (OntologyAxiom ontologyAxiom : concept.getOntologyAxioms()) {
			if (ontologyAxiom.getId() == null || ontologyAxiom.getId().isEmpty()) {
				throw new BadRequestRuleExecutorException("For validation ontology axioms must have an SCTID or some temporary ID.");
			}
			if (!conceptId.equals(ontologyAxiom.getReferencedComponentId())) {
				throw new BadRequestRuleExecutorException("For validation ontology axiom referencedComponentId must be the ID of the concept.");
			}
		}
	}

	private StatelessKieSession newStatelessKieSession(KieContainer kieContainer, ConceptService conceptService, DescriptionService descriptionService, RelationshipService relationshipService, List<InvalidContent> invalidContent) {
		final StatelessKieSession session = kieContainer.newStatelessKieSession();
		session.setGlobal("invalidContent", invalidContent);
		session.setGlobal("conceptService", conceptService);
		session.setGlobal("descriptionService", descriptionService);
		session.setGlobal("relationshipService", relationshipService);
		return session;
	}

	private void runTasks(ExecutorService executorService, List<Callable<String>> tasks) {
		try {
			executorService.invokeAll(tasks);
		} catch (InterruptedException e) {
			throw new RuleExecutorException("Validation tasks were interrupted.", e);
		}
	}

	private static void addConcept(Set<Component> components, Concept concept, boolean includeInferredRelationships) {
		components.add(concept);
		components.addAll(concept.getDescriptions());
		for (Relationship relationship : concept.getRelationships()) {
			if (includeInferredRelationships || !Constants.INFERRED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				components.add(relationship);
			}
		}
		components.addAll(concept.getOntologyAxioms());
	}

	public int getTotalRulesLoaded() {
		return assertionGroupRuleCounts.values().stream().mapToInt(Integer::intValue).sum();
	}

	public int getAssertionGroupRuleCount(String assertionGroupName) {
		return assertionGroupRuleCounts.getOrDefault(assertionGroupName, 0);
	}

	private static class IntegrityIssueReport {

		private List<String> relationshipWithNotFoundType;

		private List<String> relationshipWithNotFoundDestination;

		IntegrityIssueReport(List<String> relationshipWithNotFoundType, List<String> relationshipWithNotFoundDestination) {
			this.relationshipWithNotFoundType = relationshipWithNotFoundType;
			this.relationshipWithNotFoundDestination = relationshipWithNotFoundDestination;
		}

		public boolean isEmpty() {
			return (relationshipWithNotFoundType == null || relationshipWithNotFoundType.isEmpty()) &&
					(relationshipWithNotFoundDestination == null || relationshipWithNotFoundDestination.isEmpty());
		}

		public boolean isRelationshipTypeNotFound() {
			return relationshipWithNotFoundType != null && !relationshipWithNotFoundType.isEmpty();
		}

		public boolean isRelationshipTargetNotFound() {
			return relationshipWithNotFoundDestination != null && !relationshipWithNotFoundDestination.isEmpty();
		}

		public List<String> getRelationshipWithNotFoundType() {
			return relationshipWithNotFoundType;
		}

		public void setRelationshipWithNotFoundType(List<String> relationshipWithNotFoundType) {
			this.relationshipWithNotFoundType = relationshipWithNotFoundType;
		}

		public List<String> getRelationshipWithNotFoundDestination() {
			return relationshipWithNotFoundDestination;
		}

		public void setRelationshipWithNotFoundDestination(List<String> relationshipWithNotFoundDestination) {
			this.relationshipWithNotFoundDestination = relationshipWithNotFoundDestination;
		}
	}
}
