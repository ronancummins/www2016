package searching;

import java.io.IOException;
import java.util.logging.Logger;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.search.similarities.SPUDLMSimilarity;
import static org.apache.lucene.search.similarities.SPUDLMSimilarity.a;
import static org.apache.lucene.search.similarities.SPUDLMSimilarity.dir_mu;



/**
 *
 * This provides the correct normalisation for language models that use 
 * Bayesian smoothing (i.e. the SPUD model 
 * and the LM with Dirichlet priors smoothing)
 * 
 * @author ronanc
 */
public class LMNormScoreProvider extends CustomScoreProvider {

    private final static Logger logger = Logger.getLogger(LMNormScoreProvider.class.getName());
    
    private int queryLen;
    
    private double query_info;
    
    public static double n;
    
    public LMNormScoreProvider(LeafReaderContext context, int _queryLen) throws IOException {
        super(context);
        queryLen = _queryLen;
        
    }

    @Override
    public float customScore(int doc,
            float subQueryScore,
            float[] valSrcScores)
            throws IOException {

        //get the document length
        float dl = this.context.reader().getNumericDocValues("TotalTerms").get(doc);
        float dvl = this.context.reader().getNumericDocValues("UniqueTerms").get(doc);
        float ent = this.context.reader().getNumericDocValues("Entropy").get(doc);
        //float max_ent = this.context.reader().getNumericDocValues("MaxEntropy").get(doc);
        
        float qvl = QuerySearch.currentQuery.numTypes();

        /*
        double avg = 0;
        for (int i = 0; i < a; i++) {
            if ((doc % a) == i) {
                dl = dl * (i + 1);

            }
            avg += (i + 1) / a;
        }
        */
        
        float lmnorm = 0;
        
        if ((SPUDLMSimilarity.method == SPUDLMSimilarity.dir)||(SPUDLMSimilarity.method == SPUDLMSimilarity.dir_plus)){
            //LM Dirichlet 
            
            lmnorm = (float) (Math.log(SPUDLMSimilarity.dir_mu / (dl + SPUDLMSimilarity.dir_mu)));
        }else if (SPUDLMSimilarity.method == SPUDLMSimilarity.dir_entropy){
            //entropy
            lmnorm = (float) (Math.log(SPUDLMSimilarity.dir_mu / (ent + SPUDLMSimilarity.dir_mu)));
            
        }else if (SPUDLMSimilarity.method == SPUDLMSimilarity.spud){
            //spud
            double spud_mu = SPUDLMSimilarity.b0*SPUDLMSimilarity.omega/(1-SPUDLMSimilarity.omega);
            lmnorm = (float) (Math.log(spud_mu / (dvl + spud_mu)));
            
            
        }else if (SPUDLMSimilarity.method == SPUDLMSimilarity.two_stage){
            
            lmnorm = (float) Math.log(((1-SPUDLMSimilarity.k1)*SPUDLMSimilarity.dir_mu + 
                    SPUDLMSimilarity.k1*(SPUDLMSimilarity.dir_mu+dl)) /(dl+SPUDLMSimilarity.dir_mu));
        
        }else if (SPUDLMSimilarity.method == SPUDLMSimilarity.spud_entropy){
            //spud
            double spud_mu = SPUDLMSimilarity.b0*SPUDLMSimilarity.omega/(1-SPUDLMSimilarity.omega);
            lmnorm = (float) (Math.log(spud_mu / (ent + spud_mu)));
            
            
        }else if ((SPUDLMSimilarity.method == SPUDLMSimilarity.bm25)
                ||(SPUDLMSimilarity.method == SPUDLMSimilarity.bm25_plus)
                ||(SPUDLMSimilarity.method == SPUDLMSimilarity.matf)
                ||(SPUDLMSimilarity.method == SPUDLMSimilarity.jm)){
            lmnorm = 0f;
        }else{
            //default spud
            double spud_mu = SPUDLMSimilarity.b0*SPUDLMSimilarity.omega/(1-SPUDLMSimilarity.omega);
            lmnorm = (float) (Math.log(spud_mu / (dvl + spud_mu)));
            
        }
        
        
        
        return (subQueryScore + lmnorm );
        
    }

}
