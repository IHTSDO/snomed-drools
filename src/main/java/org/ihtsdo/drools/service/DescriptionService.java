package org.ihtsdo.drools.service;

import java.util.Set;

public interface DescriptionService {

	Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds);

}
