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


import org.apache.lucene.util.PriorityQueue;

import java.util.LinkedList;

/**
 * User: kalle
 * Date: 2007-mar-03
 * Time: 13:14:38
 */
public class SuggestionPriorityQueue extends PriorityQueue<Suggestion> {

  public SuggestionPriorityQueue(int maxSize) {
    initialize(maxSize);
  }


  protected boolean lessThan(Suggestion a, Suggestion b) {
    return a.compareTo(b) < 0;
  }

  public Suggestion[] toArray() {
    // FIXME: This implementation has quite an overhead, and has the undocumented sideeffect of emptying the heap
    LinkedList<Suggestion> list = new LinkedList<Suggestion>();
    Suggestion s;
    while ((s = (Suggestion) pop()) != null) {
      list.add(s);
    }
    return list.toArray(new Suggestion[list.size()]);
  }

}
