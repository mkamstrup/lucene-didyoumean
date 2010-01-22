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


import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.didyoumean.SuggestionPriorityQueue;

import java.io.IOException;


/**
 * A suggester that can only handle single token words.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-jan-30
 *         Time: 06:05:54
 */
public interface TokenSuggester {
  public abstract SuggestionPriorityQueue suggest(String queryToken, int n, boolean suggestSelf, IndexReader aprioriIndexReader, String aprioriIndexField, boolean selectMorePopularTokensOnly) throws IOException;
}
