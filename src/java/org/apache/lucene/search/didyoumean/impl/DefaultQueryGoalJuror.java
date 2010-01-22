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


import org.apache.lucene.search.didyoumean.QueryGoalNode;

import java.util.*;

/**
 * Sets the chronologycally last placed query as the goal.
 *
 * @see QueryGoalJuror
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 * Date: 2007-feb-23
 * Time: 23:53:21
 */
public class DefaultQueryGoalJuror<R> implements QueryGoalJuror<R> {

  // private static Log log = LogFactory.getLog(HitsBasedJuror.class);
  // private static long serialVersionUID = 1l;

  /**
   * Sets the chronologycally last placed query as the goal.
   * @param goalTreeRootNode Query goal tree root
   * @return the goal nodes
   * @throws RuntimeException If query goal tree already contains one or more goals.
   */
  public List<QueryGoalNode<R>> createGoals(QueryGoalNode<R> goalTreeRootNode) {

    List<QueryGoalNode<R>> nodes = new ArrayList<QueryGoalNode<R>>();

    for (Iterator<QueryGoalNode<R>> children = goalTreeRootNode.iterateChildrenRecursive(); children.hasNext();) {
      QueryGoalNode<R> node = children.next();
      for (QueryGoalNode.Inspection inspection : node.getInspections()) {
        if (inspection.getGoalClassification() > QueryGoalNode.MOO) {
          throw new RuntimeException("The query goal tree already contains one or more goals!");
        }
      }
      nodes.add(node);
    }
    nodes.add(goalTreeRootNode);


    Collections.sort(nodes, new Comparator<QueryGoalNode<R>>() {
      public int compare(QueryGoalNode<R> queryGoalNode, QueryGoalNode<R> queryGoalNode1) {
        return queryGoalNode1.getTimestamp().compareTo(queryGoalNode.getTimestamp());
      }

    });

    List<QueryGoalNode<R>> goalNodes = new LinkedList<QueryGoalNode<R>>();

    if (nodes.get(0).getcorpusQueryResults() > 0) {

      nodes.get(0).new Inspection(null, QueryGoalNode.GOAL);
      goalNodes.add(nodes.get(0));
    }

    return goalNodes;


  }

}
