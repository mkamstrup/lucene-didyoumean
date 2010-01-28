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


import org.apache.lucene.search.didyoumean.SecondLevelSuggester;
import org.apache.lucene.search.didyoumean.SuggestionPriorityQueue;

import java.io.IOException;

/**
 * Wraps a {@link TokenPhraseSuggester} as a {@link SecondLevelSuggester}.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>, Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since 2007-feb-17
 */
public class SecondLevelTokenPhraseSuggester implements SecondLevelSuggester {

  private TokenPhraseSuggester phraseSuggester;

  public SecondLevelTokenPhraseSuggester(TokenPhraseSuggester phraseSuggester) throws IOException {
    this.phraseSuggester = phraseSuggester;
  }

  public SuggestionPriorityQueue suggest(String query) {
    return phraseSuggester.suggest(query, 1);
  }


  public boolean hasPersistableSuggestions() {
    return true;
  }
}
