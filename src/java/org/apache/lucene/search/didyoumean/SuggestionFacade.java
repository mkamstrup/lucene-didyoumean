package org.apache.lucene.search.didyoumean;

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


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.index.facade.IndexFacadeFactory;
import org.apache.lucene.index.facade.InstantiatedIndexFacade;
import org.apache.lucene.search.didyoumean.dictionary.Dictionary;
import org.apache.lucene.search.didyoumean.dictionary.QueryException;
import org.apache.lucene.search.didyoumean.secondlevel.token.MultiTokenSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.SecondLevelTokenPhraseSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.NgramTokenSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.TermEnumIterator;
import org.apache.lucene.search.didyoumean.session.*;
import org.apache.lucene.store.instantiated.InstantiatedIndex;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Consumer interface for the adaptive user session analyzing suggester.
 * <p/>
 * The simplest implementation would look something like this:
 * <pre>
 * SuggestionFacade facade = new SuggestionFacade(new File("data"));
 * facade.getDictionary().getPrioritiesBySecondLevelSuggester().putAll(facade.secondLevelSuggestionFactory());
 * ...
 * QuerySession session = facade.getQuerySessionManager().sessionFactory();
 * ...
 * String query = "heros of mght and magik";
 * Hits hits = searcher.search(queryFactory(query));
 * String suggested = facade.didYouMean(query);
 * session.query(query, hits.length(), suggested);
 * ...
 * facade.getQuerySessionManager().getSessionsByID().put(session);
 * ...
 * facade.trainExpiredSessions();
 * ...
 * facade.close();
 * </pre>
 * <p/>
 * The trainer is fed with trees of {@link QueryGoalNode} instances. Each such
 * tree represent the events that took place while a user tried to find content within a certain context: a goal tree.
 * An instance of {@link QueryGoalTreeExtractor} will help you to find and isolate
 * all the goals in a tree representing a complete user session, as they sometimes contain more than one.
 * <p/>
 * It is up to the trainer and the suggester to decide how suggestions in the dictionary are stored and modified. Thus
 * all trainers and suggesters might not be compatible with each other.
 * <p/>
 * {@link org.apache.lucene.search.didyoumean.impl.DefaultQueryGoalTreeExtractor}
 * {@link org.apache.lucene.search.didyoumean.impl.DefaultTrainer}
 * {@link org.apache.lucene.search.didyoumean.impl.DefaultSuggester}
 * <p/>
 * {@link org.apache.lucene.search.didyoumean.impl.DefaultAprioriCorpusFactory}
 * <p/>
 * <p/>
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-feb-17
 *         Time: 02:32:50         
 */
public class SuggestionFacade<R> {

  private Dictionary dictionary;
  private Suggester suggester;

  private QuerySessionManager<R> querySessionManager;
  private QueryGoalTreeExtractor<R> queryGoalTreeExtractor;

  private Trainer<R> trainer;

  private AprioriCorpusFactory aprioriCorpusFactory;

  public SuggestionFacade(Dictionary dictionary, QuerySessionManager<R> querySessionManager, Suggester suggester, Trainer<R> trainer, QueryGoalTreeExtractor<R> queryGoalTreeExtractor, AprioriCorpusFactory aprioriCorpusFactory) throws QueryException {
    this.dictionary = dictionary;
    this.querySessionManager = querySessionManager;
    this.suggester = suggester;
    this.trainer = trainer;
    this.queryGoalTreeExtractor = queryGoalTreeExtractor;
    this.aprioriCorpusFactory = aprioriCorpusFactory;

    didYouMean("warmup"); // this is a bugfix    
  }


  public void close() throws IOException {
    dictionary.close();
    querySessionManager.close();
  }

  public Suggestion[] didYouMean(String query, int n) throws QueryException {
    return getSuggester().didYouMean(getDictionary(), query, n);
  }

  public String didYouMean(String query) throws QueryException {
    // todo remove debug
    long ms = System.currentTimeMillis();
    String ret = getSuggester().didYouMean(getDictionary(), query);
    ms = System.currentTimeMillis() - ms;
    System.out.println(ms + "ms\t " + query + " -> " + ret);
    return ret;
  }

