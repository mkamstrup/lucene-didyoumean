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
public class BDBDictionary {

  private EntityStore store;
  private PrimaryIndex<String, SuggestionList> suggestionsByQuery;


  public BDBDictionary(File bdbPath) throws IOException {
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
   * Implementation must pass query through keyformatter!
   * @param query unformatted key
   * @return suggestion list associated with key
   */
  public SuggestionList getSuggestions(String query) throws DatabaseException {
    return suggestionsByQuery.get(keyFormatter(query));
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
