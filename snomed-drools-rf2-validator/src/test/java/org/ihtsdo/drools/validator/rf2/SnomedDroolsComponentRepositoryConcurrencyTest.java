package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;
import org.ihtsdo.drools.validator.rf2.domain.DroolsRelationship;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The RF2 files are read concurrently, so several threads append to the same
 * concept at once. An unsynchronized {@code HashSet} loses entries under that
 * load, and a lost entry becomes a missing validation finding rather than a
 * crash - which is why this is tested directly rather than trusted.
 */
public class SnomedDroolsComponentRepositoryConcurrencyTest {

	private static final String MODULE_ID = "900000000000207008";
	private static final String STATED = "900000000000010007";
	private static final String IS_A = "116680003";
	private static final int THREADS = 8;
	private static final int PER_THREAD = 500;

	private SnomedDroolsComponentRepository repository;

	@Before
	public void setup() {
		repository = new SnomedDroolsComponentRepository();
	}

	@Test
	public void keepsEveryDescriptionAddedConcurrentlyToOneConcept() throws Exception {
		repository.addConcept(concept("100101001"));

		runConcurrently(thread -> {
			for (int i = 0; i < PER_THREAD; i++) {
				repository.addDescription(description(id(thread, i), "100101001", "term " + thread + " " + i));
			}
			return null;
		});

		assertEquals(THREADS * PER_THREAD, repository.getConcept("100101001").getDescriptions().size());
		assertEquals(THREADS * PER_THREAD, repository.getDescriptions().size());
	}

	@Test
	public void keepsEveryRelationshipAddedConcurrentlyToOneConcept() throws Exception {
		repository.addConcept(concept("100101001"));
		repository.addConcept(concept("100102001"));

		runConcurrently(thread -> {
			for (int i = 0; i < PER_THREAD; i++) {
				repository.addRelationship(relationship(id(thread, i), "100101001", "100102001"));
			}
			return null;
		});

		assertEquals(THREADS * PER_THREAD, repository.getConcept("100101001").getRelationships().size());
		// Every one of them is an active stated IsA, so all land as inbound on the target.
		assertEquals(THREADS * PER_THREAD,
				repository.getConcept("100102001").getActiveInboundStatedRelationships().size());
	}

	/**
	 * Two concepts related to each other in both directions. Locking the source and
	 * then the destination concept would give two threads the same pair of locks in
	 * opposite orders; if this deadlocks, the test times out rather than passing
	 * slowly.
	 */
	@Test
	public void doesNotDeadlockOnRelationshipsPointingBothWays() throws Exception {
		repository.addConcept(concept("100101001"));
		repository.addConcept(concept("100102001"));

		List<Future<Void>> futures = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			CyclicBarrier startTogether = new CyclicBarrier(2);
			futures.add(executor.submit(reciprocalAdder(startTogether, "100101001", "100102001", 0)));
			futures.add(executor.submit(reciprocalAdder(startTogether, "100102001", "100101001", 1)));

			for (Future<Void> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		}

		assertEquals(PER_THREAD, repository.getConcept("100101001").getRelationships().size());
		assertEquals(PER_THREAD, repository.getConcept("100102001").getRelationships().size());
		assertTrue(repository.getComponentLoadingErrors().isEmpty());
	}

	private Callable<Void> reciprocalAdder(CyclicBarrier startTogether, String source, String destination, int thread) {
		return () -> {
			startTogether.await(30, TimeUnit.SECONDS);
			for (int i = 0; i < PER_THREAD; i++) {
				repository.addRelationship(relationship(id(thread, i), source, destination));
			}
			return null;
		};
	}

	private void runConcurrently(ThreadBody body) throws Exception {
		try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
			CyclicBarrier startTogether = new CyclicBarrier(THREADS);
			List<Future<Void>> futures = new ArrayList<>();
			for (int thread = 0; thread < THREADS; thread++) {
				int t = thread;
				futures.add(executor.submit(() -> {
					startTogether.await(30, TimeUnit.SECONDS);
					return body.run(t);
				}));
			}
			for (Future<Void> future : futures) {
				future.get(60, TimeUnit.SECONDS);
			}
		}
	}

	private interface ThreadBody {
		Void run(int thread) throws Exception;
	}

	/** Distinct, valid-length SCTID-ish ids per thread and iteration. */
	private static String id(int thread, int i) {
		return String.format("1%01d%05d011", thread, i);
	}

	private static DroolsConcept concept(String conceptId) {
		return new DroolsConcept(conceptId, "20260731", true, MODULE_ID, "900000000000074008", true, true);
	}

	private static DroolsDescription description(String descriptionId, String conceptId, String term) {
		return new DroolsDescription(descriptionId, "20260731", true, MODULE_ID, conceptId, "en",
				"900000000000013009", term, "900000000000448009", false, true, true);
	}

	private static DroolsRelationship relationship(String relationshipId, String sourceId, String destinationId) {
		return new DroolsRelationship(null, "20260731", false, relationshipId, true, MODULE_ID,
				sourceId, destinationId, 0, IS_A, STATED, true, true, null);
	}
}
