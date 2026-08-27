package org.ihtsdo.drools.unittest;

import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.RuleExecutorFactory;
import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.exception.BadRequestRuleExecutorException;
import org.ihtsdo.drools.exception.RuleExecutorException;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.rulestestrig.service.TestConceptService;
import org.ihtsdo.drools.rulestestrig.service.TestDescriptionService;
import org.ihtsdo.drools.rulestestrig.service.TestRelationshipService;
import org.ihtsdo.drools.service.TestResourceProvider;
import org.ihtsdo.drools.unittest.domain.ConceptImpl;
import org.ihtsdo.drools.unittest.domain.DescriptionImpl;
import org.ihtsdo.drools.unittest.domain.RelationshipImpl;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class RuleExecutorTest {

	private static final Set<String> RULE_SET_NAMES = Collections.singleton("Common");
	private RuleExecutor ruleExecutor;
	private TestConceptService conceptService;
	private TestDescriptionService descriptionService;
	private TestRelationshipService relationshipService;

	@Before
	public void setup() {
		ruleExecutor = new RuleExecutorFactory().createRuleExecutor("src/test/resources/rules");
		ManualResourceConfiguration resourceConfiguration = new ManualResourceConfiguration(true, false,
				new ResourceConfiguration.Local("src/test/resources/dummy-test-resources"), null);
		TestResourceProvider testResourceProvider = ruleExecutor.newTestResourceProvider(new ResourceManager(resourceConfiguration, null));
		final Map<String, Concept> concepts = new HashMap<>();
		conceptService = new TestConceptService(concepts);
		descriptionService = new TestDescriptionService(concepts, testResourceProvider);
		relationshipService = new TestRelationshipService(concepts);
	}

	@Test(expected = RuleExecutorException.class)
	public void testInitFailure() {
		new RuleExecutorFactory().createRuleExecutor("non-existant-directory");
	}

	@Test
	public void testExecute() {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl("2", "a  "))
				.addRelationship(new RelationshipImpl("r1", "3"))
				.addRelationship(new RelationshipImpl("r2", "4"));

		final List<InvalidContent> invalidContent = ruleExecutor.execute(RULE_SET_NAMES, null, Collections.singleton(concept), conceptService, descriptionService, relationshipService, true, false);

		Assert.assertEquals(1, invalidContent.size());
		final InvalidContent invalidContent1 = invalidContent.get(0);
		Assert.assertEquals(concept.getId(), invalidContent1.getConceptId());
		Assert.assertEquals("Term should not contain double spaces.", invalidContent1.getMessage());
		Assert.assertEquals("2", invalidContent1.getComponentId());
	}

	@Test
	public void testExecuteOnlyUnpublishedContent() throws Exception {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl("2", "a  ").published())
				.addRelationship(new RelationshipImpl("r1", "3"))
				.addRelationship(new RelationshipImpl("r2", "4"));

		final List<InvalidContent> invalidContent = ruleExecutor.execute(RULE_SET_NAMES, null, Collections.singleton(concept), conceptService, descriptionService, relationshipService, false, false);

		Assert.assertEquals(0, invalidContent.size());
	}

	@Test
	public void testExecuteIgnorePublishedContentCheck() throws Exception {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl("2", "a").published().addToAcceptability("900000000000508004", "PREFERRED"));

		final List<InvalidContent> invalidContent = ruleExecutor.execute(RULE_SET_NAMES, null, Collections.singleton(concept), conceptService, descriptionService, relationshipService, false, false);

		Assert.assertEquals(1, invalidContent.size());
		final InvalidContent invalidContent1 = invalidContent.get(0);
		Assert.assertEquals(concept.getId(), invalidContent1.getConceptId());
		Assert.assertEquals("Term should have acceptability entries in one dialect.", invalidContent1.getMessage());
		Assert.assertEquals("2", invalidContent1.getComponentId());
	}

	@Test(expected = BadRequestRuleExecutorException.class)
	public void testExecuteNullConceptId() throws Exception {
		final Concept concept = new ConceptImpl(null)
				.addDescription(new DescriptionImpl("2", "a  "))
				.addRelationship(new RelationshipImpl("r1", "3"))
				.addRelationship(new RelationshipImpl("r2", "4"));

		ruleExecutor.execute(RULE_SET_NAMES, null, Collections.singleton(concept), conceptService, descriptionService, relationshipService, true, false);
	}

	@Test(expected = BadRequestRuleExecutorException.class)
	public void testExecuteNullDescriptionId() throws Exception {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl(null, "a  "))
				.addRelationship(new RelationshipImpl("r1", "3"))
				.addRelationship(new RelationshipImpl("r2", "4"));

		ruleExecutor.execute(RULE_SET_NAMES, null, Collections.singleton(concept), conceptService, descriptionService, relationshipService, true, false);
	}

	@Test(expected = BadRequestRuleExecutorException.class)
	public void testExecuteNullRelationshipId() throws Exception {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl("2", "a  "))
				.addRelationship(new RelationshipImpl("r1", "3"))
				.addRelationship(new RelationshipImpl(null, "4"));

		ruleExecutor.execute(RULE_SET_NAMES, null, Collections.singleton(concept), conceptService, descriptionService, relationshipService, true, false);
	}

	@Test
	public void testExecuteWithExcludedRules() {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl("2", "a  "))
				.addRelationship(new RelationshipImpl("r1", "3"))
				.addRelationship(new RelationshipImpl("r2", "4"));

		List<InvalidContent> invalidContents = ruleExecutor.execute(RULE_SET_NAMES, null, Collections.singleton(concept), conceptService, descriptionService, relationshipService, true, false);

		Assert.assertEquals(1, invalidContents.size());
		final InvalidContent invalidContent = invalidContents.get(0);
		Assert.assertEquals(concept.getId(), invalidContent.getConceptId());
		Assert.assertEquals("Term should not contain double spaces.", invalidContent.getMessage());
		Assert.assertEquals("2", invalidContent.getComponentId());

		Set<String> excludedRules = Collections.singleton("d04c89b7-962c-4dbc-ac5e-0033e808e913");
		invalidContents = ruleExecutor.execute(RULE_SET_NAMES, excludedRules, Collections.singleton(concept), conceptService, descriptionService, relationshipService, true, false);
		Assert.assertEquals(0, invalidContents.size());
	}

	@Test
	public void defaultsWorkerCountToAvailableProcessors() {
		Assert.assertEquals(Runtime.getRuntime().availableProcessors(), ruleExecutor.getValidationThreads());
	}

	@Test
	public void honoursAnOverriddenWorkerCount() {
		ruleExecutor.setValidationThreads(3);
		Assert.assertEquals(3, ruleExecutor.getValidationThreads());

		final Concept concept = new ConceptImpl("1").addDescription(new DescriptionImpl("2", "a  "));
		final List<InvalidContent> invalidContent = ruleExecutor.execute(RULE_SET_NAMES, null,
				Collections.singleton(concept), conceptService, descriptionService, relationshipService, true, false);

		Assert.assertEquals(1, invalidContent.size());
	}

	@Test
	public void findsTheSameContentWhateverTheWorkerCount() {
		List<Concept> concepts = new ArrayList<>();
		for (int i = 1; i <= 25; i++) {
			concepts.add(new ConceptImpl(String.valueOf(i)).addDescription(new DescriptionImpl("90" + i, "term  " + i)));
		}

		Set<String> onOneWorker = messages(ruleExecutor.execute(RULE_SET_NAMES, null, concepts,
				conceptService, descriptionService, relationshipService, true, false, 1));
		Set<String> onManyWorkers = messages(ruleExecutor.execute(RULE_SET_NAMES, null, concepts,
				conceptService, descriptionService, relationshipService, true, false, 16));

		Assert.assertEquals(25, onOneWorker.size());
		Assert.assertEquals(onOneWorker, onManyWorkers);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsAWorkerCountBelowOne() {
		ruleExecutor.setValidationThreads(0);
	}

	private Set<String> messages(List<InvalidContent> invalidContent) {
		Set<String> messages = new HashSet<>();
		for (InvalidContent content : invalidContent) {
			messages.add(content.getConceptId() + "|" + content.getComponentId() + "|" + content.getMessage());
		}
		return messages;
	}
}
