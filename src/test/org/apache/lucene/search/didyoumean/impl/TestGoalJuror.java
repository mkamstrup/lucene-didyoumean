package org.apache.lucene.search.didyoumean.impl;

/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


import junit.framework.TestCase;
import org.apache.lucene.search.didyoumean.dictionary.QueryException;
import org.apache.lucene.search.didyoumean.session.QuerySession;
import org.apache.lucene.search.didyoumean.SuggestionFacade;
import org.apache.lucene.search.didyoumean.dictionary.MemoryDictionary;
import org.apache.lucene.search.didyoumean.session.MemoryQuerySessionManager;

import java.io.*;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-feb-24
 *         Time: 00:13:05
 */
public class TestGoalJuror extends TestCase {

  // private static Log log = LogFactory.getLog(TestGoalJuror.class);
  // private static long serialVersionUID = 1l;

  private File dataRootPath = new File("data");

  @Override
  protected void setUp() throws Exception {
    if (!dataRootPath.exists()) {
      dataRootPath.mkdirs();
    }
  }

  /** left out here for closure reasons */
  private SuggestionFacade<Integer> facade;

  private SuggestionFacade<Integer> suggestionFacadeFactory() throws QueryException {
    return new SuggestionFacade<Integer>(new MemoryDictionary(), new MemoryQuerySessionManager<Integer>(), new DefaultSuggester(), null, new DefaultQueryGoalTreeExtractor<Integer>(), new DefaultAprioriCorpusFactory());
  }

  public void testConcept() throws Exception {

    facade = suggestionFacadeFactory();

    QuerySession<Integer> session = facade.getQuerySessionManager().querySessionFactory();
    session.query("heroes of night and magic", 0, null, 1l);
    session.query("heroes of knight and magic", 0, null, 2l);
    session.query("heroes of might and magic", 10, null, 3l);

    assertTrue(session.isExpired());
    facade.getQuerySessionManager().put(session);

    facade.trainExpiredQuerySessions();

    assertEquals("heroes of might and magic", facade.didYouMean("heroes of night and magic"));
    assertEquals("heroes of might and magic", facade.didYouMean("heroes of knight and magic"));
    assertEquals("heroes of might and magic", facade.didYouMean("heroes of might and magic"));

    facade.close();

  }


  public void testImportData() throws Exception {

    // load 3,500,000 user queries with session id, time stamp and number of hits.
    // no goals specified. let the goal juror figure that out.
    // all queries are lower cased.

    facade = new SuggestionFacade<Integer>(new MemoryDictionary(), new MemoryQuerySessionManager<Integer>(), new DefaultSuggester(), null, new DefaultQueryGoalTreeExtractor<Integer>(), new DefaultAprioriCorpusFactory());


    if (facade.getDictionary().size() == 0) {

      File testDataFile = new File(dataRootPath, "queries_grouped.txt");
      if (!testDataFile.exists()) {
        System.out.println("Downloading test data...");
        InputStream input = new GZIPInputStream(new URL("http://ginandtonique.org/~kalle/LUCENE-626/queries_grouped.txt.gz").openStream());
        OutputStream output = new FileOutputStream(testDataFile);
        byte[] buf = new byte[102400];
        int len;
        while ((len = input.read(buf)) > -1) {
          output.write(buf, 0, len);
        }
        input.close();
        output.close();
      }

      System.out.println("Importing test data...");


      int cnt = 0;
      int total = 0;

      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(testDataFile), "UTF8"));
      String line;

      assertEquals(5, br.readLine().split("\t").length);

      String sessionID = "";
      QuerySession<Integer> querySession = null;

      while ((line = br.readLine()) != null) {
        String[] values = line.split("\t");
        if (!values[0].equals(sessionID)) {
          if (querySession != null) {
            facade.getQuerySessionManager().put(querySession);
            if (cnt == 10000) {

              // throws a nasty exception!
              // http://forums.oracle.com/forums/thread.jspa?threadID=577588&tstart=0

//              // train while still creating...
//              if (total == 200000) {
//                new Thread(new Runnable() {
//                  public void run() {
//                    try {
//                      facade.trainExpiredQuerySessions(2);
//                    } catch (Exception e) {
//                      e.printStackTrace();
//                      // todo
//                    }
//                  }
//                }).start();
//              }

              System.out.println(total + " sessions loaded.");
              cnt = 0;
            }
          }
          querySession = new QuerySession<Integer>();
          querySession.setId(values[0]);
          sessionID = values[0];
          cnt++;
          total++;
        }
        String query = values[4].trim().toLowerCase();
        int hits = Integer.valueOf(values[3]);
        long timestamp = Long.valueOf(values[1]) * 1000;
        querySession.query(query, hits, null, timestamp);
      }
      facade.getQuerySessionManager().put(querySession);
      br.close();

