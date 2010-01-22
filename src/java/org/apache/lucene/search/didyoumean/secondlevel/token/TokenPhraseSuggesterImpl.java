package org.apache.lucene.search.didyoumean.secondlevel.token;
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
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.didyoumean.Suggestion;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import java.io.IOException;

/**
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-feb-03
 *         Time: 08:11:34
 */
public class TokenPhraseSuggesterImpl extends TokenPhraseSuggester {

  private IndexFacade aprioriIndex;
  private IndexReader aprioriReader;
  private IndexSearcher aprioriSearcher;


  protected boolean isUpdateSuggestionsOrder() {
    return true;
  }

  public TokenPhraseSuggesterImpl(TokenSuggester tokenSuggester, String aprioriIndexField, boolean defaultSuggestMorePopularTokensOnly, int defaultMaxSuggestionsPerToken, Analyzer phraseAnalyzer, IndexFacade aprioriIndex) throws IOException {
    super(tokenSuggester, aprioriIndexField, defaultSuggestMorePopularTokensOnly, defaultMaxSuggestionsPerToken, phraseAnalyzer);
    this.aprioriIndex = aprioriIndex;
    this.aprioriReader = aprioriIndex.indexReaderFactory();
    this.aprioriSearcher = new IndexSearcher(aprioriReader);
  }

  protected Query suggestionAprioriQueryFactory(Suggestion[] suggestions) {
    SpanTermQuery[] clauses = new SpanTermQuery[suggestions.length];
    for (int i = 0; i < suggestions.length; i++) {
      clauses[i] = new SpanTermQuery(new Term(getAprioriIndexField(), suggestions[i].getSuggested()));
    }
    return new SpanNearQuery(clauses, 5, false);
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
