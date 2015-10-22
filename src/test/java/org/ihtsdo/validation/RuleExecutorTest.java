package org.ihtsdo.validation;

import org.ihtsdo.validation.domain.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class RuleExecutorTest {

	private RuleExecutor ruleExecutor;

	@Before
	public void setup() {
		ruleExecutor = new RuleExecutor();
	}

	@Test
	public void testExecute() throws Exception {
		final Concept concept = new ConceptImpl("1")
				.addDescription(new DescriptionImpl("a  "))
				.addRelationship(new RelationshipImpl("3"))
				.addRelationship(new RelationshipImpl("4"));

		final List<InvalidContent> invalidContent = ruleExecutor.execute(concept);

		Assert.assertEquals(1, invalidContent.size());
		final InvalidContent invalidContent1 = invalidContent.get(0);
		Assert.assertEquals(concept, invalidContent1.getConcept());
		Assert.assertEquals("Term should not contain double spaces.", invalidContent1.getReason());
		Assert.assertEquals("Description{term='a  '}", invalidContent1.getDetail());
	}
}