      facade.trainExpiredQuerySessions(4);

      System.out.println("Reopening persistent dictionary.");
      facade.close();
      facade = suggestionFacadeFactory();

    }


    System.out.println("Running assertation tests...");
    // run some tests without the second level suggestions,
    // i.e. user behavioral data only. no ngrams or so.

    assertEquals("pirates of the caribbean", facade.didYouMean("pirates ofthe caribbean"));

    assertEquals("pirates of the caribbean", facade.didYouMean("pirates of the carribbean"));

    assertEquals("pirates caribbean", facade.didYouMean("pirates carricean"));
    assertEquals("pirates of the caribbean", facade.didYouMean("pirates of the carriben"));
    assertEquals("pirates of the caribbean", facade.didYouMean("pirates of the carabien"));
    assertEquals("pirates of the caribbean", facade.didYouMean("pirates of the carabbean"));

    assertEquals("pirates of the caribbean", facade.didYouMean("pirates og carribean"));

    assertEquals("pirates of the caribbean soundtrack", facade.didYouMean("pirates of the caribbean music"));
    assertEquals("pirates of the caribbean score", facade.didYouMean("pirates of the caribbean soundtrack"));

    assertEquals("pirate of caribbean", facade.didYouMean("pirate of carabian"));
    assertEquals("pirates of caribbean", facade.didYouMean("pirate of caribbean"));
    assertEquals("pirates of caribbean", facade.didYouMean("pirates of caribbean"));

    // depening on how many hits and goals are noted with these two queries
    // perhaps the delta should be added to a synonym dictionary? 
    assertEquals("homm iv", facade.didYouMean("homm 4"));





    // add token phrase suggester: requires a lot of memory!!

    // not yet known.. and we have no second level yet.
    assertNull(facade.didYouMean("the pilates"));

    System.out.println("Creating second level suggesters from comon typos...");
    // use the dictionary built from user queries to build the token phrase and ngram suggester.

    facade.getDictionary().getPrioritiesBySecondLevelSuggester().putAll(facade.secondLevelSuggestionFactory());
    System.out.println("Done creating second level suggesters from comon typos...");

    assertEquals("the pirates", facade.didYouMean("the pilates"));
    // now it's learned, should take no time.
    assertEquals("the pirates", facade.didYouMean("the pilates"));




    // typos
    assertEquals("heroes of might and magic", facade.didYouMean("heroes of fight and magic"));
    assertEquals("heroes of might and magic", facade.didYouMean("heroes of right and magic"));
    assertEquals("heroes of might and magic", facade.didYouMean("heroes of magic and light"));

    // composite dictionary key not learned yet..
    assertEquals(null, facade.didYouMean("heroesof lightand magik"));
    // learn
    assertEquals("heroes of might and magic", facade.didYouMean("heroes of light and magik"));
    // test
    assertEquals("heroes of might and magic", facade.didYouMean("heroesof lightand magik"));

    // wrong term order
    assertEquals("heroes of might and magic", facade.didYouMean("heroes of magic and might"));


    System.out.println("Reopening persistent dictionary.");
    facade.close();
    facade = suggestionFacadeFactory();
    System.out.println("Persistent dictionary reopened.");

    // same tests again, to make sure persistency works.

    // should be known by now
    assertEquals("the pirates", facade.didYouMean("the pilates"));

    // run some tests without the second level suggestions,
    // i.e. user behavioral data only. no ngrams or so.

    assertEquals("pirates of the caribbean", facade.didYouMean("pirates ofthe caribbean"));

    assertEquals("pirates of the caribbean", facade.didYouMean("pirates of the carribbean"));

    assertEquals("pirates caribbean", facade.didYouMean("pirates carricean"));
    assertEquals("pirates of the caribbean", facade.didYouMean("pirates of the carriben"));
    assertEquals("pirates of the caribbean", facade.didYouMean("pirates of the carabien"));
    assertEquals("pirates of the caribbean", facade.didYouMean("pirates of the carabbean"));

    assertEquals("pirates of the caribbean", facade.didYouMean("pirates og carribean"));

    assertEquals("pirates of the caribbean soundtrack", facade.didYouMean("pirates of the caribbean music"));
    assertEquals("pirates of the caribbean score", facade.didYouMean("pirates of the caribbean soundtrack"));

    assertEquals("pirate of caribbean", facade.didYouMean("pirate of carabian"));
    assertEquals("pirates of caribbean", facade.didYouMean("pirate of caribbean"));
    assertEquals("pirates of caribbean", facade.didYouMean("pirates of caribbean"));

    // depening on how many hits and goals are noted with these two queries
    // perhaps the delta should be added to a synonym dictionary?
    assertEquals("homm iv", facade.didYouMean("homm 4"));


  }


}
