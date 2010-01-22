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


/**
 * When the dictionary is not aware of any suggestion the second level
 * suggesters are called upon to find the best suggestions and insert
 * them to the dictionary.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-jan-30
 *         Time: 06:05:54
 */
public interface SecondLevelSuggester {
  public abstract SuggestionPriorityQueue suggest(String query);

  /**
   * @return true if suggestions from this suggester should be persisted in the dictionary
   */
  public abstract boolean hasPersistableSuggestions();
}
