package org.ihtsdo.drools.unittest;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RuleSchedulingProbe {

	private static final String BLOCKED_CONCEPT_ID = "1";
	private static final String RELEASING_CONCEPT_ID = "11";
	private static final Set<String> visitedConceptIds = ConcurrentHashMap.newKeySet();
	private static final AtomicBoolean blockedConceptReleasedByLaterConcept = new AtomicBoolean();
	private static volatile CountDownLatch laterConceptVisited = new CountDownLatch(1);

	private RuleSchedulingProbe() {
	}

	public static void reset() {
		visitedConceptIds.clear();
		blockedConceptReleasedByLaterConcept.set(false);
		laterConceptVisited = new CountDownLatch(1);
	}

	public static boolean visit(String conceptId) {
		visitedConceptIds.add(conceptId);
		if (RELEASING_CONCEPT_ID.equals(conceptId)) {
			laterConceptVisited.countDown();
		} else if (BLOCKED_CONCEPT_ID.equals(conceptId)) {
			try {
				blockedConceptReleasedByLaterConcept.set(laterConceptVisited.await(2, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return false;
	}

	public static boolean wasBlockedConceptReleasedByLaterConcept() {
		return blockedConceptReleasedByLaterConcept.get();
	}

	public static int getVisitedConceptCount() {
		return visitedConceptIds.size();
	}
}
