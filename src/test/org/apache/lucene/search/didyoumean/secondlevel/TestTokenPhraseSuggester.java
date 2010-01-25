package org.apache.lucene.search.didyoumean.secondlevel;

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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.index.facade.IndexWriterFacade;
import org.apache.lucene.index.facade.DirectoryIndexFacade;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.search.didyoumean.secondlevel.token.TokenPhraseSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.TokenPhraseSuggesterImpl;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.NgramTokenSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.TermEnumIterator;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.util.Collections;

/**
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-feb-02
 *         Time: 04:36:49
 */
public class TestTokenPhraseSuggester extends TestCase {

  // private static Log log = LogFactory.getLog(TestNgramPhraseSuggester.class);
  // private static long serialVersionUID = 1l;


  public void testPhraseSuggester() throws Exception {

    IndexFacade aprioriIndex = new DirectoryIndexFacade(new RAMDirectory());
    aprioriIndex.indexWriterFactory(null, true, IndexWriter.MaxFieldLength.LIMITED).close();

    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT, Collections.EMPTY_SET);
    final String field = "field";

    // the apriori index - used to build ngrams and to check if suggestions are any good.
    IndexWriterFacade indexWriter = aprioriIndex.indexWriterFactory(analyzer, true, IndexWriter.MaxFieldLength.LIMITED);
    addDocument(indexWriter, field, "heroes of might and magic III complete", Field.TermVector.WITH_POSITIONS_OFFSETS);
    addDocument(indexWriter, field, "it might be the best game ever made", Field.TermVector.WITH_POSITIONS_OFFSETS);
    addDocument(indexWriter, field, "forget about the rest", Field.TermVector.WITH_POSITIONS_OFFSETS);
    addDocument(indexWriter, field, "it's all in the fame", Field.TermVector.WITH_POSITIONS_OFFSETS);
    addDocument(indexWriter, field, "lost in translation", Field.TermVector.WITH_POSITIONS_OFFSETS);
    addDocument(indexWriter, field, "on the little hill there is a little tree, never have I seen such a little tree", Field.TermVector.NO);
    indexWriter.close();

    IndexReader reader = aprioriIndex.indexReaderFactory();

    // the single token suggester
    IndexFacade ngramIndex = new DirectoryIndexFacade(new RAMDirectory());
    ngramIndex.indexWriterFactory(null, true, IndexWriter.MaxFieldLength.LIMITED).close();

    NgramTokenSuggester tokenSuggester = new NgramTokenSuggester(ngramIndex);
    tokenSuggester.indexDictionary(new TermEnumIterator(reader, field), 2);

    // the phrase suggester backed by single token suggester
    TokenPhraseSuggester phraseSuggester = new TokenPhraseSuggesterImpl(tokenSuggester, field, false, 3, analyzer, aprioriIndex);

    assertEquals("lost in translation", phraseSuggester.didYouMean("lost on translation"));
    assertEquals("heroes might magic", phraseSuggester.didYouMean("magic light heros"));
    assertEquals("heroes of might and magic", phraseSuggester.didYouMean("heros on light and magik"));
    assertEquals("best game made", phraseSuggester.didYouMean("game best made"));
    assertEquals("game made", phraseSuggester.didYouMean("made game"));
    assertEquals("game made", phraseSuggester.didYouMean("made lame"));
    assertEquals("the game", phraseSuggester.didYouMean("the game"));
    assertEquals("in the fame", phraseSuggester.didYouMean("in the game"));
    assertEquals("made", phraseSuggester.didYouMean("mede"));
    assertEquals(0, phraseSuggester.suggest("may game", 3).size());

    // make sure that non term positions act as they should

    assertEquals("lost in translation", phraseSuggester.didYouMean("in lost translation"));       
    assertEquals("have never i seen", phraseSuggester.didYouMean("have nevrer  i seen"));

    reader.close();


  }

  private void addDocument(IndexWriterFacade indexWriter, String field, String text, Field.TermVector termVector) throws Exception {
    Document document = new Document();
    document.add(new Field(field, text, Field.Store.YES, Field.Index.ANALYZED, termVector));
    indexWriter.addDocument(document);
  }

}
