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


import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import org.apache.lucene.search.didyoumean.SecondLevelSuggester;
import org.apache.lucene.search.didyoumean.Suggester;
import org.apache.lucene.search.didyoumean.Suggestion;
import org.apache.lucene.search.didyoumean.SuggestionPriorityQueue;

import java.io.File;
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
public class Dictionary {

  private EntityStore store;
  private PrimaryIndex<String, SuggestionList> suggestionsByQuery;


  public Dictionary(File bdbPath) throws DatabaseException {
    if (!bdbPath.exists()) {
      System.out.println("Creating path " + bdbPath);
      bdbPath.mkdirs();
    }

    EnvironmentConfig envConfig = new EnvironmentConfig();
    envConfig.setAllowCreate(true);
    envConfig.setTransactional(false);
    envConfig.setLocking(false);
    Environment env = new Environment(bdbPath, envConfig);

    StoreConfig storeConfig = new StoreConfig();
    storeConfig.setAllowCreate(true);
    storeConfig.setTransactional(false);
    store = new EntityStore(env, "didYouMean", storeConfig);

    suggestionsByQuery = store.getPrimaryIndex(String.class, SuggestionList.class);
  }

  public SuggestionList suggestionListFactory(String query) {
    return new SuggestionList(keyFormatter(query));
  }

  public void close() throws DatabaseException {
    store.close();
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
   * Implementation must pass query through keyformatter!
   * @param query unformatted key
   * @return suggestion list associated with key
   */
  public SuggestionList getSuggestions(String query) throws DatabaseException {
    return suggestionsByQuery.get(keyFormatter(query));
  }

  /**
   * This is used by the second level cache so it only comes up with suggestions that don't exist in the dictionary.
   *
   * @param query     the original query from the user
   * @param suggested the suggestion to the query to inspect if suggestable
   * @return true if the suggested is a known suggetion to the query.
   */
  public boolean isExistingSuggestion(String query, String suggested) throws DatabaseException {
    SuggestionList suggestions = getSuggestions(query);
    return suggestions != null && suggestions.containsSuggested(suggested);
  }


  private Map<SecondLevelSuggester, Double> prioritesBySecondLevelSuggester = new HashMap<SecondLevelSuggester, Double>();

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
  public Suggestion[] getSecondLevelSuggestion(String query, int n) throws DatabaseException {
    Map<String, Suggestion> suggestionsBySuggested = new HashMap<String, Suggestion>();
    for (Map.Entry<SecondLevelSuggester, Double> suggester_boost : prioritesBySecondLevelSuggester.entrySet()) {
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
    suggestionsByQuery.put(suggestionList);
    return suggestions;
  }


  public Map<SecondLevelSuggester, Double> getPrioritesBySecondLevelSuggester() {
    return prioritesBySecondLevelSuggester;
  }

  public void setPrioritesBySecondLevelSuggester(Map<SecondLevelSuggester, Double> prioritesBySecondLevelSuggester) {
    this.prioritesBySecondLevelSuggester = prioritesBySecondLevelSuggester;
  }

  /**
   * Used to extract bootstrapped a priori corpus from the dictionary
   * @return a map where suggestion is key and the value is a list of misspelled words that suggests the key.
   * @throws DatabaseException
   * @see org.apache.lucene.search.didyoumean.SuggestionFacade#secondLevelSuggestionFactory(org.apache.lucene.index.facade.IndexFacade,org.apache.lucene.index.facade.IndexFacade)
   */
  public Map<String, SuggestionList> inverted() throws DatabaseException {

    // todo use a temporary bdb for this so we dont run out of memory

    Map<String, SuggestionList> inverted = new HashMap<String, SuggestionList>();

    for (Map.Entry<String, SuggestionList> e : getSuggestionsByQuery().map().entrySet()) {
      Suggestion s = e.getValue().get(0);
      SuggestionList sl = inverted.get(s.getSuggested());
      if (sl == null) {
        sl = new SuggestionList(s.getSuggested());
        inverted.put(s.getSuggested(), sl);
      }
      sl.addSuggested(e.getKey(), s.getScore(), s.getCorpusQueryResults());
    }


    return inverted;
  }


  public PrimaryIndex<String, SuggestionList> getSuggestionsByQuery() {
    return suggestionsByQuery;
  }


  /**
   * Scans the dictionary for queries that suggests a query
   * that in their own turn suggest something else.
   * These bad suggestions will be replaced by the final suggestion,
   * so that the suggester don't have to spend clock ticks doing this in real time. 
   *
   * @param suggester
   */
  public void optimize(Suggester suggester) throws DatabaseException {
    // todo
  }

  /**
   * Removes excess suggestions that probably never be suggested,
   * for instance those with too great distance from top suggestion.
   */
  public void prune(int maxSize) throws DatabaseException {
    EntityCursor<SuggestionList> cursor = getSuggestionsByQuery().entities();
    SuggestionList suggestionList;
    while ((suggestionList = cursor.next()) != null) {
      if (suggestionList.size() > maxSize) {
        for (int i = maxSize; i < suggestionList.size(); i++) {
          suggestionList.getSuggestions().remove(i);
        }
        getSuggestionsByQuery().put(suggestionList);
      }
    }
  }

}
