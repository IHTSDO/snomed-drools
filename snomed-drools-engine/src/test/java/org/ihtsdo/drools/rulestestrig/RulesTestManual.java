package org.ihtsdo.drools.rulestestrig;

import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.RuleExecutorFactory;
import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.OntologyAxiom;
import org.ihtsdo.drools.exception.RuleExecutorException;
import org.ihtsdo.drools.response.InvalidContent;
import org.ihtsdo.drools.rulestestrig.domain.*;
import org.ihtsdo.drools.rulestestrig.service.TestConceptService;
import org.ihtsdo.drools.rulestestrig.service.TestDescriptionService;
import org.ihtsdo.drools.rulestestrig.service.TestRelationshipService;
import org.ihtsdo.drools.service.TestResourceProvider;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

@RunWith(Parameterized.class)
public class RulesTestManual {

	private static final String GIVEN_CONCEPTS = "givenConcepts";
	private static final String ASSERT_CONCEPTS_PASS = "assertConceptsPass";
	private static final String ASSERT_CONCEPTS_FAIL = "assertConceptsFail";
	
	private RuleExecutor ruleExecutor;
	private Map<String, Concept> concepts;
	private Map<String, List<TestConcept<TestDescription, TestRelationship>>> testConcepts;
	
	private TestConceptService conceptService;
	private TestDescriptionService descriptionService;
	private TestRelationshipService relationshipService;

	@Parameters(name = "{0}")
	public static Iterable<? extends Object> data() {
		final String rulesPath = "../../snomed-drools-rules"; // relative path to snomed-drools-rules, either check out to here or use symlink
		final File rulesDirectory = new File(rulesPath);
		Assert.assertTrue(rulesDirectory.isDirectory());

		final List<File> ruleDirectories = new ArrayList<>();
		for (File productGroupDirectory : rulesDirectory.listFiles(TestUtil.DIRECTORY_FILTER)) {
			for (File ruleGroupDirectory : productGroupDirectory.listFiles(TestUtil.DIRECTORY_FILTER)) {
				for (File ruleDirectory : ruleGroupDirectory.listFiles(TestUtil.DIRECTORY_FILTER)) {
					final File[] ruleFiles = ruleDirectory.listFiles(TestUtil.RULE_FILE_FILTER);
					if (ruleFiles.length > 0) {
						ruleDirectories.add(ruleDirectory);
					}
				}
			}
		}
		return ruleDirectories;
	}

	public RulesTestManual(File ruleDirectory) {
		this.ruleExecutor = new RuleExecutorFactory().createRuleExecutor(ruleDirectory.getAbsolutePath(), "OneRule");
		this.concepts = new HashMap<>();
		
		final File testCasesFile = new File(ruleDirectory, "test-cases.json");
		if (testCasesFile.isFile()) {

			try {
				testConcepts = TestUtil.loadConceptMap(testCasesFile);
			} catch (FileNotFoundException e) {
				throw new AssertionError("Unexpected FileNotFoundException", e);
			}
			
			setConceptIdReferencesAndTempIds(testConcepts);

			final List<TestConcept<TestDescription, TestRelationship>> givenConcepts = testConcepts.get(GIVEN_CONCEPTS);
			if (givenConcepts != null) {
				for (TestConcept<TestDescription, TestRelationship> givenConcept : givenConcepts) {
					String id = givenConcept.getId();
					Assert.assertNotNull("Concepts in the set '" + GIVEN_CONCEPTS + "' must have an ID", id);
					concepts.put(id, givenConcept);
				}
			}
		}
	}
	
	@Before
	public void setup() {
		ManualResourceConfiguration resourceConfiguration = new ManualResourceConfiguration(true, false, new ResourceConfiguration.Local("src/test/resources/dummy-test-resources"), null);
		TestResourceProvider testResourceProvider = this.ruleExecutor.newTestResourceProvider(new ResourceManager(resourceConfiguration, null));
		conceptService = new TestConceptService(concepts);
		descriptionService = new TestDescriptionService(concepts, testResourceProvider);
		relationshipService = new TestRelationshipService(concepts);
	}

	@Test
	public void testRulesInDirectory() throws JSONException {

		final List<TestConcept<TestDescription, TestRelationship>> conceptsThatShouldPass = testConcepts.get(ASSERT_CONCEPTS_PASS);
		Assert.assertNotNull("The set of concepts '" + ASSERT_CONCEPTS_PASS + "' is required.", conceptsThatShouldPass);
		executeRulesAndAssertExpectations(ruleExecutor, conceptsThatShouldPass, true);

		final List<TestConcept<TestDescription, TestRelationship>> conceptsThatShouldFail = testConcepts.get(ASSERT_CONCEPTS_FAIL);
		Assert.assertNotNull("The set of concepts '" + ASSERT_CONCEPTS_FAIL + "' is required.", conceptsThatShouldPass);
		executeRulesAndAssertExpectations(ruleExecutor, conceptsThatShouldFail, false);
	}

	private void setConceptIdReferencesAndTempIds(Map<String, List<TestConcept<TestDescription, TestRelationship>>> testConcepts) {
		for (List<TestConcept<TestDescription, TestRelationship>> concepts : testConcepts.values()) {
			for (TestConcept<TestDescription, TestRelationship> concept : concepts) {
				setTempIdIfMissing(concept);
				final String id = concept.getId();
				for (TestRelationship relationship : concept.getRelationships()) {
					setTempIdIfMissing(relationship);
					relationship.setSourceId(id);
				}
				for (TestDescription description : concept.getDescriptions()) {
					setTempIdIfMissing(description);
					description.setConceptId(id);
					
					if (description.isTextDefinition()) {
						description.setTypeId(Constants.TEXT_DEFINITION);
					}
				}
				for (OntologyAxiom ontologyAxiom : concept.getOntologyAxioms()) {
					TestOntologyAxiom testOntologyAxiom = (TestOntologyAxiom) ontologyAxiom;
					setTempIdIfMissing(testOntologyAxiom);
					testOntologyAxiom.setReferencedComponentId(id);
				}
			}
		}
	}

	private void setTempIdIfMissing(TestComponent component) {
		if (component.getId() == null) {
			component.setId("temp-id-" + UUID.randomUUID());
		}
	}

	private void executeRulesAndAssertExpectations(RuleExecutor ruleExecutor, List<TestConcept<TestDescription, TestRelationship>> conceptsToTest, boolean expectPass) {
		for (TestConcept<TestDescription, TestRelationship> concept : conceptsToTest) {
			final HashSet<String> ruleSetNames = new HashSet<>();
			ruleSetNames.add("OneRule");
			final List<InvalidContent> invalidContent = ruleExecutor.execute(
					ruleSetNames,
					Collections.singleton(concept),
					conceptService, descriptionService, relationshipService, false, false);

			if (expectPass) {
				Assert.assertEquals("A concept from the " + ASSERT_CONCEPTS_PASS + " set actually failed! " + invalidContent.toString(), 0, invalidContent.size());
			} else {
				Assert.assertNotEquals("A concept from the " + ASSERT_CONCEPTS_FAIL + " set actually passed! " + concept.toString(), 0, invalidContent.size());
			}
		}
	}

}
