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

  /* This test needs to be in specific subclasses of TestDictionary
   * since subclasses may override formatQueryKey() and change the value */
  public void testQueryKeys() throws Exception {
    assertEquals("foobar", dict.formatQueryKey("foo  bar"));
    assertEquals("foobar", dict.formatQueryKey("foobar"));
    assertEquals("foobar", dict.formatQueryKey("   foo bar "));
    assertEquals("foobar", dict.formatQueryKey(" @$  foo.bar\n\r"));
  }
}
