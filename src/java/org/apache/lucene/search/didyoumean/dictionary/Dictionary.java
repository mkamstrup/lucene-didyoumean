package org.apache.lucene.search.didyoumean.dictionary;

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
import org.apache.lucene.search.didyoumean.Suggester;
import org.apache.lucene.search.didyoumean.Suggestion;
import org.apache.lucene.search.didyoumean.SuggestionPriorityQueue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Essentially a dictionary with a bunch of weighted suggestions, created by a {@link org.apache.lucene.search.didyoumean.Trainer}
 * and navigated by a {@link org.apache.lucene.search.didyoumean.Suggester}.
 * <p/>
 * If the dictionary does not contain a suggestion for a given query, it will be passed on to any available instance
 * of {@link org.apache.lucene.search.didyoumean.SecondLevelSuggester} that hopefully will comes up with a suggestion.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: Jul 31, 2006
 *         Time: 2:10:19 PM
 */
public abstract class Dictionary {

  public static SuggestionList suggestionListFactory(String query){
    return null;
  }

  public abstract void close() throws IOException;

  /**
   * This function allows modification of the dictionary keys. Default implementation stripps them from all
   * whitespace and punctuation, enabling features such as "allwork and no fun" suggesting "all work and no fun"
   * That will of course require that the trainer have inserted the key to suggest to it's original self. Training
   * everything the users type in might consume a lot of resources.
   * {@link org.apache.lucene.search.didyoumean.impl.DefaultTrainer#setTrainingFinalGoalAsSelf(boolean)} ()}
   *
   * @param inputKey dictionary key to be formatted
   * @return inputKey stripped from all whitespace and punctuation.
   */
  public abstract String keyFormatter(String inputKey);

  /**
   * Implementation must pass query through keyformatter!
   * @param query unformatted key
   * @return suggestion list associated with key
   */
  public abstract SuggestionList getSuggestions(String query) throws QueryException;

  /**
   * This is used by the second level cache so it only comes up with suggestions that don't exist in the dictionary.
   *
   * @param query     the original query from the user
   * @param suggested the suggestion to the query to inspect if suggestable
   * @return true if the suggested is a known suggetion to the query.
   */
  public abstract boolean isExistingSuggestion(String query, String suggested) throws QueryException;

  /**
   * Comes up with the best suggestion from the second level suggesters.
   * {@link org.apache.lucene.search.didyoumean.SecondLevelSuggester}
   * <p/>
   * This metod also adds the suggestion to the dictionary!
   *
   * @param query the user input
   * @param n     number of suggestions requested
   * @return the best suggestion the second level suggesters could come up with
   */
  public abstract Suggestion[] getSecondLevelSuggestion(String query, int n) throws QueryException;

  public abstract Map<SecondLevelSuggester, Double> getPrioritesBySecondLevelSuggester();

  public abstract void setPrioritesBySecondLevelSuggester(Map<SecondLevelSuggester, Double> prioritesBySecondLevelSuggester);

  /**
   * Used to extract bootstrapped a priori corpus from the dictionary
   * @return a map where suggestion is key and the value is a list of misspelled words that suggests the key.
   * @throws QueryException
   * @see org.apache.lucene.search.didyoumean.SuggestionFacade#secondLevelSuggestionFactory()
   */
  public abstract Map<String, SuggestionList> inverted() throws QueryException;

  public abstract void put(String suggestion, SuggestionList suggestions);

  /**
   * Scans the dictionary for queries that suggests a query
   * that in their own turn suggest something else.
   * These bad suggestions will be replaced by the final suggestion,
   * so that the suggester don't have to spend clock ticks doing this in real time. 
   *
   * @param suggester
   */
  public abstract void optimize(Suggester suggester) throws IOException;

  /**
   * Removes excess suggestions that probably never be suggested,
   * for instance those with too great distance from top suggestion.
   */
  public abstract void prune(int maxSize) throws IOException;

  /**
   *
   */
  public abstract int size();

}
