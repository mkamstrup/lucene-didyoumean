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
 * A suggester is the entity that navigates a dictionary in search for a suggestion to a query.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: Jul 31, 2006
 *         Time: 4:46:57 PM
 */
public interface Suggester {

  /**
   * @param dictionary
   * @param query
   * @param n max number of suggestions @return
   */
  public abstract Suggestion[] didYouMean(Dictionary dictionary, String query, int n) throws DatabaseException;

  public abstract String didYouMean(Dictionary dictionary, String query) throws DatabaseException ;


}
