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

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A node in a tree describing what queries, suggestions and inspection of results
 * that a user undertook while searching for a specific thing.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-jan-31
 *         Time: 06:23:42
 */
@Persistent
public class QueryGoalNode<R>  {

  private QueryGoalNode<R> parent;
  private List<QueryGoalNode<R>> children = new ArrayList<QueryGoalNode<R>>();

  /** time of query */
  private Long timestamp;
  /** user query */
  private String query;
  /** number of hits in corpus from query */
  private Integer corpusQueryResults;
  /** the suggested text from the dictonary */
  private String suggestion;

  /** corpus query results inspected by user */
  private List<Inspection> inspections = new LinkedList<Inspection>();

  /** bdb persistence */
  private QueryGoalNode() {
  }

  public QueryGoalNode(QueryGoalNode<R> parentNode, String query, Integer corpusQueryResults) {
    this(parentNode, query, corpusQueryResults, null, System.currentTimeMillis());
  }

  public QueryGoalNode(QueryGoalNode<R> parentNode, String query, Integer corpusQueryResults, Long timeStamp) {
    this(parentNode, query, corpusQueryResults, null, timeStamp);
  }


  public QueryGoalNode(QueryGoalNode<R> parentNode, String query, Integer corpusQueryResults, String suggestion) {
    this(parentNode, query, corpusQueryResults, suggestion, System.currentTimeMillis());
  }

  public QueryGoalNode(QueryGoalNode<R> parentNode, String query, Integer corpusQueryResults, String suggestion, Long timeStamp) {
    this.parent = parentNode;
    if (parentNode != null) {
      parentNode.getChildren().add(this);
    }
    this.query = query;
    this.corpusQueryResults = corpusQueryResults;
    this.suggestion = suggestion;
    this.timestamp = timeStamp;
  }

  public Double calculateInspectionWeight() {
    if (getInspections().size() == 0) {
      return null;
    }
    double score = 1d;
    for (Inspection inspection : getInspections()) {
      score *= 1 + inspection.getGoalClassification();
    }
    return score;
  }

  public QueryGoalNode<R> getRoot() {
    QueryGoalNode<R> node = this;
    while (node.getParent() != null) {
      node = node.getParent();
    }
    return node;
  }


  public QueryGoalNode<R> getParent() {
    return parent;
  }

  public void setParent(QueryGoalNode<R> parent) {
    this.parent = parent;
  }

  public List<QueryGoalNode<R>> getChildren() {
    return children;
  }

  public void setChildren(List<QueryGoalNode<R>> children) {
    this.children = children;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getSuggestion() {
    return suggestion;
  }

  public void setSuggestion(String suggestion) {
    this.suggestion = suggestion;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }


  public Integer getcorpusQueryResults() {
    return corpusQueryResults;
  }

  public void setcorpusQueryResults(Integer corpusQueryResults) {
    this.corpusQueryResults = corpusQueryResults;
  }

  public List<Inspection> getInspections() {
    return inspections;
  }

  public void setInspections(List<Inspection> inspections) {
    this.inspections = inspections;
  }

  /**
   * The user was really looking for Windows vista, but searched only for vista.
   * Buena vista social club was found, but as this subject also was compelling
   * the user went on and read about that instead.
   */
  public static final Double NO_PART_OF_THE_GOAL = -1d;

  /**
   * We have no clue to wether the inspected reference was a part of the goal or not.
   */
  public static final Double MOO = 0d;

  /**
   * To set this high value, the users should explicitally have told the system
   * that they found just the thing they were looking for.
   */
  public static final Double GOAL = 1d;

  @Persistent
  public class Inspection {
    /** reference to the inspected item. */
    private R reference;
    /** time of inspection */
    private Long timeStamp;
    /** wether or not this inspection was the goal of the query. */
    private double goalClassification;

    public Inspection(R reference, double goalClassification) {
      this(reference, goalClassification, System.currentTimeMillis());
    }

    public Inspection(R reference, double goalClassification, Long timeStamp) {
      this.reference = reference;
      this.goalClassification = goalClassification;
      this.timeStamp = timeStamp;
      QueryGoalNode.this.getInspections().add(this);
    }


    public R getReference() {
      return reference;
    }

    public void setReference(R reference) {
      this.reference = reference;
    }

    public Long getTimeStamp() {
      return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
      this.timeStamp = timeStamp;
    }

    public double getGoalClassification() {
      return goalClassification;
    }

    public void setGoalClassification(double goalClassification) {
      this.goalClassification = goalClassification;
    }
  }

  public int numChildrenRecursive() {
    int i = 0;
    Iterator<QueryGoalNode<R>> it = iterateChildrenRecursive();
    while (it.hasNext()) {
      it.next();
      i++;
    }
    return i;
  }

  public Iterator<QueryGoalNode<R>> iterateChildrenRecursive() {
    return new Iterator<QueryGoalNode<R>>() {

      Iterator<QueryGoalNode<R>> children = getChildren().iterator();
      Iterator<QueryGoalNode<R>> subChildren;

      Iterator<QueryGoalNode<R>> currentChildren;

      QueryGoalNode<R> currentChild;
      QueryGoalNode<R> nextChild;

      public boolean hasNext() {

        if (nextChild != null) {
          return true;
        }

        while (true) {
          if (subChildren == null) {
            if (children.hasNext()) {
              nextChild = children.next();
              subChildren = nextChild.iterateChildrenRecursive();
              currentChildren = children; // for remove
              return true;
            } else {
              return false;
            }
          } else if (subChildren.hasNext()) {
            nextChild = subChildren.next();
            currentChildren = subChildren; // for remove
            return true;
          } else {
            subChildren = null;
          }
        }
      }

      public QueryGoalNode<R> next() {
        hasNext();
        currentChild = nextChild;
        nextChild = null;
        return currentChild;
      }

      public void remove() {
        currentChildren.remove();
      }
    };
  }

  public String toString() {
    return getQuery() + " " + getcorpusQueryResults() + " " + getInspections().size();
  }

}
