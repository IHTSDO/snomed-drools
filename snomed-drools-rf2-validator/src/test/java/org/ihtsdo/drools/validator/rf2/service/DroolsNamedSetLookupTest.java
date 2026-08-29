package org.ihtsdo.drools.validator.rf2.service;

import org.ihtsdo.drools.service.TestResourceProvider;
import org.ihtsdo.drools.validator.rf2.DroolsRF2Validator;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@code isInNamedSet} against the real reference-data store.
 *
 * <p>The absent-key case is the one that matters, because it is the state every
 * edition is in until it adds a key: a rule that negates this method must go on
 * behaving as it did before the key existed.
 */
public class DroolsNamedSetLookupTest extends BaseServiceTest {

	private static final String KEY = "redundant-isa-exempt-modules";
	private static final String AU_MODULE = "32506021000036107";
	private static final String CORE_MODULE = "900000000000207008";

	private DroolsDescriptionService serviceWithNoResources;

	@Before
	public void setup() throws IOException {
		loadConceptsIntoRepository();
		loadDescriptionsIntoRepository();
		ResourceManager blank = new ResourceManager(DroolsRF2Validator.BLANK_RESOURCES_CONFIGURATION, null);
		serviceWithNoResources = service(new TestResourceProvider(blank));
	}

	private DroolsDescriptionService service(TestResourceProvider provider) {
		return new DroolsDescriptionService(repository, new DroolsConceptService(repository, null), provider);
	}

	/** No reference data at all - every lookup must answer false. */
	@Test
	public void absentFileMeansNotAMember() {
		assertFalse(serviceWithNoResources.isInNamedSet(KEY, AU_MODULE));
		assertFalse(serviceWithNoResources.isInNamedSet(KEY, CORE_MODULE));
	}

	@Test
	public void aKeyThatIsNotInTheFileMeansNotAMember() throws IOException {
		assertFalse(withSet("some-other-key", AU_MODULE).isInNamedSet(KEY, AU_MODULE));
	}

	@Test
	public void aListedValueIsAMemberAndAnUnlistedOneIsNot() throws IOException {
		DroolsDescriptionService s = withSet(KEY, AU_MODULE);
		assertTrue(s.isInNamedSet(KEY, AU_MODULE));
		assertFalse(s.isInNamedSet(KEY, CORE_MODULE));
	}

	@Test
	public void matchingIsExactRatherThanBySubstring() throws IOException {
		// Module ids are long and share prefixes; a substring match would exempt
		// modules nobody listed.
		DroolsDescriptionService s = withSet(KEY, AU_MODULE);
		assertFalse(s.isInNamedSet(KEY, AU_MODULE.substring(0, 8)));
		assertFalse(s.isInNamedSet(KEY, AU_MODULE + "9"));
	}

	@Test
	public void nullsAreNotMembers() throws IOException {
		DroolsDescriptionService s = withSet(KEY, AU_MODULE);
		assertFalse(s.isInNamedSet(null, AU_MODULE));
		assertFalse(s.isInNamedSet(KEY, null));
	}

	/**
	 * The loader that reads {@code key=value,value} from
	 * semantic-tag-hierarchies.txt is already exercised by the semantic-tag
	 * tests, so the map is supplied directly here to keep this about the lookup.
	 */
	private DroolsDescriptionService withSet(String key, String... values) throws IOException {
		ResourceManager blank = new ResourceManager(DroolsRF2Validator.BLANK_RESOURCES_CONFIGURATION, null);
		Map<String, Set<String>> named = new HashMap<>();
		named.put(key, new HashSet<>(java.util.Arrays.asList(values)));
		return service(new TestResourceProvider(blank) {
			@Override
			public Map<String, Set<String>> getSemanticHierarchyMap() {
				return Collections.unmodifiableMap(named);
			}
		});
	}
}
