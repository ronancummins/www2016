package searching;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.special.Gamma;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.similarities.SPUDLMSimilarity;


/**
 *
 * @author ronanc
 */
public class CustomQuery {
    
    private final static Logger logger = Logger.getLogger(CustomQuery.class.getName());  
    
    private TreeMap<String,Double> bag;

    

    private Double mass;
    
    
    public static boolean QuerySmoothing = false;
    
    
    public static boolean useBackgroundQuery = false;
    public static TreeMap<String, Double> BackgroundQueryModel = new TreeMap();
    public static TreeMap<String, Double> BackgroundSPUDQueryModel = new TreeMap();
    
    public static ArrayList<Integer> qlength = new ArrayList();
    public static ArrayList<Integer> qvlength = new ArrayList();
    
    public static Double BackgroundQueryMass=0.0;
    public static Double BackgroundSPUDQueryMass=0.0;
    
    public static int QueryMethod;
    
    public static int QueryJM = 1;
    public static int QueryDir = 2;
    public static int QuerySPUD = 3;
    
    public static double QueryParam;
    
    public CustomQuery(){
        bag = new TreeMap<String,Double>();
        mass = 0.0;

    }
    
    
    public void empty(){
        bag.clear();
        mass = 0.0;
    }
    
  
    public void setUniformProbs(){
        for (String t: bag.keySet()){
            bag.put(t, 1.0);
        }
        
        mass = (double)bag.size();
    }
    
    public void add(String str, Double f){
        
        Double c = bag.get(str);
        
        
        if (c==null){
            bag.put(str, f);
        }else{
            bag.put(str, c+f);
        }
        mass += f;
    }
    
    
    
    public void remove(String str, Double f){
        Double c = bag.get(str);
        
        if (c==null){
            bag.put(str, -f);
        }else{
            bag.put(str, c-f);
        }
        mass -= f;
    }
    
   
    
    
    public double get(String str){
        
        Double ret = bag.get(str);
        
        if (ret == null){
            return 0;
        }else{
            return ret;
        }
        
    }
    
    //types in bag
    public int numTypes(){
        return this.bag.size();
    }
    
    //raw mass of query bag
    public double mass(){
        return this.mass;
    }
    
    

    
    //
    // returns the string representation
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (String key: this.bag.keySet()){
            sb.append(key).append(" ");
        }
        return sb.toString();
    }
    
    
    public boolean contains(String term){
        return this.bag.containsKey(term);
    }
    
    public String examine(){
        StringBuilder sb = new StringBuilder();
        for (String key: this.bag.keySet()){
            sb.append("\n" + key + "\t" + bag.get(key)/this.mass);
        }
        sb.append("\n");
        return sb.toString();
    }    
    

    public Set<String> terms(){
        return bag.keySet();
    }
    
    
    public static double estimate_query_mass() throws IOException {

        
        // now estimate mass of background query model (DQM)
        //
        logger.info("estimate background DQM mass...");
        double denom;
        double s = 250;
        double sumDocFreq = BackgroundSPUDQueryMass;
        double numQueries = 499;
        for (int i = 0; i < 20; i++) {
            logger.log(Level.INFO, "iteration " + i + " estimated mu value is " + s );
            denom = 0;
            
            for (int j = 0; j < numQueries; j++) {
                
                //logger.info(str_dl[0] + "");
                Double dl = qlength.get(j).doubleValue();
                denom += Gamma.digamma(s + dl);
            }
            
            
            denom =  (denom - (numQueries * Gamma.digamma(s)));
            
            s = sumDocFreq/denom;

        }        
        logger.info("done.");
        
        
        
        return s;
    }    
    
    
    public void extractQueryTopic(IndexReader reader) throws IOException{
        
        //this is the smoothing parameter for a background query Polya
        double pi;
        double gbmass;
        if (CustomQuery.QueryMethod == CustomQuery.QuerySPUD ){
            if (useBackgroundQuery){
                gbmass = 32;
            }else{
                gbmass = SPUDLMSimilarity.b0;
            }
            
            pi = gbmass * CustomQuery.QueryParam / 
                            (bag.keySet().size() * (1 - CustomQuery.QueryParam) + gbmass * CustomQuery.QueryParam);
        }else if (CustomQuery.QueryMethod == CustomQuery.QueryJM ){
            pi = CustomQuery.QueryParam;
            //pi = SPUDLMSimilarity.b0 * CustomQuery.QueryParam / 
            //                (1 * (1 - CustomQuery.QueryParam) + SPUDLMSimilarity.b0 * CustomQuery.QueryParam);
        }else if (CustomQuery.QueryMethod == CustomQuery.QueryDir ){
            
            if (useBackgroundQuery){
                gbmass = 0.1;
            }else{
                gbmass = 1.0;
            }
            pi = CustomQuery.QueryParam*gbmass/(CustomQuery.QueryParam*gbmass + mass);
            
        }else{
            if (useBackgroundQuery){
                gbmass = 32;
            }else{
                gbmass = SPUDLMSimilarity.b0;
            }
            pi = gbmass * CustomQuery.QueryParam / 
                            (bag.keySet().size() * (1 - CustomQuery.QueryParam) + gbmass * CustomQuery.QueryParam);
        }
        
        double pC;
        double pQ, w;
        double topic=0;
        Double bqtf;
        //logger.info(currentQuery.toString() + "\t" + spud_pi );
        for(String term : bag.keySet()){
            
            pQ = bag.get(term)/mass;
            
            
            
            if (useBackgroundQuery){
                
                if (CustomQuery.QueryMethod == CustomQuery.QuerySPUD ){
                    bqtf = CustomQuery.BackgroundSPUDQueryModel.get(term);
                
                    if (bqtf == null){ bqtf = 0.0;}
                    pC = (bqtf + 0.00001) / 
                            (CustomQuery.BackgroundSPUDQueryMass);
                    
                }else{
                    bqtf = CustomQuery.BackgroundQueryModel.get(term);
                
                    if (bqtf == null){ bqtf = 0.0;}
                    pC = (bqtf + 0.00001) / 
                            (CustomQuery.BackgroundQueryMass);
                }
                
            }else{
                if (CustomQuery.QueryMethod == CustomQuery.QuerySPUD ){
                    pC = (double)reader.docFreq(new Term("text",term)) / reader.getSumDocFreq("text");
                }else{
                    pC = (double)reader.totalTermFreq(new Term("text",term)) / reader.getSumTotalTermFreq("text");
                }
            }
                   
            
            
            w = bag.get(term) * (1.0-pi)*pQ /((1.0-pi)*pQ + pi*pC);
            
            //w = (1.0-pi)*pQ /((1.0-pi)*pQ + pi*pC);
            
            //logger.info(term + "\t" + w + "\t" + bag.get(term) + "\t" + pi*pC + "\t" + (1.0-pi)*pQ + "\t" + pi);
            if (w > 0){
                topic += w;
            }
            
            bag.put(term, w);
            
            //logger.info(term + "\t" + reader.numDocs() + "\t" + reader.docFreq(new Term("text",term)) + "\t"+ query_info);
        }
        //logger.info("################# ");
        mass = topic;
        
    }    
    
}
