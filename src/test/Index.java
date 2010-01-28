import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.facade.DirectoryIndexFacade;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.index.facade.IndexWriterFacade;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;

/**
 * Generate a Lucene index by reading textual data from stdin mapping each line
 * to a document
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Jan 28, 2010
 */
public class Index {

  public static void main (String[] args) {
    if (args.length != 2) {
      System.err.println(
        "Please specify exactly two arguments - the name of the folder " +
        "to store the index in and the field name to store indexed data in");
      System.exit(1);
    }

    File indexDir = new File(args[0]);
    String fieldName = args[1];

    if (indexDir.isFile()) {
      System.err.println("'" + indexDir + "' is a regular file");
      System.exit(2);
    }

    IndexFacade index = null;
    IndexWriterFacade writer = null;
    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT, Collections.EMPTY_SET);

    try {
      index = new DirectoryIndexFacade(FSDirectory.open(indexDir));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(3);
    }

    boolean doUpdate = false;
    try {
      if (indexDir.isDirectory()) {
        // Update existing index
        writer = index.indexWriterFactory(analyzer, false);
        doUpdate = true;
      } else {
        writer = index.indexWriterFactory(analyzer, true);
        doUpdate = false;
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(4);
    }

    int count = 0;
    String line;
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    try {
      while ((line = in.readLine()) != null) {
        Document document = new Document();
        document.add(new Field(
          fieldName, line, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
        writer.addDocument(document);
        count++;
      }
      writer.close();
      System.out.println(
        (doUpdate ? "Updated index" : "Created new index") + " with " + count + " documents");
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(5);
    }
  }

}
