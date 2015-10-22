package org.ihtsdo.validation;

import org.drools.core.SessionConfiguration;
import org.ihtsdo.validation.domain.Concept;
import org.ihtsdo.validation.domain.Description;
import org.ihtsdo.validation.domain.Relationship;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RuleExecutor {

	private final KieContainer kieContainer;
	private final SessionConfiguration sessionConfiguration;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public RuleExecutor() {
		KieServices kieServices = KieServices.Factory.get();
		kieContainer = kieServices.getKieClasspathContainer();
		sessionConfiguration = SessionConfiguration.getDefaultInstance();
		sessionConfiguration.setProperty("drools.dialect.mvel.strict", "false");
	}

	public List<InvalidContent> execute(Concept concept) {
		final StatelessKieSession session = kieContainer.newStatelessKieSession(sessionConfiguration);

		final List<InvalidContent> invalidContent = new ArrayList<>();
		session.setGlobal("invalidContent", invalidContent);
		session.setGlobal("ecl", new ECL());

		Date start = new Date();
		Set<Object> content = new HashSet<>();
		addConcept(concept, content);
		session.execute(content);
		logger.info("execute took {} milliseconds", new Date().getTime() - start.getTime());

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
