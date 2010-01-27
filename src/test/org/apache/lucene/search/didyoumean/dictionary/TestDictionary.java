package org.apache.lucene.search.didyoumean.dictionary;

import junit.framework.TestCase;

/**
 * FIXME: Missing class docs for org.apache.lucene.search.didyoumean.dictionary.TestDictionary
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Jan 26, 2010
 */
public class TestDictionary extends TestCase {

  protected Dictionary dict;

  /* This test needs to be in specific subclasses of TestDictionary
   * since subclasses may override formatQueryKey() and change the value */
  public void testQueryKeys() throws Exception {
    assertEquals("foobar", dict.formatQueryKey("foo  bar"));
    assertEquals("foobar", dict.formatQueryKey("foobar"));
    assertEquals("foobar", dict.formatQueryKey("   foo bar "));
    assertEquals("foobar", dict.formatQueryKey(" @$  foo.bar\n\r"));
  }

  public void testEmptyDict() throws Exception {
    assertEquals(0, dict.size());
    assertEquals(0, dict.getSuggestions("ff").size());
  }

  public void testOneSuggestion() throws Exception {
    SuggestionList suggestions = dict.suggestionListFactory("foo");
    suggestions.addSuggested("foobar", 1d, 1);
    dict.put(suggestions);
    assertEquals(1, dict.size());

    SuggestionList suggs = dict.getSuggestions("foo");
    assertEquals(1, suggs.size());
    assertEquals(suggestions.get(0), suggs.get(0));

    suggs = dict.getSuggestions("foo \n");
    assertEquals(1, suggs.size());
    assertEquals(suggestions.get(0), suggs.get(0));

    // Try an put the same suggestions again - this should be a no-op
    dict.put(suggestions);
    assertEquals(1, dict.size());

    suggs = dict.getSuggestions("foo");
    assertEquals(1, suggs.size());
    assertEquals(suggestions.get(0), suggs.get(0));
  }
}
