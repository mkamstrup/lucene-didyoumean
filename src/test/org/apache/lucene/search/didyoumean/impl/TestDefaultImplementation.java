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
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.didyoumean.session.QueryGoalNode;
import org.apache.lucene.search.didyoumean.Suggester;
import org.apache.lucene.search.didyoumean.Suggestion;
import org.apache.lucene.search.didyoumean.SuggestionFacade;
import org.apache.lucene.search.didyoumean.dictionary.MemoryDictionary;
import org.apache.lucene.search.didyoumean.secondlevel.token.SecondLevelTokenPhraseSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.NgramTokenSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.TermEnumIterator;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.index.facade.DirectoryIndexFacade;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.search.didyoumean.session.MemoryQuerySessionManager;
import org.apache.lucene.search.didyoumean.session.QueryGoalTreeExtractor;
import org.apache.lucene.store.RAMDirectory;

import java.io.File;

/**
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: Jul 31, 2006
 *         Time: 12:26:07 PM
 */
public class TestDefaultImplementation extends TestCase {

  private SuggestionFacade<Integer> suggestionFacade;

  @Override
  protected void setUp() throws Exception {

    suggestionFacade = new SuggestionFacade<Integer>(new MemoryDictionary(), new MemoryQuerySessionManager<Integer>(), new DefaultSuggester(), new DefaultTrainer(), new DefaultQueryGoalTreeExtractor<Integer>(), new DefaultAprioriCorpusFactory());

    // your primary index that suggestions must match.
    IndexFacade aprioriIndex = new DirectoryIndexFacade(new RAMDirectory());
    aprioriIndex.indexWriterFactory(null, true).close();
    String aprioriField = "title";

    // build the ngram suggester
    IndexFacade ngramIndex = new DirectoryIndexFacade(new RAMDirectory());
    ngramIndex.indexWriterFactory(null, true).close();
    NgramTokenSuggester ngramSuggester = new NgramTokenSuggester(ngramIndex);

    IndexReader aprioriReader = aprioriIndex.indexReaderFactory();
    ngramSuggester.indexDictionary(new TermEnumIterator(aprioriReader, aprioriField));

    // the greater the better results but with a longer response time.
    int maxSuggestionsPerToken = 3;

    // add ngram suggester wrapped in a single token phrase suggester as second level suggester.
    suggestionFacade.getDictionary().getPrioritiesBySecondLevelSuggester().put(new SecondLevelTokenPhraseSuggester(ngramSuggester, aprioriField, false, maxSuggestionsPerToken, new WhitespaceAnalyzer(), aprioriIndex), 1d);
  }

  public void testBasicTraining() throws Exception {
    QueryGoalNode<Integer> node;

    node = new QueryGoalNode<Integer>(null, "heroes of nmight and magic", 3);
    node = new QueryGoalNode<Integer>(node, "heroes of night and magic", 3);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic", 10);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);

    node = new QueryGoalNode<Integer>(null, "heroes of night and magic", 3);
    node = new QueryGoalNode<Integer>(node, "heroes of knight and magic", 7);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic", 20);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);

    node = new QueryGoalNode<Integer>(null, "heroes of might and magic", 20, 1l);
    suggestionFacade.trainSessionQueryTree(node);

    node = new QueryGoalNode<Integer>(null, "heroes of night and magic", 7, 0l);
    node = new QueryGoalNode<Integer>(node, "heroes of light and magic", 14, 1l);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic", 2, 6l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);

    node = new QueryGoalNode<Integer>(null, "heroes of night and magic", 4, 0l);
    node = new QueryGoalNode<Integer>(node, "heroes of knight and magic", 17, 1l);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic", 2, 2l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);

    assertEquals("heroes of might and magic", suggestionFacade.didYouMean("heroes of light and magic"));
    assertEquals("heroes of might and magic", suggestionFacade.didYouMean("heroes of night and magic"));

    assertEquals("heroes of might and magic", suggestionFacade.didYouMean("heroes ofnight andmagic"));

    // todo fight has not been trained. use trie .
    //assertEquals("heroes of might and magic", spellChecker.didYouMean(didYouMean.getDictionary(), "heroes of fight and magic"));


  }


