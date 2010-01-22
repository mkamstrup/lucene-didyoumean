package org.apache.lucene.search.didyoumean;

import java.util.ArrayList;
import java.util.List;
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


/**
 * @author karl wettin <mailto:karl.wettin@gmail.com>
 * Date: 2007-okt-17
 * Time: 17:24:47
 */
@Entity
public class QuerySession<R> {

  private static long defaultExpirationTimeMilliseconds = 1000 * 60 * 10; // 10 minutes

  @PrimaryKey
  private String id;

  private long lastTouched = System.currentTimeMillis();
  private long expirationTimeMilliseconds = defaultExpirationTimeMilliseconds;

  private List<QueryGoalNode<R>> nodes = new ArrayList<QueryGoalNode<R>>();

  /**
   * Sets the last query node as parent, or null if no previous nodes.
   *
   * @param query user query
   * @param corpusQueryResults number of hits
   * @return query node index
   */
  public Integer query(String query, Integer corpusQueryResults) {
    return query(query, corpusQueryResults, null);
  }



  /**
   * Sets the last query node as parent, or null if no previous nodes.
   * 
   * @param query user query
   * @param corpusQueryResults number of hits
   * @param suggestion suggestion to user query as given by suggester
   * @return query node index
   */
  public Integer query(String query, Integer corpusQueryResults, String suggestion) {
    return query(query, corpusQueryResults, suggestion, System.currentTimeMillis());
  }

  /**
   * @param parentNodeIndex parent query
   * @param query user query
   * @param corpusQueryResults number of hits
   * @param suggestion suggestion to user query as given by suggester
   * @return query node index
   */
  public Integer query(Integer parentNodeIndex, String query, Integer corpusQueryResults, String suggestion) {
    return query(parentNodeIndex, query, corpusQueryResults, suggestion, System.currentTimeMillis());
  }

  /**
   * Sets the last query node as parent, or null if no previous nodes.
   *
   * @param query user query
   * @param corpusQueryResults number of hits
   * @param suggestion suggestion to user query as given by suggester
   * @param timeStamp
   * @return query node index
   */
  public Integer query(String query, Integer corpusQueryResults, String suggestion, Long timeStamp) {
    return query(nodes.size() == 0 ? null : nodes.size() - 1, query, corpusQueryResults, suggestion, timeStamp);
  }

  /**
   *
   * @param parentNodeIndex parent query
   * @param query user query
   * @param corpusQueryResults number of hits
   * @param suggestion suggestion to user query as given by suggester
   * @param timeStamp
   * @return query node index
   */
  public synchronized Integer query(Integer parentNodeIndex, String query, Integer corpusQueryResults, String suggestion, Long timeStamp) {
    nodes.add(new QueryGoalNode<R>(parentNodeIndex == null ? null : nodes.get(parentNodeIndex), query, corpusQueryResults, suggestion, timeStamp));
    lastTouched = timeStamp;
    return nodes.size() - 1;
  }

  public void inspect(int nodeIndex, R reference, double goalClassification) {
    inspect(nodeIndex, reference, goalClassification, System.currentTimeMillis());
  }

  public void inspect(int nodeIndex, R reference, double goalClassification, Long timeStamp) {
    lastTouched = timeStamp;
    nodes.get(nodeIndex).new Inspection(reference, goalClassification, timeStamp);    
  }

  public static long getDefaultExpirationTimeMilliseconds() {
    return defaultExpirationTimeMilliseconds;
  }

  public static void setDefaultExpirationTimeMilliseconds(long defaultExpirationTimeMilliseconds) {
    QuerySession.defaultExpirationTimeMilliseconds = defaultExpirationTimeMilliseconds;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getExpirationTimeMilliseconds() {
    return expirationTimeMilliseconds;
  }

  public void setExpirationTimeMilliseconds(long expirationTimeMilliseconds) {
    this.expirationTimeMilliseconds = expirationTimeMilliseconds;
  }


  public List<QueryGoalNode<R>> getNodes() {
    return nodes;
  }

  public void setNodes(List<QueryGoalNode<R>> nodes) {
    this.nodes = nodes;
  }


  public long getLastTouched() {
    return lastTouched;
  }

  public void setLastTouched(long lastTouched) {
    this.lastTouched = lastTouched;
  }

  public boolean isExpired() {
    return getLastTouched() + getExpirationTimeMilliseconds() < System.currentTimeMillis();
  }
}
