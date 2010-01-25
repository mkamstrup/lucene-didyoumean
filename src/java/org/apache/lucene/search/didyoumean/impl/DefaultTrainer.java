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


import org.apache.lucene.search.didyoumean.EditDistance;
import org.apache.lucene.search.didyoumean.Levenshtein;
import org.apache.lucene.search.didyoumean.Suggestion;
import org.apache.lucene.search.didyoumean.dictionary.Dictionary;
import org.apache.lucene.search.didyoumean.dictionary.QueryException;
import org.apache.lucene.search.didyoumean.dictionary.SuggestionList;
import org.apache.lucene.search.didyoumean.session.QueryGoalNode;
import org.apache.lucene.search.didyoumean.session.Trainer;

import java.util.*;

/**
 * A simple adapting suggestion strategy that updates the content of a dictionary based on what a query session looked like.
 * <p/>
 * If a user accepts the suggestion made by the system, then we increase the score for that suggestion. (positive adaptation)
 * If a user does not accept the suggestion made by the system, then we decrease the score for that suggestion. (negative adaptation)
 * <p/>
 * The the goal tree is a single query, one query only (perhaps with multiple inspections)
 * then we adapt negative once again.
 * <p/>
 * Suggestions are the query with inspections, ordered by the classification weight.
 * All the queries in the goal witout inspections will be adpated positive with
 * the query with inspections that has the shortest edit distance.
 * <p/>
 * Suggest back from best goal to second best goal. homm -> heroes of might and magic -> homm
 * <p/>
 * In case of no inspections in a goal tree, a {@link org.apache.lucene.search.didyoumean.impl.QueryGoalJuror} is used to judge what is the goal.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: Jul 31, 2006
 *         Time: 4:56:13 PM
 */
public class DefaultTrainer<R> implements Trainer<R> {

  private boolean trainingFinalGoalAsSelf = true;

  private double notSuggestedPositiveAdaptationFactor = 1.4d;
  private double acceptedSuggestionPositiveAdaptationFactor = 1.4d;
  private double ignoredSuggestionNegativeAdaptationFactor = 0.9d;

  private QueryGoalJuror<R> juror = new DefaultQueryGoalJuror<R>();

  private static Comparator<Suggestion> scoreComparator = new Comparator<Suggestion>() {
    public int compare(Suggestion o1, Suggestion o2) {
      return Double.compare(o2.getScore(), o1.getScore());
    }
  };


