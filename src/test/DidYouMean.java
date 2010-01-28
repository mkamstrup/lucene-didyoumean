import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.facade.DirectoryIndexFacade;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.search.didyoumean.secondlevel.token.MultiTokenSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.TokenPhraseSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.TokenPhraseSuggesterImpl;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.NgramTokenSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.TermEnumIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;

/**
 * Stand alone program with an interactive shell. Open with a given Lucene index
 * to use as A Priori index.
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Jan 28, 2010
 */
public class DidYouMean {

  public static void main (String[] args) {
    if (args.length != 2) {
      System.err.println(
        "Please specify a Lucene index as the first argument and " +
        "the field name to base the dictionary on as the second argument");
      System.exit(1);
    }
    
    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT, Collections.EMPTY_SET);
    String aprioriField = args[1];

    File indexDir = new File(args[0]);
    if (!indexDir.isDirectory()) {
      System.err.println("'" + indexDir + "' is not a directory");
      System.exit(2);
    }

    IndexFacade aprioriIndex = null;
    try {
      aprioriIndex = new DirectoryIndexFacade(FSDirectory.open(indexDir));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(2);
    }

    IndexFacade ngramIndex = null;
    NgramTokenSuggester tokenSuggester = null;
    try {
      ngramIndex = new DirectoryIndexFacade(new RAMDirectory());
      ngramIndex.indexWriterFactory(null, true).close(); // Initialize empty index
      tokenSuggester = new NgramTokenSuggester(ngramIndex);
      tokenSuggester.indexDictionary(new TermEnumIterator(aprioriIndex.indexReaderFactory(), aprioriField), 2);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(3);
    }

    TokenPhraseSuggester phraseSuggester = null;
    try {
      //phraseSuggester = new TokenPhraseSuggesterImpl(tokenSuggester, aprioriField, false, 3, analyzer, aprioriIndex);
      phraseSuggester = new MultiTokenSuggester(tokenSuggester, aprioriField, false, 3, analyzer, aprioriIndex);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(4);
    }

    String line;
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    try {
      System.out.print("$query> ");
      System.out.flush();
      while ((line = in.readLine()) != null) {
        String dym = phraseSuggester.didYouMean(line);
        System.out.println(String.format("Did you mean: \"%s\"?", dym));
        System.out.print("$query> ");
        System.out.flush();
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(5);
    }
  }

}
