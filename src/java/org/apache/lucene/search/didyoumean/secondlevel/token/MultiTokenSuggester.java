package org.apache.lucene.search.didyoumean.secondlevel.token;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.search.*;
import org.apache.lucene.search.didyoumean.SecondLevelSuggester;
import org.apache.lucene.search.didyoumean.Suggestion;
import org.apache.lucene.search.didyoumean.SuggestionPriorityQueue;

import java.io.IOException;
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


/**
 * Uses term queries rather than span near queries.
 *
 * @author karl wettin <mailto:karl.wettin@gmail.com>
 * Date: 2007-okt-23
 * Time: 04:33:29
 */
public class MultiTokenSuggester extends TokenPhraseSuggester implements SecondLevelSuggester {

  private IndexFacade aprioriIndex;
  private IndexReader aprioriReader;
  private IndexSearcher aprioriSearcher;


  protected boolean isUpdateSuggestionsOrder() {
    return false;
  }

  public boolean hasPersistableSuggestions() {
    return false;
  }


  /**
   *
   * @param tokenSuggester
   * @param aprioriIndexField
   * @param defaultSuggestMorePopularTokensOnly it makes sense setting this to true
   * @param defaultMaxSuggestionsPerToken 
   * @param queryAnalyzer
   * @param aprioriIndex
   * @throws IOException
   */
  public MultiTokenSuggester(TokenSuggester tokenSuggester, String aprioriIndexField, boolean defaultSuggestMorePopularTokensOnly, int defaultMaxSuggestionsPerToken, Analyzer queryAnalyzer, IndexFacade aprioriIndex) throws IOException {
    super(tokenSuggester, aprioriIndexField, defaultSuggestMorePopularTokensOnly, defaultMaxSuggestionsPerToken, queryAnalyzer);
    this.aprioriIndex = aprioriIndex;
    this.aprioriReader = aprioriIndex.indexReaderFactory();
    this.aprioriSearcher = new IndexSearcher(aprioriReader);
  }

  public SuggestionPriorityQueue suggest(String query) {
    return suggest(query, 1);
  }

  protected Query suggestionAprioriQueryFactory(Suggestion[] suggestions) {
    BooleanQuery bq = new BooleanQuery();
    for (Suggestion suggestion : suggestions) {
      bq.add(new TermQuery(new Term(getAprioriIndexField(), suggestion.getSuggested())), BooleanClause.Occur.MUST);
    }
    return bq;
  }

  public IndexFacade getAprioriIndex() {
    return aprioriIndex;
  }

  protected IndexReader getAprioriReader() {
    return aprioriReader;
  }

  protected IndexSearcher getAprioriSearcher() {
    return aprioriSearcher;
  }

}