  public void trainGoalTree(Dictionary dictionary, QueryGoalNode<R> goalTreeRoot) throws QueryException {

    int numChildrenRecursive = 0;
    // positive and negative adaptation of suggestion scores 
    for (Iterator<QueryGoalNode<R>> it = goalTreeRoot.iterateChildrenRecursive(); it.hasNext();) {
      numChildrenRecursive++;
      QueryGoalNode<R> node = it.next();
      if (node.getParent().getSuggestion() != null) {

        SuggestionList suggestions = dictionary.getSuggestions(dictionary.keyFormatter(node.getParent().getQuery()));
        Suggestion suggestion = suggestions.get(node.getParent().getSuggestion());

        if (node.getQuery().equals(node.getParent().getSuggestion())) {
          // user took our suggestion, increase the score of that suggestion. 
          suggestion.setScore(suggestion.getScore() * getAcceptedSuggestionPositiveAdaptationFactor());
        } else {
          // user did not take our suggestion, decrease the score of that suggestion.
          suggestion.setScore(suggestion.getScore() * getIgnoredSuggestionNegativeAdaptationFactor());
        }

        suggestions.sort();
        dictionary.put(suggestions);
      }
    }

    if (numChildrenRecursive == 0) {

      // a single query

      boolean train = false;
      for (QueryGoalNode.Inspection inspection : goalTreeRoot.getInspections()) {
        if (inspection.getGoalClassification() > QueryGoalNode.MOO) {
          train = true;
          break;
        }
      }
      if (train) {
        adaptPositive(dictionary, goalTreeRoot.getQuery(), goalTreeRoot.getcorpusQueryResults(), goalTreeRoot);
      }
//
//      // if inspected, negative train on top suggestion,
//      // but only if available and not the same as the query
//      SuggestionList suggestions = dictionary.getSuggestions(goalTreeRoot.getQuery());
//      if (goalTreeRoot.getInspections().size() > 0 && suggestions != null && suggestions.size() > 0 && !suggestions.get(0).getSuggested().equals(goalTreeRoot.getQuery())) {
//        suggestions.get(0).setPopularity(suggestions.get(0).getPopularity() * getIgnoredSuggestionNegativeAdaptationFactor());
//        suggestions.sort();
//      }
    } else {

      /** suggestions are the nodes with inspections, ordered by the classification weight. */
      List<QueryGoalNode<R>> nodesWithGoals = new LinkedList<QueryGoalNode<R>>();

      /** all the other nodes, them without inspections, will suggest the suggestions. */
      List<QueryGoalNode<R>> nodesWithoutGoals = new LinkedList<QueryGoalNode<R>>();

      if (goalTreeRoot.getInspections().size() > 0) {
        nodesWithGoals.add(goalTreeRoot);
      } else {
        nodesWithoutGoals.add(goalTreeRoot);
      }
      for (Iterator<QueryGoalNode<R>> it = goalTreeRoot.iterateChildrenRecursive(); it.hasNext();) {
        QueryGoalNode<R> node = it.next();
        if (node.getInspections().size() > 0) {
          for (QueryGoalNode.Inspection inspection : node.getInspections()) {
            if (inspection.getGoalClassification() > QueryGoalNode.MOO) {
              nodesWithGoals.add(node);
              break;
            }
          }
        }

        if (!nodesWithGoals.contains(node)) {
          nodesWithoutGoals.add(node);
        }
      }

      if (nodesWithGoals.size() == 0) {
        // there was  no inspections.
        // we have no clue what the users was looking for. not sure if this mean something extra.

        // call juror strategy
        nodesWithGoals = getJuror().createGoals(goalTreeRoot);
        nodesWithoutGoals.removeAll(nodesWithGoals);
        if (nodesWithGoals.size() == 0) {
          return;
        }
      }

      {
        // order by inspection weight and time - most recent query with top importance.
        Collections.sort(nodesWithGoals, new Comparator<QueryGoalNode<R>>() {
          public int compare(QueryGoalNode<R> queryGoalNode, QueryGoalNode<R> queryGoalNode1) {
            int ret = Double.compare(queryGoalNode1.calculateInspectionWeight(), queryGoalNode.calculateInspectionWeight());
            if (ret == 0) {
              // todo: consider by number of hits? does the same inspection weight but more hits mean something better or something worse?

              // by time
              if (queryGoalNode1.getTimestamp() == queryGoalNode.getTimestamp()) {
                return 0;
              } else if (queryGoalNode1.getTimestamp() > queryGoalNode.getTimestamp()) {
                return 1;
              } else {
                return -1;
              }
            } else {
              return ret;
            }
          }
        });

        if (isTrainingFinalGoalAsSelf()) {
          // we also train the suggestion as suggested self,
          // as we store the dictionary key stripped from characters.
          // this will suggest "the da vinci code" from "thedavincicode" and "the davinci code".

          // it is however not needed to suppres bad suggestions from beeing suggested.
          // for that its enough with a low enough suggestion score detected by the suggester.

          for (QueryGoalNode<R> node : nodesWithGoals) {

            // but only register it once.
            SuggestionList suggestions = dictionary.getSuggestions(node.getQuery());
            if (suggestions == null) {
              suggestions = dictionary.suggestionListFactory(node.getQuery());
            }

            String suggestedQuery = nodesWithGoals.get(0).getQuery();
            if (!suggestions.containsSuggested(suggestedQuery)) {
              suggestions.addSuggested(suggestedQuery, 1d, nodesWithGoals.get(0).getcorpusQueryResults());
              dictionary.put(suggestions);
            }

            // uncomment to adapt every time
            // adaptPositive(dictionary, nodesWithGoals.get(0).getQuery(), nodesWithGoals.get(0).getCorpusQueryResults(), node);
          }
        }

        // suggest back from best goal to second best goal. homm -> heroes of might and magic -> homm
        if (nodesWithGoals.size() > 1) {
          adaptPositive(dictionary, nodesWithGoals.get(1).getQuery(), nodesWithGoals.get(1).getcorpusQueryResults(), nodesWithGoals.get(0));
        }

        // node without inspections are suggested to try the node with inspections that has least edit distance to the query.
        for (QueryGoalNode<R> node : nodesWithoutGoals) {
          QueryGoalNode<R> closestNode = findNodeWithShortestDistanceToQuery(node, nodesWithGoals);
          adaptPositive(dictionary, closestNode.getQuery(), closestNode.getcorpusQueryResults(), node);
        }
      }
    }
  }


