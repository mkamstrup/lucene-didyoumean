package org.apache.lucene.search.didyoumean.impl;

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

import org.apache.lucene.search.didyoumean.AbstractSuggester;
import org.apache.lucene.search.didyoumean.EditDistance;
import org.apache.lucene.search.didyoumean.Levenshtein;
import org.apache.lucene.search.didyoumean.Suggestion;
import org.apache.lucene.search.didyoumean.dictionary.Dictionary;
import org.apache.lucene.search.didyoumean.dictionary.QueryException;
import org.apache.lucene.search.didyoumean.dictionary.SuggestionList;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Returns highest scoring suggestion available,
 * unless the score is lower than the suggestion supression threadshold.
 * <p/>
 * If there are no suggestions available, the second level suggesters
 * registred to the dictionary are used to produce the suggestions.
 * <p/>
 * If the top scoring suggestion is same as the query,
 * and the second best is not supressed below threadshold,
 * change order
 * <p/>
 * Ignoring a suggestion 50 times or so with a DefaultTrainer makes a score hit 0.05d.
 * <p/>
 * It will navigate towards better suggestions this way:
 * <pre>
 *      Suggestion[] currentSuggestions = toQuerySensitiveArray(suggestions, n, query);
 *      Suggestion[] topSuggestionSuggestions = toQuerySensitiveArray(gatherSuggestionList(dictionary, currentSuggestions[0].getSuggested(), n), n, currentSuggestions[0].getSuggested());
 *      // if the input query results in bad results according to some strategy,
 *      // then attempt to navigate towards a top suggestion that does result in something good,
 *      while (
 *          topSuggestionSuggestions != null && topSuggestionSuggestions.length > 0
 *              && (currentSuggestions[0].getCorpusQueryResults().size() == 0
 *              || currentSuggestions[0].getCorpusQueryResults().size() * 3 < topSuggestionSuggestions[0].getCorpusQueryResults().size())) {
 *        currentSuggestions = topSuggestionSuggestions;
 *        topSuggestionSuggestions = toQuerySensitiveArray(gatherSuggestionList(dictionary, currentSuggestions[0].getSuggested(), n), n, currentSuggestions[0].getSuggested());
 *      }
 * </pre>
 * todo: above should be an aggregated strategy!
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: Jul 31, 2006
 *         Time: 4:56:34 PM
 */
public class DefaultSuggester extends AbstractSuggester implements Serializable {

  private static final long serialVersionUID = 1l;


  public DefaultSuggester() {
  }

  /**
   * ignoring a suggestion 50 times or so makes hit 0.05d.
   */
  private double suggestionSupressionThreadshold = 0.05d;

  private SuggestionList gatherSuggestionList(Dictionary dictionary, String query, int n) throws QueryException {
    SuggestionList suggestions = dictionary.getSuggestions(query);
    if (suggestions != null && suggestions.size() > 0) {
      // if top suggestion is suppressed,
      // then try to get some more suggestions from second level
      if (suggestions.get(0).getScore() < getSuggestionSupressionThreadshold()) {
        Suggestion[] secondLevel = dictionary.getSecondLevelSuggestion(query, n);
        if (secondLevel != null && secondLevel.length > 0) {
          // then merge the not yet seen suggestions with the dictionary suggestions.
          // this way we don't suggest something that has been suppressed.
          double score = 1d;
          for (Suggestion suggestion : secondLevel) {
            score -= 0.01d;
            if (!suggestions.containsSuggested(suggestion.getSuggested())) {
              suggestions.addSuggested(suggestion.getSuggested(), score, suggestion.getCorpusQueryResults());
              dictionary.put(suggestions);
            }
          }
        }
      }
    }
    return suggestions;
  }

