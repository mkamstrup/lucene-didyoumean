package org.apache.lucene.search.didyoumean;

import com.sleepycat.je.DatabaseException;
import org.apache.lucene.search.didyoumean.dictionary.Dictionary;

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
 * The analyzing part of a session analyzed dictionary.
 * It build and updates the dictionary based on what users did during their visit.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: Jul 31, 2006
 *         Time: 4:46:13 PM
 */
public interface Trainer<R> {

  /**
   * @param dictionary   the dictionary to update.
   * @param goalTreeRoot the query goal tree to be used for training.
   */
  public abstract void trainGoalTree(Dictionary dictionary, QueryGoalNode<R> goalTreeRoot) throws DatabaseException;


//  /**
//   * Places a goal tree in the queue.
//   *
//   * @param goalTreeRoot goal tree to be queued.
//   * @see org.apache.lucene.search.didyoumean.Trainer#flush(org.apache.lucene.search.didyoumean.dictionary.Dictionary)
//   */
//  public abstract void queueGoalTree(QueryGoalNode<R> goalTreeRoot);
//
//  /**
//   * Process all available goal trees in the queue.
//   *
//   * @param dictionary dictionary to work against
//   */
//  public abstract void flush(Dictionary dictionary) throws DatabaseException;

}
