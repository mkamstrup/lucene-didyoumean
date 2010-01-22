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
import org.apache.lucene.search.didyoumean.SecondLevelSuggester;
import org.apache.lucene.search.didyoumean.SuggestionPriorityQueue;

import java.io.IOException;

/**
 * Makes TokenPhraseSuggesterImpl a SecondLevelSuggester.
 *
 * todo: this is an ugly class. decoration? 
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com> 
 * Date: 2007-feb-17
 * Time: 08:03:01
 */
public class SecondLevelTokenPhraseSuggester extends TokenPhraseSuggesterImpl implements SecondLevelSuggester {

  public SecondLevelTokenPhraseSuggester(TokenSuggester tokenSuggester, String aprioriIndexField, boolean defaultSuggestMorePopularTokensOnly, int defaultMaxSuggestionsPerToken, Analyzer phraseAnalyzer, IndexFacade aprioriIndex) throws IOException {
    super(tokenSuggester, aprioriIndexField, defaultSuggestMorePopularTokensOnly, defaultMaxSuggestionsPerToken, phraseAnalyzer, aprioriIndex);
  }

  public SuggestionPriorityQueue suggest(String query) {
    return suggest(query, 1);
  }


  public boolean hasPersistableSuggestions() {
    return true;
  }
}
