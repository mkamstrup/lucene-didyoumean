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

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Essentially a dictionary with a bunch of weighted suggestions, created by a
 * {@link org.apache.lucene.search.didyoumean.session.Trainer}
 * and navigated by a {@link org.apache.lucene.search.didyoumean.Suggester}.
 * <p/>
 * If the dictionary does not contain a suggestion for a given query, it will be passed on to any available instance
 * of {@link org.apache.lucene.search.didyoumean.SecondLevelSuggester} that hopefully will comes up with a suggestion.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>, Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since 2010-01-26
 */
public abstract class Dictionary implements Iterable<SuggestionList> {

  protected Map<SecondLevelSuggester, Double> prioritiesBySecondLevelSuggester = new HashMap<SecondLevelSuggester, Double>();

  /**
   * Create a new {@link SuggestionList} for {@code query}. The query will automatically be
   * converted to a query key internally by calling {@link #keyFormatter}.
   * @param query the raw suer query to create a suggestion list for
   * @return a newly allocated, empty, suggestion list 
   */
  public SuggestionList suggestionListFactory(String query){
    return new SuggestionList(keyFormatter(query));
  }

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
  public String keyFormatter(String inputKey) {
    return inputKey.replaceAll("\\p{Punct}", "").replaceAll("\\s", "").toLowerCase();
  }

  /**
   * This is used by the second level cache so it only comes up with suggestions that don't exist in the dictionary.
   *
   * @param query     the original query from the user
   * @param suggested the suggestion to the query to inspect if suggestable
   * @return true if the suggested is a known suggetion to the query.
   */
  public boolean isExistingSuggestion(String query, String suggested) throws QueryException {
    SuggestionList suggestions = getSuggestions(query);
    return suggestions != null && suggestions.containsSuggested(suggested);
  }

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
  public Suggestion[] getSecondLevelSuggestion(String query, int n) throws QueryException {
    Map<String, Suggestion> suggestionsBySuggested = new HashMap<String, Suggestion>();
    for (Map.Entry<SecondLevelSuggester, Double> suggester_boost : prioritiesBySecondLevelSuggester.entrySet()) {
      SuggestionPriorityQueue suggestions = suggester_boost.getKey().suggest(query);
      for (int i = 0; i < n && suggestions.size() > i; i++) {
        Suggestion suggestion = (Suggestion) suggestions.pop();
        if (suggester_boost.getKey().hasPersistableSuggestions()) {
          Suggestion internedSuggestion = suggestionsBySuggested.get(suggestion.getSuggested());
          if (internedSuggestion == null) {
            internedSuggestion = new Suggestion(suggestion.getSuggested(), 0d);
          }
          internedSuggestion.setScore(internedSuggestion.getScore() + suggester_boost.getValue());
          suggestionsBySuggested.put(suggestion.getSuggested(), internedSuggestion);
        }
      }
    }

    if (suggestionsBySuggested.size() == 0) {
      return null;
    }

    Suggestion[] suggestions = suggestionsBySuggested.values().toArray(new Suggestion[suggestionsBySuggested.size()]);
    Arrays.sort(suggestions, new Comparator<Suggestion>() {
      public int compare(Suggestion suggestion, Suggestion suggestion1) {
        return Double.compare(suggestion.getScore(), suggestion1.getScore());
      }
    });

    // add to dictionary
    SuggestionList suggestionList = suggestionListFactory(query);
    suggestionList.addSuggested(suggestions[0].getSuggested(), 1d, suggestions[0].getCorpusQueryResults());
    put(suggestionList);
    return suggestions;
  }

  /**
   * Used to extract bootstrapped a priori corpus from the dictionary
   * @return a map where suggestion is key and the value is a list of misspelled words that suggests the key.
   * @throws QueryException
   * @see org.apache.lucene.search.didyoumean.SuggestionFacade#secondLevelSuggestionFactory()
   */
  public Map<String, SuggestionList> inverted() throws QueryException {

    // todo use a temporary bdb for this so we dont run out of memory

    Map<String, SuggestionList> inverted = new HashMap<String, SuggestionList>();

    for (SuggestionList suggestions : this) {
      Suggestion s = suggestions.get(0);

      SuggestionList sl = inverted.get(s.getSuggested());
      if (sl == null) {
        sl = new SuggestionList(s.getSuggested());
        inverted.put(s.getSuggested(), sl);
      }
      sl.addSuggested(sl.getQuery(), s.getScore(), s.getCorpusQueryResults());
    }

    return inverted;
  }

  public Map<SecondLevelSuggester, Double> getPrioritiesBySecondLevelSuggester() {
    return prioritiesBySecondLevelSuggester;
  }

  public void setPrioritiesBySecondLevelSuggester(Map<SecondLevelSuggester, Double> prioritiesBySecondLevelSuggester) {
    this.prioritiesBySecondLevelSuggester = prioritiesBySecondLevelSuggester;
  }

  /**
   * Get a list of suggestions for the user provided query {@code query}.
   * <p/>
   * Implementation note: The implementing class should pass {@code query} through {@link #keyFormatter}
   * in order to have it match with the query keys generated for {@link SuggestionList}s
   * @param query unformatted key
   * @return suggestion list associated with key. If no suggestions are found an empty list will be returned
   */
  public abstract SuggestionList getSuggestions(String query) throws QueryException;

  /**
   * Free all resources allocated by the dictionary. Any subsequent access to the dictionary will
   * throw an {@link IOException}.
   * @throws IOException if there is an error closing down the dictionary
   */
  public abstract void close() throws IOException;

  /**
   * Store a suggestion list (by query key) in the dictionary
   * @param suggestions the suggestion list to store
   */
  public abstract void put(SuggestionList suggestions);

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
   *
   * @param maxSize the maximum number of suggestions for a given query
   */
  public abstract void prune(int maxSize) throws IOException;

  /**
   * Returns the number of unique query keys stored in the dictionary
   */
  public abstract int size();

}
