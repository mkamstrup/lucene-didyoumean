package org.apache.lucene.search.didyoumean;

import com.sleepycat.je.DatabaseException;
import org.apache.lucene.search.didyoumean.dictionary.Dictionary;
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
 * Abstract suggester.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-feb-17
 *         Time: 02:10:42
 */
public abstract class AbstractSuggester implements Suggester {

  public String didYouMean(Dictionary dictionary, String query) throws DatabaseException {
    Suggestion[] suggestions = didYouMean(dictionary, query, 1);
    if (suggestions == null || suggestions.length == 0) {
      return null;
    } else {
      return suggestions[0].getSuggested();
    }
  }


}
