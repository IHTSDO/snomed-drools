package org.ihtsdo.drools.unittest;

import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.RuleExecutorException;
import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.rulestestrig.service.TestConceptService;
import org.ihtsdo.drools.rulestestrig.service.TestRelationshipService;
import org.ihtsdo.drools.unittest.domain.ConceptImpl;
import org.ihtsdo.drools.unittest.domain.DescriptionImpl;
import org.ihtsdo.drools.unittest.domain.RelationshipImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuleExecutorTest {

	private RuleExecutor ruleExecutor;
	private TestConceptService conceptService;
	private TestRelationshipService relationshipService;

	@Before
	public void setup() {
		final Map<String, Concept> concepts = new HashMap<>();
		conceptService = new TestConceptService(concepts);
		relationshipService = new TestRelationshipService(concepts);
		ruleExecutor = new RuleExecutor("src/test/resources/rules");
	}

	@Test
	public void testInitFailure() {
		final RuleExecutor ruleExecutor1 = new RuleExecutor("non-existant-directory");

		try {
			ruleExecutor1.execute(new ConceptImpl("1"), true, conceptService, relationshipService);
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

		final List<InvalidContent> invalidContent = ruleExecutor.execute(concept, true, conceptService, relationshipService);

		Assert.assertEquals(1, invalidContent.size());
		final InvalidContent invalidContent1 = invalidContent.get(0);
		Assert.assertEquals(concept.getId(), invalidContent1.getConceptId());
		Assert.assertEquals("Term should not contain double spaces.", invalidContent1.getMessage());
		Assert.assertEquals("2", invalidContent1.getComponentId());
	}

	@Test
	public void testExecuteNullConceptId() throws Exception {
		final Concept concept = new ConceptImpl(null)
				.addDescription(new DescriptionImpl("2", "a  "))
				.addRelationship(new RelationshipImpl("r1", "3"))
				.addRelationship(new RelationshipImpl("r2", "4"));

		final List<InvalidContent> invalidContent = ruleExecutor.execute(concept, true, conceptService, relationshipService);

		Assert.assertEquals(1, invalidContent.size());
		final InvalidContent invalidContent1 = invalidContent.get(0);
		Assert.assertEquals(concept.getId(), invalidContent1.getConceptId());
		Assert.assertEquals("Term should not contain double spaces.", invalidContent1.getMessage());
		Assert.assertEquals("2", invalidContent1.getComponentId());
	}
}