  /**
   * Gathers and trains all expired query sessions from the query session manager
   */
  public synchronized void trainExpiredQuerySessions() throws QueryException {
    trainExpiredQuerySessions(1);
  }

  /**
   * Gathers and trains all expired query sessions from the query session manager
   */
  public synchronized void trainExpiredQuerySessions(int maxThreads) throws QueryException {
    trainExpiredQuerySessions(maxThreads, 10000);
  }

  /**
   * Gathers and trains all expired query sessions from the query session manager
   */
  public synchronized void trainExpiredQuerySessions(int maxThreads, int batchSize) throws QueryException {
    int count = 0;
    for (; ;) {

      final ConcurrentLinkedQueue<QuerySession<R>> queue = new ConcurrentLinkedQueue<QuerySession<R>>();

      for (QuerySession querySession : getQuerySessionManager()) {
        if (querySession.isExpired()) {
          count++;
          queue.add((QuerySession<R>) querySession);
          if (queue.size() == batchSize) {
            break;
          }
        }
      }

      if (queue.size() == 0) {
        break;
      }

      Thread[] threads = new Thread[maxThreads];
      for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread(new Runnable() {
          public void run() {
            QuerySession<R> session;
            while ((session = queue.poll()) != null) {
              try {
                trainSession(session);
                getQuerySessionManager().remove(session.getId());
              } catch (SessionException e) {
                e.printStackTrace(); // FIXME
              } catch (QueryException e) {
                  e.printStackTrace();  //FIXME
              }
            }
          }
        });
        threads[i].start();
      }
      for (Thread thread : threads) {
        try {
          thread.join();
        } catch (InterruptedException ie) {
          ie.printStackTrace(); // FIXME
        }
      }

      System.out.println(count + " sessions trained.");
    }

