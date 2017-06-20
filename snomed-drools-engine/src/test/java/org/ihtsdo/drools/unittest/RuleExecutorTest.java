package org.ihtsdo.drools.unittest;

import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.exception.BadRequestRuleExecutorException;
import org.ihtsdo.drools.exception.RuleExecutorException;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.rulestestrig.service.TestConceptService;
import org.ihtsdo.drools.rulestestrig.service.TestDescriptionService;
import org.ihtsdo.drools.rulestestrig.service.TestRelationshipService;
import org.ihtsdo.drools.unittest.domain.ConceptImpl;
import org.ihtsdo.drools.unittest.domain.DescriptionImpl;
import org.ihtsdo.drools.unittest.domain.RelationshipImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class RuleExecutorTest {

	public static final Set<String> RULE_SET_NAMES = Collections.singleton("Common");
	private RuleExecutor ruleExecutor;
	private TestConceptService conceptService;
	private TestDescriptionService descriptionService;
	private TestRelationshipService relationshipService;

	@Before
	public void setup() {
		final Map<String, Concept> concepts = new HashMap<>();
		conceptService = new TestConceptService(concepts);
		descriptionService = new TestDescriptionService(concepts);
		relationshipService = new TestRelationshipService(concepts);
		ruleExecutor = new RuleExecutor("src/test/resources/rules");
	}

	@Test
	public void testInitFailure() {
		final RuleExecutor ruleExecutor1 = new RuleExecutor("non-existant-directory");

		try {
			ruleExecutor1.execute(RULE_SET_NAMES, Collections.singleton(new ConceptImpl("1")), conceptService, descriptionService, relationshipService, true, false);
			Assert.fail("Should have thrown exception.");
		} catch (RuleExecutorException e) {
			// Pass
		}
	}

	@Test
	public void testExecute() throws Exception {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl("2", "a  "))
				.addRelationship(new RelationshipImpl("r1", "3"))
				.addRelationship(new RelationshipImpl("r2", "4"));

		final List<InvalidContent> invalidContent = ruleExecutor.execute(RULE_SET_NAMES, Collections.singleton(concept), conceptService, descriptionService, relationshipService, true, false);

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

		final List<InvalidContent> invalidContent = ruleExecutor.execute(RULE_SET_NAMES, Collections.singleton(concept), conceptService, descriptionService, relationshipService, false, false);

		Assert.assertEquals(0, invalidContent.size());
	}

	@Test
	public void testExecuteIgnorePublishedContentCheck() throws Exception {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl("2", "a").published().addToAcceptability("900000000000508004", "PREFERRED"));

		final List<InvalidContent> invalidContent = ruleExecutor.execute(RULE_SET_NAMES, Collections.singleton(concept), conceptService, descriptionService, relationshipService, false, false);

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

		ruleExecutor.execute(RULE_SET_NAMES, Collections.singleton(concept), conceptService, descriptionService, relationshipService, true, false);
	}

	@Test(expected = BadRequestRuleExecutorException.class)
	public void testExecuteNullDescriptionId() throws Exception {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl(null, "a  "))
				.addRelationship(new RelationshipImpl("r1", "3"))
				.addRelationship(new RelationshipImpl("r2", "4"));

		ruleExecutor.execute(RULE_SET_NAMES, Collections.singleton(concept), conceptService, descriptionService, relationshipService, true, false);
	}

	@Test(expected = BadRequestRuleExecutorException.class)
	public void testExecuteNullRelationshipId() throws Exception {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl("2", "a  "))
				.addRelationship(new RelationshipImpl("r1", "3"))
				.addRelationship(new RelationshipImpl(null, "4"));

		ruleExecutor.execute(RULE_SET_NAMES, Collections.singleton(concept), conceptService, descriptionService, relationshipService, true, false);
	}
}
