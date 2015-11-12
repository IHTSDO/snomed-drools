package org.ihtsdo.drools;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.service.DescriptionService;
import org.ihtsdo.drools.domain.Relationship;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

public class RuleExecutor {

	public static final FilenameFilter RULE_FILE_FILTER = new FilenameFilter() {
		@Override
		public boolean accept(File file, String name) {
			return name.endsWith(".drl");
		}
	};

	private final KieContainer kieContainer;
	private final DescriptionService descriptionService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public RuleExecutor(String rulesDirectory, DescriptionService descriptionService) {
		this.descriptionService = descriptionService;
		KieServices kieServices = KieServices.Factory.get();

		// Create the in-memory File System and add the resources files  to it
		KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
		final File rulesDir = new File(rulesDirectory);
		if (!rulesDir.isDirectory()) {
			throw new RuleExecutorException("Rules directory does not exist: " + rulesDir.getAbsolutePath());
		}
		int rules = 0;
		for (File ruleFile : rulesDir.listFiles(RULE_FILE_FILTER)) {
			logger.info("Loading rule {}", ruleFile.getAbsolutePath());
			kieFileSystem.write(ResourceFactory.newFileResource(ruleFile));
			rules++;
		}

		if (rules == 0) {
			logger.warn("No rules loaded.");
		}

		// Create the builder for the resources of the File System
		KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);

		// Build the KieBases
		kieBuilder.buildAll();

		// Check for errors
		if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
			throw new RuleExecutorException(kieBuilder.getResults().toString());
		}

		// Get the Release ID (mvn style: groupId, artifactId,version)
		ReleaseId relId = kieBuilder.getKieModule().getReleaseId();

		// Create the Container, wrapping the KieModule with the given ReleaseId
		kieContainer = kieServices.newKieContainer(relId);
	}

	public List<InvalidContent> execute(Concept concept) {
		final StatelessKieSession session = kieContainer.newStatelessKieSession();

		final List<InvalidContent> invalidContent = new ArrayList<>();
		session.setGlobal("invalidContent", invalidContent);
		session.setGlobal("descriptionService", descriptionService);

		Date start = new Date();
		Set<Object> content = new HashSet<>();
		addConcept(concept, content);
		session.execute(content);
		logger.debug("execute took {} milliseconds", new Date().getTime() - start.getTime());

		return invalidContent;
	}

	private static void addConcept(Concept concept, Set<Object> content) {
		content.add(concept);
		for (Description description : concept.getDescriptions()) {
			content.add(description);
		}
		for (Relationship relationship : concept.getRelationships()) {
			content.add(relationship);
		}
	}

}
