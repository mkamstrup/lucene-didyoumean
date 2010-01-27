package org.apache.lucene.search.didyoumean.dictionary;

import java.io.File;

/**
 * FIXME: Missing class docs for org.apache.lucene.search.didyoumean.dictionary.TestH2Dictionary
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Jan 27, 2010
 */
public class TestH2Dictionary extends TestDictionary {

  File baseDir;

  public void setUp() throws Exception {
    baseDir = new File(System.getProperty("java.io.tmpdir"),
               "" + System.currentTimeMillis());
    dict = new H2Dictionary(baseDir);
  }

  public void tearDown() throws Exception {
    dict.close();
    baseDir.deleteOnExit();
  }  
}