  public void testSynonymTraining() throws Exception {
    QueryGoalNode<Integer> node;


    node = new QueryGoalNode<Integer>(null, "heroes of night and magic", 12, 0l);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic", 30, 1l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);

    node = new QueryGoalNode<Integer>(null, "heroes of night and magic", 13, 0l);
    node = new QueryGoalNode<Integer>(node, "heroes of knight and magic", 14, 1l);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic", 22, 2l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    node = new QueryGoalNode<Integer>(node, "homm", 17, 4l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);


    assertEquals("heroes of might and magic", suggestionFacade.didYouMean("homm"));


    node = new QueryGoalNode<Integer>(null, "homm", 17, 0l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);


    node = new QueryGoalNode<Integer>(null, "heroes of night and magic", 12, 0l);
    node = new QueryGoalNode<Integer>(node, "heroes of light and magic", 4, 1l);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic", 17, 2l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    node = new QueryGoalNode<Integer>(node, "homm", 34, 4l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    node.new Inspection(24, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);


    node = new QueryGoalNode<Integer>(null, "heroes of night and magic", 3, 0l);
    node = new QueryGoalNode<Integer>(node, "heroes of knight and magic", 3, 1l);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic", 22, 2l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);

    assertEquals("homm", suggestionFacade.didYouMean("heroes of might and magic"));
    assertEquals("heroes of might and magic", suggestionFacade.didYouMean("heroes of night and magic"));

    // todo fight has not been trained. navigate the shortest way down the trie to find suggestion?
    //assertEquals("heroes of might and magic", spellChecker.didYouMean(didYouMean.getDictionary(), "heroes of fight and magic"));

    assertEquals("homm", suggestionFacade.didYouMean("heroes of might and magic"));
    assertEquals("heroes of might and magic", suggestionFacade.didYouMean("homm"));

  }

  public void testPrefixedSynonymTraining() throws Exception {
    QueryGoalNode<Integer> node;


    node = new QueryGoalNode<Integer>(null, "heroes of might and magic 2", 22, 0l);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic ii", 13, 1l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);



    node = new QueryGoalNode<Integer>(null, "heroes of might and magic 2", 22, 0l);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic ii", 13, suggestionFacade.didYouMean("heroes of might and magic 2", 1)[0].getSuggested(), 1l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);



    node = new QueryGoalNode<Integer>(null, "heroes of might and magic ii", 22, 0l);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic 2", 13, suggestionFacade.didYouMean("heroes of might and magic ii", 1)[0].getSuggested(), 1l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);

    node = new QueryGoalNode<Integer>(null, "heroes of might and magic ii", 22, 0l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    node = new QueryGoalNode<Integer>(node, "heroes of might and magic 2", 13, suggestionFacade.didYouMean("heroes of might and magic ii", 1)[0].getSuggested(), 2l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);

    assertEquals("heroes of might and magic 2", suggestionFacade.didYouMean("heroes of might and magic ii"));
    assertEquals("heroes of might and magic ii", suggestionFacade.didYouMean("heroes of might and magic 2"));

  }

  public void testNegativeAdaptation() throws Exception {

    testSynonymTraining();


    Suggestion aprioriSuggestion = suggestionFacade.getDictionary().getSuggestions("heroesofmightandmagic").get(0);
    assertEquals("homm", aprioriSuggestion.getSuggested());

    for (int i = 0; i < 10; i++) {
      QueryGoalNode<Integer> node;
      node = new QueryGoalNode<Integer>(null, "heroes of might and magic", 22, 0l);
      node.new Inspection(23, QueryGoalNode.GOAL);
      suggestionFacade.trainSessionQueryTree(node);
    }

    Suggestion resultSuggestion = suggestionFacade.getDictionary().getSuggestions("heroesofmightandmagic").get(0);

    assertFalse("homm".equals(resultSuggestion.getSuggested()));    

  }


  public void testWhiteSpace() throws Exception {

    QueryGoalNode<Integer> node;

    node = new QueryGoalNode<Integer>(null, "the davinci code", 22, 0l);
    node = new QueryGoalNode<Integer>(node, "the da vinci code", 13, 1l);
    node.new Inspection(23, QueryGoalNode.GOAL);
    suggestionFacade.trainSessionQueryTree(node);

    assertEquals("the da vinci code", suggestionFacade.didYouMean("thedavincicode"));
    assertEquals("the da vinci code", suggestionFacade.didYouMean("the dav-inci code"));

  }

}
