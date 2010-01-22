package org.apache.lucene.search.didyoumean.secondlevel.token;
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


import org.apache.lucene.search.didyoumean.Suggestion;

import java.io.Serializable;

/**
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: 2007-feb-03
 *         Time: 05:43:03
 */
public class TokenSuggestion extends Suggestion implements Serializable {

  // private static Log log = LogFactory.getLog(SingleTokenSuggestion.class);
  private static long serialVersionUID = 1l;

  private int frequency;


  public TokenSuggestion() {
  }

  public TokenSuggestion(String suggested, double score, int frequency) {
    super(suggested, score);
    this.frequency = frequency;
  }


  public int getFrequency() {
    return frequency;
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }


  public int compareTo(Suggestion suggestion) {
    if (suggestion instanceof TokenSuggestion) {
      TokenSuggestion tokenSuggestion = (TokenSuggestion) suggestion;
      // first criteria: the edit distance
      if (getScore() > tokenSuggestion.getScore()) {
        return 1;
      }
      if (getScore() < tokenSuggestion.getScore()) {
        return -1;
      }

      // second criteria (if first criteria is equal): the popularity
      if (getFrequency() > tokenSuggestion.getFrequency()) {
        return 1;
      }

      if (getFrequency() < tokenSuggestion.getFrequency()) {
        return -1;
      }
      return 0;
    } else {
      return super.compareTo(suggestion);
    }
  }
}
