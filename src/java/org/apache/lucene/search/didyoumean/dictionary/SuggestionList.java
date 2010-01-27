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

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.didyoumean.Suggestion;

/**
 * A list of suggetions to a miss spelled word, which may optionally be ordered
 * by suggestion score.
 * <p/>
 * The suggestion list does not know about the original user query, only the
 * normalized form of the query, known as the <i>query key</i>, obtained from
 * calling {@link Dictionary#keyFormatter(String)} on the original user query.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>, Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since 2007-feb-02
 */
public class SuggestionList implements Iterable<Suggestion>, Serializable {

  // private static Log log = LogFactory.getLog(SuggestionsList.class);
  private static long serialVersionUID = 1l;

  //@PrimaryKey
  private String query;
  private List<Suggestion> suggestions = new LinkedList<Suggestion>();

  /**
   * Create a new suggestion list for the query key {@code queryKey}.
   * The query key is normally generated from a user query by calling
   * {@link Dictionary#keyFormatter(String)}
   * @param queryKey the query key to use for this suggestion list
   */
  SuggestionList(String queryKey) {
    this.query = queryKey;
  }

  /**
   * Get the suggestin at offset {@code index} in this list
   * @param index the offset at which to get the {@link Suggestion} instance
   * @return the {@code Suggestion} at offset {@code index}
   */
  public Suggestion get(int index) {
    return suggestions.get(index);
  }

  /**
   * Get the first {@link Suggestion} having a suggestion string matching {@code suggested}
   * @param suggested the suggestion string to look for
   * @return a {@link Suggestion} with {@code s.getSuggested() == suggested}
   *         or {@code null} in case no such suggestion was found
   */
  public Suggestion get(String suggested) {
    for (Suggestion suggestion : suggestions) {
      if (suggestion.getSuggested().equals(suggested)) {
        return suggestion;
      }
    }
    return null;
  }

  /**
   * Return an iterator over all {@link Suggestion}s in this list
   * @return an iterator over all {@link Suggestion}s in this list
   */
  public Iterator<Suggestion> iterator() {
    return suggestions.iterator();
  }

  /**
   * The number of suggestions in this list
   * @return the number of suggestions in this list
   */
  public int size() {
    return suggestions.size();
  }

  /**
   * Returns {@code true} if and only if this list contains a {@link Suggestion}
   * instance where {@code suggestion.getSuggested() == suggested}.
   * @param suggested the suggestion string to look for
   * @return see above
   */
  public boolean containsSuggested(String suggested) {
    return get(suggested) != null;
  }

  /**
   * Add a new {@link Suggestion} instance based on the given parameters
   * @param suggested the suggested correction to the query key
   * @param score the quality measure of the proposed correction
   * @param corpusQueryResults the number of hits in the main corpus for the query {@code suggested}
   */
  public void addSuggested(String suggested, double score, Integer corpusQueryResults) {
    if (containsSuggested(suggested)) {
      throw new IllegalArgumentException("Already contains suggested '" + suggested + "'");
    }
    Suggestion suggestion = new Suggestion(suggested, score, corpusQueryResults);
    int index = Collections.binarySearch(suggestions, suggestion);
    if (index < 0) {
      index = (index * -1) - 1;
    }
    suggestions.add(index, suggestion);
  }

  /**
   * Simple interface for filtering {@link SuggestionList}s using
   * {@link #filter(Filter)} or {@link #filterTo(List,Filter)}
   */
  public static interface Filter {
    public abstract boolean accept(Suggestion suggestion);
  }

  /**
   * Create a new list of suggestions containing only those where
   * {@code filter.accepts(suggestion)} returns {@code true}
   * @param filter the filter to apply to {@code this}
   * @return a newly allocated list containing references to the
   *         accepted suggestions
   */
  public List<Suggestion> filter(Filter filter) {
    List<Suggestion> list = new LinkedList<Suggestion>();
    filterTo(list, filter);
    return list;
  }

  /**
   * Add all suggestions from {@code this} where {@code filter.accepts(suggestion)}
   * returns {@code true} to {@code list}
   * @param list the list to add accepted suggestions to
   * @param filter the filter to apply to {@code this}
   */
  public void filterTo(List<Suggestion> list, Filter filter) {
    for (Suggestion s : this.suggestions) {
      if (filter.accept(s)) {
        list.add(s);
      }
    }
  }

  /**
   * Return all suggestions from this list in an array. If {@code arr.length >= this.size()}
   * then {@code arr} will be reused and if there is any spare room in {@code arr}
   * then the entry immediately following the last suggestion will be set to
   * {@code null}, ie. {@code arr[this.size()] = null}.
   * @param arr the array to use if there is room
   * @return {@code arr} or a new array as described above
   */
  public Suggestion[] toArray(Suggestion[] arr) {
    return suggestions.toArray(arr);
  }

  /**
   * Sort all suggestions in this list
   */
  public void sort() {
    Collections.sort(suggestions);
  }

  /**
   * Get the query key this list contains suggestions for
   * @return the query key, ie. <i>not</i> the original user query, but the normalized form
   */
  public String getQueryKey() {
    return query;
  }

  /**
   * Get the raw {@code List} backing this suggestion list
   * @return
   */
  List<Suggestion> getSuggestions() {
    return suggestions;
  }
}
