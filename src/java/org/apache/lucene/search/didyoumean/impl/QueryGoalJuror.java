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

import java.util.List;

/**
 * When a query goal tree has no goals, an implementation of this interface is used to set them up. 
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 * Date: 2007-feb-24
 * Time: 02:25:25
 */
public interface QueryGoalJuror<R> {

  /**
   * @param goalTreeRootNode Query goal tree root node.
   * @return the goal nodes
   */
  public abstract List<QueryGoalNode<R>> createGoals(QueryGoalNode<R> goalTreeRootNode);
}
