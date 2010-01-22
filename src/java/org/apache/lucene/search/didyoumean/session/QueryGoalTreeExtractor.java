package org.apache.lucene.search.didyoumean.session;

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


import java.util.List;

/**
 * A user session could contain multiple quests for content. For example,
 * first the user looks for the Apache licence,
 * spells it wrong, inspects different results,
 * and then the user search for the author Ivan Goncharov.
 * <p/>
 * In the session analyzing suggester, this is called different goals.
 * <p/>
 * It is up to the QueryGoalTreeExtractor implementations to decide what
 * events in a session are parts of the same goal, as we don't want to
 * suggest the user to check out Goncharov when looking for the Apache license.
 *
 * @author Karl Wettin <mailto:karl.wettin@gmail.com>
 *         Date: Aug 1, 2006
 *         Time: 2:33:47 PM
 */
public interface QueryGoalTreeExtractor<R> {

  public abstract List<QueryGoalNode<R>> extractGoalRoots(QueryGoalNode<R> session);
}
