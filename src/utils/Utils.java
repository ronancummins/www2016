package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;

/**
 *
 * @author ronanc
 */
public class Utils {

    private final static Logger logger = Logger.getLogger(Utils.class.getName());

    
    private static CharArraySet stoplist = new CharArraySet(1000, true);
    
    public static EnglishAnalyzer newAnalyzer() throws FileNotFoundException, IOException{
 
        String file = "/home/ronanc/Dropbox/Code/lucene/aux/stopwords.english.large";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                stoplist.add(line);
            }
        }
        
        return new EnglishAnalyzer(stoplist);
        
    }
    
    public static String applyAnalyzer(String text, Analyzer analyzer) throws IOException{
        String term;
        StringBuilder sb = new StringBuilder();
        
        
        //ad in these two lines for extra-stopwords
        
        analyzer = newAnalyzer();
        
        
        
        TokenStream ts = analyzer.tokenStream("myfield", new StringReader(text));
        //OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);
        try {
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                term = ts.getAttribute(CharTermAttribute.class).toString();
                sb.append(term).append(" ");
            }
            ts.end();
        } finally {
            ts.close();
        }         
        
        
        return sb.toString();
    }    
    
    
    public static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
                new Comparator<Map.Entry<K, V>>() {
                    @Override
                    public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                        int res = e2.getValue().compareTo(e1.getValue());
                        return res != 0 ? res : 1; // Special fix to preserve items with equal values
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }    
    
    public static float log2(float x){
        return (float) (Math.log(x)/Math.log(2.0));
    }
    
    
    public synchronized static String tidyWord(String str){
       
        if(str.matches("[-]+")){
            return "";
        }else if(str.length() > 20){
            return "";
        }else if( str.startsWith("<") && str.endsWith(">")){
            return "";
        }else if( str.startsWith("-") && str.endsWith("-")){
            return "";
        }else {
            
            //to lower case
            str = str.toLowerCase();

            //replace any non word characters
            str = str.replaceAll("[^a-zA-Z0-9-\\. ]", "");

            

            return str;
        }
        
    }    
    
    
    
    public static String strip_whitespace(String word){
        word = word.replaceAll("[^a-zA-Z0-9-]", "");
        return word;
    }
    

   
}
