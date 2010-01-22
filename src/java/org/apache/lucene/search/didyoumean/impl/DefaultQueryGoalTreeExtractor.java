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
import org.apache.lucene.search.didyoumean.QueryGoalNode;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p/>
 * A not too advanced way to extract the goals from a session.
 * <p/>
 * Nodes are parts of the same goal as their parent when:
 * <ul>
 * <li>the queries are the same</li>
 * <li>the suggestion to the parent query was followed</li>
 * <li>the queries are similair enough</li>
 * <li>the queries was entered within short enough time</li>
 * </ul>
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: Aug 1, 2006
 *         Time: 5:01:14 PM
 */
public class DefaultQueryGoalTreeExtractor<R> implements org.apache.lucene.search.didyoumean.QueryGoalTreeExtractor<R>, Serializable {

  private static final long serialVersionUID = 1l;

  public static final long DEFAULT_MAXIMUM_MILLISECONDS_BETWEEN_QUERIES = 20000;
  public static final double DEFAULT_MINIMUM_SIMILARITY = 0.6f;
  public static final int DEFAULT_MINIMUM_HITS_IN_FINAL_QUERY = 1;
  public static final double DEFAULT_MINIMUM_HITS_RATIO = 0f;


  private long maximumMillisecondsBetweenQueries = DEFAULT_MAXIMUM_MILLISECONDS_BETWEEN_QUERIES;
  private double minimumSimilarity = DEFAULT_MINIMUM_SIMILARITY;
  private int minimumHitsInFinalQuery = DEFAULT_MINIMUM_HITS_IN_FINAL_QUERY;
  private double minimumHitsRatio = DEFAULT_MINIMUM_HITS_RATIO;

  private double inspectionWeightGoalThreadshold = 0d;

  private class GoalFactory {
    private List<QueryGoalNode<R>> goalQueries = new LinkedList<QueryGoalNode<R>>();
  }

  public boolean isPartOfParentGoal(QueryGoalNode<R> child) {
    if (child.getParent() == null) {
      // no parent
      return false;
    } else if (child.getQuery().equals(child.getParent().getSuggestion())) {
      // the user followed a suggestion made by the system.
      return true;
    } else if (child.getQuery().equals(child.getParent().getQuery())) {
      // it is the same query placed once more.
      // todo: consider if this should have been merged when creating a new child in DefaultTrainer?
      return true;
    } else {
      // check edit distance

      EditDistance editDistance = editDistanceFactory(child.getParent().getQuery());
      //double similarity = ((double) distance / previousLink.link.length());
      double similarity = 1.0f - ((double) editDistance.getDistance(child.getQuery()) / Math.min(child.getQuery().length(), child.getParent().getQuery().length()));
      if (similarity >= getMinimumSimilarity()) {
        // similair enough
        return true;
      } else if (child.getTimestamp() - child.getParent().getTimestamp() < getMaximumMillisecondsBetweenQueries()) {
        // todo: consider time spent inspecting et c.

        // not similair enough.
        // but fast enough between searches.

        // if the child or its children reached the goal // todo recursive
        // then it is a part of the parent goal.

        Double weight = child.calculateInspectionWeight();
        if (weight == null) {
          return false;
        } else {
          return weight > getInspectionWeightGoalThreadshold();
        }

      }
    }

    return false;
  }

  public EditDistance editDistanceFactory(String sd) {
    return new Levenshtein(sd);
  }


