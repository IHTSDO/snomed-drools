package org.ihtsdo.drools.rulestestrig;

import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.domain.Concept;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RulesTestManual {

	public static final String GIVEN_CONCEPTS = "givenConcepts";
	public static final String ASSERT_CONCEPTS_PASS = "assertConceptsPass";
	public static final String ASSERT_CONCEPTS_FAIL = "assertConceptsFail";
	private File rulesDirectory;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Before
	public void setup() {
		final String rulesPath = "rules"; // symlink to snomed-drools-rules
		rulesDirectory = new File(rulesPath);
		Assert.assertTrue(rulesDirectory.isDirectory());
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

					final TestDescriptionService descriptionService = new TestDescriptionService();
					final RuleExecutor ruleExecutor = new RuleExecutor(ruleDirectory.getAbsolutePath(), descriptionService);

					final File testCasesFile = new File(ruleDirectory, "test-cases.json");
					if (testCasesFile.isFile()) {

						Map<String, List<TestConcept>> testConcepts = TestUtil.loadConceptMap(testCasesFile);

						final List<TestConcept> givenConcepts = testConcepts.get(GIVEN_CONCEPTS);
						if (givenConcepts != null) {
							descriptionService.addTestConcepts(new ArrayList<Concept>(givenConcepts));
						}

						final List<TestConcept> conceptsThatShouldPass = testConcepts.get(ASSERT_CONCEPTS_PASS);
						executeRulesAndAssertExpectations(ruleExecutor, conceptsThatShouldPass, true);

						final List<TestConcept> conceptsThatShouldFail = testConcepts.get(ASSERT_CONCEPTS_FAIL);
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

	private void executeRulesAndAssertExpectations(RuleExecutor ruleExecutor, List<TestConcept> concepts, boolean expectPass) throws JSONException {
		for (TestConcept concept : concepts) {
			final List<InvalidContent> invalidContent = ruleExecutor.execute(concept);
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
