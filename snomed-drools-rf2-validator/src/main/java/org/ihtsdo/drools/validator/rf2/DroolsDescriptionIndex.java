package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Exact-term lookup over the descriptions of the release being validated.
 *
 * <h2>Why this is a map rather than a Lucene index</h2>
 *
 * <p>This class used to hold an in-memory Lucene index, but nothing about the
 * question it answers needs a search engine. The term was indexed as a
 * {@code StringField}, which is a single token and is never passed through the
 * analyser; the query was a {@code TermQuery}, which is exact; both clauses were
 * {@code Occur.FILTER}, so nothing was ever scored; and the only value read back
 * was a stored field holding the description id. That is an exact-match
 * dictionary implemented as an inverted index.
 *
 * <p>The cost of that was not theoretical. Sampling the worker threads of a full
 * 722,404-concept Snapshot validation put <strong>69.6% of all rule-execution
 * CPU</strong> inside {@link #findMatchedDescriptionTerm}, and the leaf frames
 * were almost entirely Lucene block decoding and LZ4 decompression of the stored
 * ids - {@code ByteBuffersDataInput.slice}, {@code readBytes},
 * {@code LZ4.decompress}, {@code SegmentTermsEnum.getFrame}. None of that work
 * contributes to the answer. A hash lookup returns the same ids without touching
 * a byte buffer.
 *
 * <h2>Why the values are arrays</h2>
 *
 * <p>A {@code Set<String>} per term would be the obvious value type, but there
 * is roughly one distinct term per description - 2.26M on an AU edition - and a
 * single-element {@code HashSet} costs about 150 bytes of overhead against 24
 * for a single-element array. The array form keeps this structure smaller than
 * the Lucene index it replaces, which matters because the Drools pass has run
 * out of heap before now. Terms shared by several descriptions are rare, so
 * growing by one on collision is cheaper than carrying a set everywhere.
 */
public class DroolsDescriptionIndex {

    /** Term to description ids, for active descriptions only. */
    private Map<String, String[]> activeTermToDescriptionIds;

    /** Term to description ids, for inactive descriptions only. */
    private Map<String, String[]> inactiveTermToDescriptionIds;

    public DroolsDescriptionIndex(SnomedDroolsComponentRepository repository) {
        loadRepository(repository);
    }

    private void loadRepository(SnomedDroolsComponentRepository repository) {
        //Only load repository at first time
        if (activeTermToDescriptionIds != null) {
            return;
        }

        Collection<DroolsDescription> descriptions = repository.getDescriptions();
        // Sized for the active map because most descriptions are active, and
        // growing a map towards two million entries rehashes it around twenty
        // times. The inactive map is left to grow from its default capacity.
        activeTermToDescriptionIds = new HashMap<>(Math.max(16, descriptions.size()));
        inactiveTermToDescriptionIds = new HashMap<>();

        for (DroolsDescription description : descriptions) {
            String term = description.getTerm();
            if (term == null) {
                continue;
            }
            Map<String, String[]> termToDescriptionIds =
                    description.isActive() ? activeTermToDescriptionIds : inactiveTermToDescriptionIds;
            addDescriptionId(termToDescriptionIds, term, description.getId());
        }
    }

    private static void addDescriptionId(Map<String, String[]> termToDescriptionIds, String term, String descriptionId) {
        String[] existing = termToDescriptionIds.get(term);
        if (existing == null) {
            termToDescriptionIds.put(term, new String[]{descriptionId});
            return;
        }
        String[] combined = Arrays.copyOf(existing, existing.length + 1);
        combined[existing.length] = descriptionId;
        termToDescriptionIds.put(term, combined);
    }

    /**
     * Ids of the descriptions whose term is exactly {@code term} and whose active
     * flag is {@code isActive}.
     *
     * <p>The returned set is immutable. Description ids are unique, so the ids
     * held against one term cannot repeat.
     */
    public Set<String> findMatchedDescriptionTerm(String term, boolean isActive) {
        Map<String, String[]> termToDescriptionIds =
                isActive ? activeTermToDescriptionIds : inactiveTermToDescriptionIds;
        if (termToDescriptionIds == null) {
            return Collections.emptySet();
        }
        String[] descriptionIds = termToDescriptionIds.get(term);
        if (descriptionIds == null) {
            return Collections.emptySet();
        }
        return Set.of(descriptionIds);
    }

    public void cleanup() {
        activeTermToDescriptionIds = null;
        inactiveTermToDescriptionIds = null;
    }

}