  public List<QueryGoalNode<R>> extractGoalRoots(QueryGoalNode<R> sessionRoot) {

    List<QueryGoalNode<R>> goalRoots = new LinkedList<QueryGoalNode<R>>();

    Map<QueryGoalNode<R>, GoalFactory> goalFactoryByNodes = new HashMap<QueryGoalNode<R>, GoalFactory>();


    Queue<QueryGoalNode<R>> leafsQueue = new ConcurrentLinkedQueue<QueryGoalNode<R>>();
    for (Iterator<QueryGoalNode<R>> leafsIterator = sessionRoot.iterateChildrenRecursive(); leafsIterator.hasNext();) {
      QueryGoalNode<R> node = leafsIterator.next();
      if (node.getChildren().size() == 0) {
        leafsQueue.add(node);
      }
    }

    if (leafsQueue.size() == 0) {
      // this is just a single node.
      goalRoots.add(sessionRoot);
    } else while (leafsQueue.size() > 0) {
      QueryGoalNode<R> node = leafsQueue.poll();
      while (isPartOfParentGoal(node)) {
        GoalFactory nodeGoalFactory = goalFactoryByNodes.get(node);
        if (nodeGoalFactory == null) {
          nodeGoalFactory = new GoalFactory();
          nodeGoalFactory.goalQueries.add(node);
          goalFactoryByNodes.put(node, nodeGoalFactory);
        }

        if (node.getParent() != null) {
          GoalFactory parentGoalFactory = goalFactoryByNodes.get(node.getParent());
          if (parentGoalFactory != null) {
            // the parent has already been added to a goal factory
            // merge this goal factory with the parent goal factory.
            for (QueryGoalNode<R> childGoalNode : goalFactoryByNodes.get(node).goalQueries) {
              goalFactoryByNodes.put(childGoalNode, parentGoalFactory);
              parentGoalFactory.goalQueries.add(childGoalNode);
            }
          } else {
            goalFactoryByNodes.put(node, nodeGoalFactory);
            nodeGoalFactory.goalQueries.add(node.getParent());
          }
        }

        node = node.getParent();
      }

      // make current node a new root
      if (node.getParent() != null) {
        leafsQueue.add(node.getParent()); // parent is a new goal leaf!
        node.getParent().getChildren().remove(node);
        node.setParent(null);
      }
      goalRoots.add(node);
    }

    return goalRoots;
  }

  /**
   * @return maximun time in milliseconds between two queries to be considered part of the same correction sequence
   */
  public final long getMaximumMillisecondsBetweenQueries() {
    return maximumMillisecondsBetweenQueries;
  }

  /**
   * @param maximumMillisecondsBetweenQueries
   *         maximun time in milliseconds between two queries to be considered part of the same correction sequence
   */
  public final void setMaximumMillisecondsBetweenQueries(long maximumMillisecondsBetweenQueries) {
    this.maximumMillisecondsBetweenQueries = maximumMillisecondsBetweenQueries;
  }

  /**
   * @return minimum edit-distance between two queries in order to be considered part of the same correction sequence
   */
  public final double getMinimumSimilarity() {
    return minimumSimilarity;
  }

  /**
   * Using Levenshtein edit distance factory,
   * anything less than 0.5 is crazy,
   * 0.7 can be almost be considered fail-safe,
   * but as this is an adapting spell checker some bad data is ok,
   * so I set default to 0.6
   *
   * @param minimumSimilarity minimum edit-distance between two queries in order to be considered part of the same correction sequence
   */
  public final void setMinimumSimilarity(double minimumSimilarity) {
    this.minimumSimilarity = minimumSimilarity;
  }

  /**
   * @return number of hits required in the final query of a correction sequence
   */
  public final int getMinimumHitsInFinalQuery() {
    return minimumHitsInFinalQuery;
  }

  /**
   * @param minimumHitsInFinalQuery number of hits required in the final query of a correction sequence
   */
  public final void setMinimumHitsInFinalQuery(int minimumHitsInFinalQuery) {
    this.minimumHitsInFinalQuery = minimumHitsInFinalQuery;
  }

  /**
   * @return minimum allowed ratio of the low and top number of hits of any two queries in a correction sequence. default to 2, that is twice as many hits in the top resulting query is required
   */
  public final double getMinimumHitsRatio() {
    return minimumHitsRatio;
  }

  /**
   * @param minimumHitsRatio minimum allowed ratio of the low and top number of hits of any two queries in a correction sequence. default to 2, that is twice as many hits in the top resulting query is required
   */
  public final void setMinimumHitsRatio(double minimumHitsRatio) {
    this.minimumHitsRatio = minimumHitsRatio;
  }


  public double getInspectionWeightGoalThreadshold() {
    return inspectionWeightGoalThreadshold;
  }

  public void setInspectionWeightGoalThreadshold(double inspectionWeightGoalThreadshold) {
    this.inspectionWeightGoalThreadshold = inspectionWeightGoalThreadshold;
  }
}