    System.out.println("Finished training a total of " + count + " sessions.");

  }

  /**
   * Extracts multiple goal trees from a tree containing all queries in a session,
   * and queues each such goal tree to the trainer.
   *
   * @param session the session to be trained
   * @throws QueryException
   */
  public void trainSession(QuerySession<R> session) throws QueryException {
    trainSessionQueryTree(session.getNodes().get(0));
  }

  /**
   * Extracts multiple goal trees from a tree containing all queries in a session,
   * and queues each such goal tree to the trainer.
   *
   * @param session any node (preferably the root) of a query goal tree
   * @throws QueryException
   */
  public void trainSessionQueryTree(QueryGoalNode<R> session) throws QueryException {
    for (QueryGoalNode<R> goalTreeRoot : getQueryGoalTreeExtractor().extractGoalRoots(session.getRoot())) {
      getTrainer().trainGoalTree(getDictionary(), goalTreeRoot);
    }
  }


  /**
   * Compiles algorithmic second level suggesters based on the data in the dictionary.
   *
   * @return
   * @throws IOException
   * @throws QueryException
   */
  public Map<SecondLevelSuggester, Double> secondLevelSuggestionFactory() throws IOException, QueryException {
    return secondLevelSuggestionFactory(
        new IndexFacadeFactory() {
          public IndexFacade factory() throws IOException {
            return new InstantiatedIndexFacade(new InstantiatedIndex());
          }
        }
    );
  }

  /**
   * Compiles algorithmic second level suggesters based on the data in the dictionary.
   *
=   * @param indexFacadeFactory index in which to store ngrams created by all terms in the a priori corpus
   * @return a second level suggester
   * @throws IOException
   * @throws QueryException
   */
  public Map<SecondLevelSuggester, Double> secondLevelSuggestionFactory(IndexFacadeFactory indexFacadeFactory) throws IOException, QueryException {
    return secondLevelSuggestionFactory(null, null, null, indexFacadeFactory, "apriori", indexFacadeFactory, 2, 7);
  }

  /**
   * Compiles algorithmic second level suggesters based on the data in the dictionary.
   *
   * @param systemIndex system index user queries are placed in. optional.
   * @param systemIndexField field in system index used to extract ngram tokens. must not be null if systemIndex is available.
   * @param systemNgramIndexFacadeFactory
   * @param aprioriIndexFacadeFactory index in which to store the created a priori corpus
   * @param aprioriField field in a priori index to store values
   * @param aprioriNgramIndexFacadeFactory index in which to store ngrams created by all terms in the a priori corpus
   * @param minNgramSize minimum ngram size. 2 makes sense.
   * @param maxSuggestionsPerWord maximum number of suggestions per word in matrix. A maximum of n^w queries will be placed.
   * @return a second level suggester
   * @throws IOException
   * @throws QueryException
   */
  public Map<SecondLevelSuggester, Double> secondLevelSuggestionFactory(IndexFacade systemIndex, String systemIndexField, IndexFacadeFactory systemNgramIndexFacadeFactory, IndexFacadeFactory aprioriIndexFacadeFactory, String aprioriField, IndexFacadeFactory aprioriNgramIndexFacadeFactory, int minNgramSize, int maxSuggestionsPerWord) throws IOException {
    if (systemIndex != null && systemIndexField == null) {
      throw new NullPointerException("systemIndexField must be set if systemIndex is present.");
    }

    Map<SecondLevelSuggester, Double> ret = new HashMap<SecondLevelSuggester, Double>(2);

    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT, Collections.emptySet());


    System.out.println("Creating a priori corpus...");
    IndexFacade aprioriIndex = aprioriIndexFacadeFactory.factory();

    getAprioriCorpusFactory().factory(getDictionary(), getSuggester(), aprioriIndex, aprioriField, analyzer);

    System.out.println("Creating ngram index from a priori corpus terms...");
    IndexFacade aprioriNgramIndex = aprioriNgramIndexFacadeFactory.factory();
    aprioriNgramIndex.indexWriterFactory(null, true).close(); // reset
    NgramTokenSuggester ngramTokenSuggester = new NgramTokenSuggester(aprioriNgramIndex);
    IndexReader aprioriIndexReader = aprioriIndex.indexReaderFactory();
    ngramTokenSuggester.indexDictionary(new TermEnumIterator(aprioriIndexReader, aprioriField), minNgramSize);
    aprioriIndexReader.close();

    ret.put(new SecondLevelTokenPhraseSuggester(ngramTokenSuggester, aprioriField, false, maxSuggestionsPerWord, analyzer, aprioriIndex), 3d);

    if (systemIndex != null) {
      System.out.println("Creating ngram index from system corpus terms...");
      IndexFacade systemNgramIndex = systemNgramIndexFacadeFactory.factory();
      systemNgramIndex.indexWriterFactory(null, true).close(); // reset
      NgramTokenSuggester sysetmNgramTokenSuggester = new NgramTokenSuggester(systemNgramIndex);
      IndexReader systemIndexReader = systemIndex.indexReaderFactory();
      ngramTokenSuggester.indexDictionary(new TermEnumIterator(systemIndexReader, systemIndexField), minNgramSize);
      systemIndexReader.close();

      ret.put(new MultiTokenSuggester(sysetmNgramTokenSuggester, systemIndexField, true, maxSuggestionsPerWord, analyzer, systemIndex), 1d);
    }

    return ret;
  }


  public Dictionary getDictionary() {
    return dictionary;
  }

  public void setDictionary(Dictionary dictionary) {
    this.dictionary = dictionary;
  }

  public Suggester getSuggester() {
    return suggester;
  }

  public void setSuggester(Suggester suggester) {
    this.suggester = suggester;
  }

  public Trainer<R> getTrainer() {
    return trainer;
  }

  public void setTrainer(Trainer<R> trainer) {
    this.trainer = trainer;
  }

  public QueryGoalTreeExtractor<R> getQueryGoalTreeExtractor() {
    return queryGoalTreeExtractor;
  }

  public void setQueryGoalTreeExtractor(QueryGoalTreeExtractor<R> queryGoalTreeExtractor) {
    this.queryGoalTreeExtractor = queryGoalTreeExtractor;
  }


  public QuerySessionManager<R> getQuerySessionManager() {
    return querySessionManager;
  }

  public void setQuerySessionManager(QuerySessionManager<R> querySessionManager) {
    this.querySessionManager = querySessionManager;
  }


  public AprioriCorpusFactory getAprioriCorpusFactory() {
    return aprioriCorpusFactory;
  }

  public void setAprioriCorpusFactory(AprioriCorpusFactory aprioriCorpusFactory) {
    this.aprioriCorpusFactory = aprioriCorpusFactory;
  }
}
