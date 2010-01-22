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


import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.Persistent;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 * A scored string value, a suggestion in a dictionary.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: Jul 31, 2006
 *         Time: 4:59:06 PM
 *         <p/>
 */
@Persistent
public class Suggestion implements Comparable<Suggestion> {

  private String suggested;
  private double score = 1d;
  private Integer corpusQueryResults;


  public Suggestion() {
  }

  public Suggestion(String suggested) {
    this.suggested = suggested;
  }


  public Suggestion(String suggested, double score) {
    this.suggested = suggested;
    this.score = score;
  }


  public Suggestion(String suggested, double score, Integer corpusQueryResults) {
    this.suggested = suggested;
    this.score = score;
    this.corpusQueryResults = corpusQueryResults;
  }

  /**
   *
   * @param suggestion suggestion this instance to be compared to
   * @return the suggestion scores compared.
   */
  public int compareTo(Suggestion suggestion) {
    return Double.compare(suggestion.getScore(), getScore());
  }


  public Integer getCorpusQueryResults() {
    return corpusQueryResults;
  }

  public void setCorpusQueryResults(Integer corpusQueryResults) {
    this.corpusQueryResults = corpusQueryResults;
  }

  public String getSuggested() {
    return suggested;
  }

  public void setSuggested(String suggested) {
    this.suggested = suggested;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  private static final DecimalFormat df = new DecimalFormat("#.##");

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(df.format(getScore()));
    sb.append('\t');
    sb.append(getSuggested());
    return sb.toString();
  }


}
