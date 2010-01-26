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

  public void testEmptyDict() throws Exception {
    assertEquals(0, dict.size());
    assertEquals(0, dict.getSuggestions("ff").size());
  }

  public void testOneSuggestion() throws Exception {
    SuggestionList suggestions = dict.suggestionListFactory("foo");
    suggestions.addSuggested("foobar", 1d, 1);
    dict.put(suggestions);
    assertEquals(1, dict.size());
    assertEquals(1, dict.getSuggestions("foo").size());
    assertEquals("foobar", dict.getSuggestions("foo").get(0).getSuggested());
  }
}
