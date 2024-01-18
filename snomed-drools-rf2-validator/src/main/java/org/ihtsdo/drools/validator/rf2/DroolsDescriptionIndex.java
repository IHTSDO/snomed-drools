package org.ihtsdo.drools.validator.rf2;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DroolsDescriptionIndex {

    private Directory index;
    private IndexSearcher indexSearcher;
    private IndexReader indexReader;
    private static final String FIELD_TERM = "term";
    private static final String FIELD_ID = "id";
    private static final String FIELD_IS_ACTIVE = "isActive";

    private static final String BOOLEAN_TRUE_VALUE = "1";
    private static final String BOOLEAN_FALSE_VALUE = "0";

    private static final Logger LOGGER = LoggerFactory.getLogger(DroolsDescriptionIndex.class);

    public DroolsDescriptionIndex(SnomedDroolsComponentRepository repository) {
        loadRepository(repository);
    }

    private void loadRepository(SnomedDroolsComponentRepository repository) {
        //Only load repository at first time
        if (index == null) {
            index = new ByteBuffersDirectory();
            StandardAnalyzer analyzer = new StandardAnalyzer();
            Collection<DroolsDescription> descriptions = repository.getDescriptions();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            try {
                IndexWriter indexWriter = new IndexWriter(index, config);
                for (DroolsDescription description : descriptions) {
                    addDoc(indexWriter, description.getTerm(), description.getId(), description.isActive());
                }
                indexWriter.commit();
                indexWriter.close();
                indexReader = DirectoryReader.open(index);
                indexSearcher = new IndexSearcher(indexReader);
            } catch (IOException e) {
                LOGGER.error("Encounter error when loading repository", e);
            }
        }
    }

    public Set<String> findMatchedDescriptionTerm(String term, boolean isActive) {
        Set<String> results = new HashSet<>();
        try {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(new TermQuery(new Term(FIELD_IS_ACTIVE, getBooleanValue(isActive))), BooleanClause.Occur.FILTER);
            builder.add(new TermQuery(new Term(FIELD_TERM, term)), BooleanClause.Occur.FILTER);
            int preView = indexReader.docFreq(new Term(FIELD_TERM, term));
            TopDocs docs = indexSearcher.search(builder.build(), preView == 0 ? 1 : preView);
            ScoreDoc[] hits = docs.scoreDocs;
            for (ScoreDoc hit : hits) {
                Document document = indexSearcher.storedFields().document(hit.doc);
                results.add(document.get(FIELD_ID));
            }

        } catch (IOException e) {
            LOGGER.error("Encounter error when finding matching term", e);
        }
        return results;
    }

    private static void addDoc(IndexWriter w, String term, String id, boolean isActive) throws IOException {
        Document doc = new Document();
        doc.add(new StringField(FIELD_IS_ACTIVE, getBooleanValue(isActive), Field.Store.NO));
        doc.add(new StringField(FIELD_TERM, term, Field.Store.NO));
        doc.add(new StringField(FIELD_ID, id, Field.Store.YES));
        w.addDocument(doc);
    }

    private static String getBooleanValue(boolean value) {
        return value ? BOOLEAN_TRUE_VALUE : BOOLEAN_FALSE_VALUE;
    }

    public void cleanup() {
        try {
            indexReader.close();
            index.close();
            index = null;
            indexSearcher = null;
        } catch (IOException e) {
            LOGGER.error("Encounter error when cleaning up DroolsDescriptionIndex", e);
        }

    }

}
