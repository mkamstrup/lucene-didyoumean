package org.apache.lucene.search.didyoumean;

import junit.framework.TestCase;

/**
 * FIXME: Missing class docs for org.apache.lucene.search.didyoumean.SuggestionPriorityQueueTest
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Jan 26, 2010
 */
public class SuggestionPriorityQueueTest extends TestCase {

  public void setUp() {

  }

  public void testOrdering() {
    SuggestionPriorityQueue q = new SuggestionPriorityQueue(5);
    Suggestion s1 = new Suggestion("foo", 0.5d, 10);
    Suggestion s2 = new Suggestion("foo", 1d, 10);
    Suggestion s3 = new Suggestion("foo", 1.5d, 10);

    assertTrue(q.lessThan(s1, s2));
    assertTrue(q.lessThan(s2, s3));
    assertTrue(q.lessThan(s1, s3));
  }

}
