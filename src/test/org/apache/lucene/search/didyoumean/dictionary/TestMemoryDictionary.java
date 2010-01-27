package org.apache.lucene.search.didyoumean.dictionary;

/**
 * FIXME: Missing class docs for org.apache.lucene.search.didyoumean.dictionary.TestMemoryDictionary
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Jan 26, 2010
 */
public class TestMemoryDictionary extends TestDictionary{

  public void setUp() {
    dict = new MemoryDictionary();
  }  
}