  public EditDistance editDistanceFactory(String sd) {
    return new Levenshtein(sd);
  }


  private QueryGoalNode<R> findNodeWithShortestDistanceToQuery(QueryGoalNode<R> queryNode, Collection<QueryGoalNode<R>> targetNodes) {
    EditDistance editDistance = editDistanceFactory(queryNode.getQuery());
    QueryGoalNode<R> closest = null;
    double closestDistance = Double.MAX_VALUE;
    for (QueryGoalNode<R> targetNode : targetNodes) {
      double distanceToTarget = editDistance.getDistance(targetNode.getQuery());
      if (distanceToTarget < closestDistance) {
        closestDistance = distanceToTarget;
        closest = targetNode;
      }
    }
    return closest;
  }

  private void adaptPositive(Dictionary dictionary, String suggested, Integer suggestedCorpusQueryResults, QueryGoalNode<R> dictionaryKeyNode) throws QueryException {
    SuggestionList suggestions = dictionary.getSuggestions(dictionaryKeyNode.getQuery());
    if (suggestions == null) {
      suggestions = dictionary.suggestionListFactory(dictionaryKeyNode.getQuery());
      suggestions.addSuggested(suggested, 1d, suggestedCorpusQueryResults);
    } else {
      boolean suggestionUpdated = false;
      for (Suggestion existingSuggestion : suggestions) {
        if (existingSuggestion.getSuggested().equals(suggested)) {
          // the query already have this suggestion in the suggestions.
          // increase the score for the suggestion. (positive adaptation)
          double score = existingSuggestion.getScore() * getNotSuggestedPositiveAdaptationFactor();
          if (score > 9999) {
            score = 9999;
          }
          existingSuggestion.setScore(score);
          suggestionUpdated = true;
          break;
        }
      }
      if (!suggestionUpdated) {
        suggestions.addSuggested(suggested, 1d, suggestedCorpusQueryResults);
      }
      suggestions.sort();
    }
    dictionary.put(suggestions);
  }


  public double getAcceptedSuggestionPositiveAdaptationFactor() {
    return acceptedSuggestionPositiveAdaptationFactor;
  }

  public void setAcceptedSuggestionPositiveAdaptationFactor(double acceptedSuggestionPositiveAdaptationFactor) {
    this.acceptedSuggestionPositiveAdaptationFactor = acceptedSuggestionPositiveAdaptationFactor;
  }

  public double getIgnoredSuggestionNegativeAdaptationFactor() {
    return ignoredSuggestionNegativeAdaptationFactor;
  }

  public void setIgnoredSuggestionNegativeAdaptationFactor(double ignoredSuggestionNegativeAdaptationFactor) {
    this.ignoredSuggestionNegativeAdaptationFactor = ignoredSuggestionNegativeAdaptationFactor;
  }

  public double getNotSuggestedPositiveAdaptationFactor() {
    return notSuggestedPositiveAdaptationFactor;
  }

  public void setNotSuggestedPositiveAdaptationFactor(double notSuggestedPositiveAdaptationFactor) {
    this.notSuggestedPositiveAdaptationFactor = notSuggestedPositiveAdaptationFactor;
  }

  public boolean isTrainingFinalGoalAsSelf() {
    return trainingFinalGoalAsSelf;
  }

  public void setTrainingFinalGoalAsSelf(boolean trainingFinalGoalAsSelf) {
    this.trainingFinalGoalAsSelf = trainingFinalGoalAsSelf;
  }

  public static Comparator<Suggestion> getScoreComparator() {
    return scoreComparator;
  }

  public static void setScoreComparator(Comparator<Suggestion> scoreComparator) {
    DefaultTrainer.scoreComparator = scoreComparator;
  }


  /**
   * @return Stragegy used when a query goal tree has no goals.
   */
  public QueryGoalJuror<R> getJuror() {
    return juror;
  }

  /**
   * @param juror Stragegy used when a query goal tree has no goals.
   */
  public void setJuror(QueryGoalJuror<R> juror) {
    this.juror = juror;
  }

}
