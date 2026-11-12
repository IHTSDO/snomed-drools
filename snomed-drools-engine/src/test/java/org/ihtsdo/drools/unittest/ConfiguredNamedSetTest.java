package org.ihtsdo.drools.unittest;

import org.ihtsdo.drools.RuleExecutorFactory;
import org.ihtsdo.drools.rulestestrig.service.TestDescriptionService;
import org.ihtsdo.drools.service.TestResourceProvider;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link TestDescriptionService#isInNamedSet} against the dummy test resources
 * that the rules test rig loads, so a rule driven by a named set can be tested
 * in its configured state as well as its unconfigured one.
 *
 * <p>The module id is synthetic: it is listed under
 * {@code redundant-isa-exempt-modules} in
 * {@code dummy-test-resources/semantic-tag-hierarchies.txt} and appears nowhere
 * else.
 */
public class ConfiguredNamedSetTest {

	private static final String KEY = "redundant-isa-exempt-modules";
	private static final String LISTED_MODULE = "999999001000000104";
	private static final String CORE_MODULE = "900000000000207008";

	private TestDescriptionService descriptionService;

	@Before
	public void setup() {
		ManualResourceConfiguration resourceConfiguration = new ManualResourceConfiguration(true, false,
				new ResourceConfiguration.Local("src/test/resources/dummy-test-resources"), null);
		TestResourceProvider testResourceProvider = new RuleExecutorFactory().createRuleExecutor("src/test/resources/rules")
				.newTestResourceProvider(new ResourceManager(resourceConfiguration, null));
		descriptionService = new TestDescriptionService(new HashMap<>(), testResourceProvider);
	}

	@Test
	public void aListedValueIsAMember() {
		assertTrue(descriptionService.isInNamedSet(KEY, LISTED_MODULE));
	}

	@Test
	public void anUnlistedValueIsNotAMember() {
		assertFalse(descriptionService.isInNamedSet(KEY, CORE_MODULE));
	}

	@Test
	public void aKeyThatIsNotInTheFileHasNoMembers() {
		assertFalse(descriptionService.isInNamedSet("no-such-key", LISTED_MODULE));
	}

	@Test
	public void matchingIsExactRatherThanBySubstring() {
		assertFalse(descriptionService.isInNamedSet(KEY, LISTED_MODULE.substring(0, 9)));
		assertFalse(descriptionService.isInNamedSet(KEY, LISTED_MODULE + "9"));
	}
}
