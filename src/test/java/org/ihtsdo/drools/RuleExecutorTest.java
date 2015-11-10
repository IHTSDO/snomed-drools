package org.ihtsdo.drools;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.ConceptImpl;
import org.ihtsdo.drools.domain.DescriptionImpl;
import org.ihtsdo.drools.domain.RelationshipImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class RuleExecutorTest {

	private RuleExecutor ruleExecutor;

	@Before
	public void setup() {
		ruleExecutor = new RuleExecutor("src/test/resources/rules");
	}

	@Test
	public void testExecute() throws Exception {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl("2", "a  "))
				.addRelationship(new RelationshipImpl("r1", "3"))
				.addRelationship(new RelationshipImpl("r2", "4"));

		final List<InvalidContent> invalidContent = ruleExecutor.execute(concept);

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

		final List<InvalidContent> invalidContent = ruleExecutor.execute(concept);

		Assert.assertEquals(1, invalidContent.size());
		final InvalidContent invalidContent1 = invalidContent.get(0);
		Assert.assertEquals(concept.getId(), invalidContent1.getConceptId());
		Assert.assertEquals("Term should not contain double spaces.", invalidContent1.getMessage());
		Assert.assertEquals("2", invalidContent1.getComponentId());
	}
}
