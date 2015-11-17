package org.ihtsdo.drools.rulestestrig;

import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.rulestestrig.domain.TestConcept;
import org.ihtsdo.drools.rulestestrig.domain.TestRelationship;
import org.ihtsdo.drools.rulestestrig.service.TestConceptService;
import org.ihtsdo.drools.rulestestrig.service.TestRelationshipService;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RulesTestManual {

	public static final String GIVEN_CONCEPTS = "givenConcepts";
	public static final String ASSERT_CONCEPTS_PASS = "assertConceptsPass";
	public static final String ASSERT_CONCEPTS_FAIL = "assertConceptsFail";
	private File rulesDirectory;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Map<String, Concept> concepts;
	private TestConceptService conceptService;
	private TestRelationshipService relationshipService;

	@Before
	public void setup() {
		final String rulesPath = "rules"; // symlink to snomed-drools-rules
		rulesDirectory = new File(rulesPath);
		Assert.assertTrue(rulesDirectory.isDirectory());

		concepts = new HashMap<>();
		conceptService = new TestConceptService(concepts);
		relationshipService = new TestRelationshipService(concepts);
	}

	@Test
	public void testAllRules() {
		boolean anyErrors = false;
		for (File ruleGroupDirectory : rulesDirectory.listFiles(TestUtil.DIRECTORY_FILTER)) {
			for (File ruleDirectory : ruleGroupDirectory.listFiles(TestUtil.DIRECTORY_FILTER)) {
				logger.info("Testing {}", ruleDirectory.getName());
				try {
					final File[] ruleFiles = ruleDirectory.listFiles(TestUtil.RULE_FILE_FILTER);
					if (ruleFiles.length == 0) {
						continue;
					}

					final RuleExecutor ruleExecutor = new RuleExecutor(ruleDirectory.getAbsolutePath());

					final File testCasesFile = new File(ruleDirectory, "test-cases.json");
					if (testCasesFile.isFile()) {

						Map<String, List<TestConcept>> testConcepts = TestUtil.loadConceptMap(testCasesFile);
						setRelationshipSourceIds(testConcepts);

						final List<TestConcept> givenConcepts = testConcepts.get(GIVEN_CONCEPTS);
						if (givenConcepts != null) {
							for (TestConcept givenConcept : givenConcepts) {
								String id = givenConcept.getId();
								Assert.assertNotNull("Concepts in the set '" + GIVEN_CONCEPTS + "' must have an ID", id);
								concepts.put(id, givenConcept);
							}
						}

						final List<TestConcept> conceptsThatShouldPass = testConcepts.get(ASSERT_CONCEPTS_PASS);
						Assert.assertNotNull("The set of concepts '" + ASSERT_CONCEPTS_PASS + "' is required.", conceptsThatShouldPass);
						executeRulesAndAssertExpectations(ruleExecutor, conceptsThatShouldPass, true);

						final List<TestConcept> conceptsThatShouldFail = testConcepts.get(ASSERT_CONCEPTS_FAIL);
						Assert.assertNotNull("The set of concepts '" + ASSERT_CONCEPTS_FAIL + "' is required.", conceptsThatShouldPass);
						executeRulesAndAssertExpectations(ruleExecutor, conceptsThatShouldFail, false);
					}
				} catch (Exception e) {
					anyErrors = true;
					logger.error("Error testing {}", ruleDirectory.getName(), e);
				}
			}
		}
		Assert.assertFalse("There should be no errors while testing all rules.", anyErrors);
	}

	private void setRelationshipSourceIds(Map<String, List<TestConcept>> conceptListMap) {
		for (List<TestConcept> concepts : conceptListMap.values()) {
			for (TestConcept concept : concepts) {
				final Collection<TestRelationship> relationships = concept.getRelationships();
				for (TestRelationship relationship : relationships) {
					relationship.setSourceId(concept.getId());
				}
			}
		}
	}

	private void executeRulesAndAssertExpectations(RuleExecutor ruleExecutor, List<TestConcept> concepts, boolean expectPass) throws JSONException {
		for (TestConcept concept : concepts) {
			final List<InvalidContent> invalidContent = ruleExecutor.execute(concept, true, conceptService, relationshipService);
			if (expectPass) {
				Assert.assertEquals("A concept from the " + ASSERT_CONCEPTS_PASS + " set actually failed! " + invalidContent.toString(), 0, invalidContent.size());

			} else {
				Assert.assertNotEquals("A concept from the " + ASSERT_CONCEPTS_FAIL + " set actually passed! " + concept.toString(), 0, invalidContent.size());
			}
		}
	}

	public static void main(String[] args) {
		System.out.println("asdf(asdf".matches(".*[^ ]\\(.*"));
	}

}
