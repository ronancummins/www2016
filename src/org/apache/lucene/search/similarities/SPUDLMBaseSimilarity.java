package org.apache.lucene.search.similarities;

import java.util.Locale;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;

public abstract class SPUDLMBaseSimilarity extends SimilarityBase {

    protected final CollectionModel collectionModel;

    public SPUDLMBaseSimilarity(CollectionModel collectionModel) {
        this.collectionModel = collectionModel;
    }

    /**
     * Creates a new instance with the default collection language model.
     */
    public SPUDLMBaseSimilarity() {
        this(new DefaultCollectionModel());
    }

    @Override
    protected BasicStats newStats(String field, float queryBoost) {
        return new LMStats(field, queryBoost);
    }

    /**
     * Computes the collection probability of the current term in addition to
     * the usual statistics.
     */
    @Override
    protected void fillBasicStats(BasicStats stats, CollectionStatistics collectionStats, TermStatistics termStats) {
        super.fillBasicStats(stats, collectionStats, termStats);
        
        LMStats lmStats = (LMStats) stats;
        
        lmStats.setCollectionPolyaProbability(collectionModel.computePolyaProbability(stats, collectionStats));
        lmStats.setCollectionMultProbability(collectionModel.computePolyaProbability(stats, collectionStats));
        lmStats.setTerm(termStats.term());
        
    }

    @Override
    protected void explain(Explanation expl, BasicStats stats, int doc,
            float freq, float docLen) {
        expl.addDetail(new Explanation(collectionModel.computePolyaProbability(stats, null),
                "collection probability"));
    }

    /**
     * 079 Returns the name of the LM method. The values of the parameters
     * should be 080 included as well. 081 Used in {@link #toString()}. 082
     */
    public abstract String getName();

    /**
     * 086 Returns the name of the LM method. If a custom collection model
     * strategy is 087 used, its name is included as well. 088
     *
     * @see #getName() 089
     * @see CollectionModel#getName() 090
     * @see DefaultCollectionModel 091
     */
    @Override
    public String toString() {
        String coll = collectionModel.getName();
        if (coll != null) {
            return String.format(Locale.ROOT, "LM %s - %s", getName(), coll);
        } else {
            return String.format(Locale.ROOT, "LM %s", getName());
        }
    }

    /**
     * Stores the collection distribution of the current term.
     */
    public static class LMStats extends BasicStats {

        /**
         * The probability that the current term is generated by the collection.
         */
        private float collectionPolyaProbability;
        private float collectionMultProbability;
        

        
        private BytesRef term;
        
        /**
         * 
         * The number of word types in the document
         * 
         */
        
        private float doc_types;

        /**
         * Creates LMStats for the provided field and query-time boost
         */
        public LMStats(String field, float queryBoost) {
            super(field, queryBoost);
            
        }

        /**
         * Returns the probability that the current term is generated by the
         * collection.
         */
        public final float getPolyaCollectionProbability() {
            return collectionPolyaProbability;
        }
        
        public final float getMultCollectionProbability() {
            return collectionMultProbability;
        }        

        public final float getDocTypes() {
            return this.doc_types;
        }  
        
        public final BytesRef getTerm() {
            return this.term;
        }        
        
       
        
        /**
         * Sets the probability that the current term is generated by the
         * collection.
         */
        public final void setCollectionPolyaProbability(float collectionProbability) {
            this.collectionPolyaProbability = collectionProbability;
        }
        
        public final void setCollectionMultProbability(float collectionProbability) {
            this.collectionMultProbability = collectionProbability;
        }        
        
        public final void setTerm(BytesRef _term) {
            this.term = _term;
        }        
        
        public final void setDocTypes(float _types){
            this.doc_types = _types;
            
        }
        
     
        
    }

    /**
     * A strategy for computing the collection language model.
     */
    public static interface CollectionModel {

        /**
         * Computes the probability {@code p(w|C)} according to the language
         * model strategy for the current term.
         */
        public float computePolyaProbability(BasicStats stats, CollectionStatistics colStats);

        public float computeMultProbability(BasicStats stats, CollectionStatistics colStats);
        
        /**
         * The name of the collection model strategy.
         */
        public String getName();
    }

    /**
     * Models {@code p(w|C)} as the number of occurrences of the term in the
     * collection, divided by the total number of tokens {@code + 1}.
     */
    public static class DefaultCollectionModel implements CollectionModel {

        /**
         * Sole constructor: parameter-free
         */
        public DefaultCollectionModel() {
        }

        @Override
        public float computePolyaProbability(BasicStats stats, CollectionStatistics colStats) {
            return (stats.getDocFreq() + 1F) / (colStats.sumDocFreq() + 1F);
            
            
        }
        
         @Override
        public float computeMultProbability(BasicStats stats, CollectionStatistics colStats) {
            return (stats.getNumberOfFieldTokens() + 1F) / (colStats.sumTotalTermFreq() + 1F);
            
        }

        @Override
        public String getName() {
            return null;
        }
        

        
    }
}