  public Suggestion[] didYouMean(Dictionary dictionary, String query, int n) throws QueryException {
    SuggestionList suggestions = gatherSuggestionList(dictionary, query, n);
    if (suggestions != null) {
      if (suggestions.size() > 0) {
        Suggestion[] originalSuggestions = toQuerySensitiveArray(suggestions, n, query);
        Suggestion[] currentSuggestions = originalSuggestions;
        Suggestion[] topSuggestionSuggestions = toQuerySensitiveArray(gatherSuggestionList(dictionary, currentSuggestions[0].getSuggested(), n), n, currentSuggestions[0].getSuggested());
        // if the input query results in bad results according to some strategy,
        // then attempt to navigate towards a top suggestion that does result in something good,
        int noEternalLoops = 0;
        while (hasBetterNestedSuggestion(topSuggestionSuggestions, currentSuggestions)) {
          // no eternal loops!
          if (++noEternalLoops == 100) {
            System.out.println(query + " points back at it self or is too complex!");
            return originalSuggestions;
          }
          currentSuggestions = topSuggestionSuggestions;
          topSuggestionSuggestions = toQuerySensitiveArray(gatherSuggestionList(dictionary, currentSuggestions[0].getSuggested(), n), n, currentSuggestions[0].getSuggested());
        }
        return currentSuggestions;
      } else {
        return dictionary.getSecondLevelSuggestion(query, n);
      }

    } else {
      return dictionary.getSecondLevelSuggestion(query, n);
    }
  }

  /**
   * If topSuggestionSuggestions points at something
   * that has three times as many results as the top current suggestion
   * then this method returns true.
   *
   * if the current suggestion has no hits
   * and there is a suggestion with hits in topSuggestionSuggestions
   * this method returns true
   *
   * @param topSuggestionSuggestions suggestions to first suggestion in currentSuggestions
   * @param currentSuggestions suggestions to the current query
   * @return true if topSuggestionSuggestions has a better suggestion than currentSuggestions
   */
  public boolean hasBetterNestedSuggestion(Suggestion[] topSuggestionSuggestions, Suggestion[] currentSuggestions) {
    return topSuggestionSuggestions != null
        && topSuggestionSuggestions.length > 0
        && currentSuggestions[0].getCorpusQueryResults() != null
        &&
        ((currentSuggestions[0].getCorpusQueryResults() == 0 && topSuggestionSuggestions[0].getCorpusQueryResults() > 0)
            || (currentSuggestions[0].getCorpusQueryResults() * 3 < topSuggestionSuggestions[0].getCorpusQueryResults())
        );
  }


  public EditDistance editDistanceFactory(String sd) {
    return new Levenshtein(sd);
  }


  private Suggestion[] toQuerySensitiveArray(SuggestionList suggestions, int n, String query) {
    // build an array that is max n in size.

    LinkedList<Suggestion> ret = new LinkedList<Suggestion>();
    suggestions.filterTo(ret, new SuggestionList.Filter() {

      public boolean accept(Suggestion suggestion) {
        return suggestion.getScore() > getSuggestionSupressionThreadshold();
      }
    });


    if (ret.size() > 1) {

      // if the top scoring suggestion is same as the query
      if (query.equals(ret.get(0).getSuggested())) {

        // switch top two suggestions
        Suggestion s = ret.get(0);
        ret.set(0, ret.get(1));
        ret.set(1, s);

        // if any other suggestion has many more hits
        // then set that as top suggestion.

        EditDistance ed = editDistanceFactory(ret.get(0).getSuggested());
        List<Suggestion> topHits = new LinkedList<Suggestion>();
        if (ret.size() > 2) {
          for (int i = 2; i < ret.size(); i++) {
            if (ret.get(i).getCorpusQueryResults() > ret.get(0).getCorpusQueryResults()
                && ed.getNormalizedDistance(ret.get(i).getSuggested()) > 0.8d /* todo setting */) {
              topHits.add(ret.get(i));
            }
          }
        }
        if (topHits.size() > 0) {

          if (topHits.size() > 1) {
            Collections.sort(topHits, new Comparator<Suggestion>() {
              public int compare(Suggestion suggestion, Suggestion suggestion1) {
                return new Integer(suggestion.getCorpusQueryResults()).compareTo(suggestion1.getCorpusQueryResults());
              }
            });
          }
          ret.remove(topHits.get(0));
          ret.addFirst(topHits.get(0));
        }
      }
//      else {
//        // order by number of hits
//        Arrays.sort(ret, new Comparator<Suggestion>() {
//          public int compare(Suggestion suggestion, Suggestion suggestion1) {
//            return new Integer(suggestion1.getCorpusQueryResults().size()).compareTo(suggestion.getCorpusQueryResults().size());
//          }
//        });
//      }
    }
    return ret.toArray(new Suggestion[n < ret.size() ? n : ret.size()]);
  }


  public double getSuggestionSupressionThreadshold
      () {
    return suggestionSupressionThreadshold;
  }

  public void setSuggestionSupressionThreadshold
      (
          double suggestionSupressionThreadshold) {
    this.suggestionSupressionThreadshold = suggestionSupressionThreadshold;
  }
}
