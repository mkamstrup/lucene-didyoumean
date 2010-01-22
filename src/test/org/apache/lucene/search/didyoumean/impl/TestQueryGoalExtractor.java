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



import junit.framework.TestCase;

import java.util.List;

import org.apache.lucene.search.didyoumean.QueryGoalNode;

/**
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: Aug 4, 2006
 *         Time: 6:41:38 AM
 */
public abstract class TestQueryGoalExtractor<R> extends TestCase {

  private org.apache.lucene.search.didyoumean.QueryGoalTreeExtractor goalTreeExtractor;

  @Override
  protected void setUp() throws Exception {
    goalTreeExtractor = goalTreeExtractorFactory();
  }

  protected abstract org.apache.lucene.search.didyoumean.QueryGoalTreeExtractor goalTreeExtractorFactory();

  public void testSingleNode() throws Exception {
    assertEquals(1, goalTreeExtractor.extractGoalRoots(new QueryGoalNode<R>(null, "heroes of knight and magic", 10, null, 1l)).size());

  }

  public void testTime() throws Exception {

    QueryGoalNode<R> node;

    QueryGoalNode<R> session = new QueryGoalNode<R>(null, "heroes of knight and magic", 10, null, 1l);
    node = new QueryGoalNode<R>(session, "heroes of knight and magic", 10, 0l);
    node = new QueryGoalNode<R>(node, "heroes of night and magic", 13, 10000l);
    node = new QueryGoalNode<R>(node, "heroes of might and magic", 132, 15000l);
    node = new QueryGoalNode<R>(node, "heroes of might and magic 3", 12, 25000l);
    node.new Inspection(null, QueryGoalNode.MOO, 25000l);
    node = new QueryGoalNode<R>(node, "heroes of might and magic iv", 12, 100000l);
    node = new QueryGoalNode<R>(node, "heroes of might and magic 4", 6, 105000l);
    node.new Inspection(null, QueryGoalNode.GOAL, 105000l);

    assertEquals(1, goalTreeExtractor.extractGoalRoots(session).size());

  }

  public void testSimilarity() throws Exception {

    QueryGoalNode<R> node;
    QueryGoalNode<R> session = new QueryGoalNode<R>(null, "heroes of knight and magic", 10, null, 0l);
    node = new QueryGoalNode<R>(session, "heroes of night and magic", 13, 10000l);
    node = new QueryGoalNode<R>(node, "heroes of might and magic", 132, 15000l);
    node = new QueryGoalNode<R>(node, "heroes of might and magic 3", 12, 25000l);

    node = new QueryGoalNode<R>(node, "the davinci code", 12, 27000l);
    new QueryGoalNode<R>(node, "the da vinci code", 6, 29000l);

    List<QueryGoalNode<R>> goalTreeRoots = goalTreeExtractor.extractGoalRoots(session);

    assertEquals(2, goalTreeRoots.size());
    assertEquals(1, goalTreeRoots.get(0).numChildrenRecursive());
    assertEquals(3, goalTreeRoots.get(1).numChildrenRecursive());

  }

  public void testHits() throws Exception {
    QueryGoalNode<R> node;
    QueryGoalNode<R> session = new QueryGoalNode<R>(null, "heroes of knight and magic", 10, null, 0l);
    node = new QueryGoalNode<R>(session, "heroes of night and magic", 13, 10000l);
    node = new QueryGoalNode<R>(node, "heroes of might and magic", 132, 15000l);
    node = new QueryGoalNode<R>(node, "heroes of might and magic 3", 0, 25000l);

    node = new QueryGoalNode<R>(node, "the davinci code", 12, 27000l);
    new QueryGoalNode<R>(node, "the da vinci code", 6, 29000l);

    List<QueryGoalNode<R>> goalTreeRoots = goalTreeExtractor.extractGoalRoots(session);

    assertEquals(2, goalTreeRoots.size());
    assertEquals(1, goalTreeRoots.get(0).numChildrenRecursive());
    assertEquals(3, goalTreeRoots.get(1).numChildrenRecursive());


  }

}
