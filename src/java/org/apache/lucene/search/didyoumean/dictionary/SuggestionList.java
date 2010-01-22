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


import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.didyoumean.Suggestion;

/**
 * A list of suggetions to a miss spelled word, ordered by suggestion score.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-feb-02
 *         Time: 06:28:23
 *         <p/>
 */
@Entity
public class SuggestionList implements Iterable<Suggestion>, Serializable {

  // private static Log log = LogFactory.getLog(SuggestionsList.class);
  private static long serialVersionUID = 1l;

  @PrimaryKey
  private String query;
  private List<Suggestion> suggestions = new LinkedList<Suggestion>();

  /** bdb persistency */
  private SuggestionList(){}

  SuggestionList(String query) {
    this.query = query;
  }

  public Suggestion get(int index) {
    return suggestions.get(index);
  }

  public Suggestion get(String suggested) {
    for (Suggestion suggestion : suggestions) {
      if (suggestion.getSuggested().equals(suggested)) {
        return suggestion;
      }
    }
    return null;
  }

  public Iterator<Suggestion> iterator() {
    return suggestions.iterator();
  }

  public int size() {
    return suggestions.size();
  }

  public boolean containsSuggested(String suggested) {
    for (Suggestion suggestion : suggestions) {
      if (suggested.equals(suggestion.getSuggested())) {
        return true;
      }
    }
    return false;
  }

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

  public static interface SuggestionFilter {
    public abstract boolean accept(Suggestion suggestion);
  }

  public Suggestion[] toArray(Suggestion[] arr) {
    return suggestions.toArray(arr);
  }

  public List<Suggestion> filter(SuggestionFilter filter) {
    List<Suggestion> list = new LinkedList<Suggestion>();
    filter(list, filter);
    return list;
  }

  public void filter(List<Suggestion> list, SuggestionFilter filter) {
    for (Suggestion s : this.suggestions) {
      if (filter.accept(s)) {
        list.add(s);
      }
    }
  }

  public void sort() {
    Collections.sort(suggestions);
  }

  public String getQuery() {
    return query;
  }


  List<Suggestion> getSuggestions() {
    return suggestions;
  }
}
