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

/**
 * Interface for session managers
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since 2010-01-24
 */
public abstract class QuerySessionManager<R> implements Iterable<QuerySession<R>> {

  public abstract void close() throws SessionException;

  public abstract void put(QuerySession<R> session) throws SessionException;

  public abstract QuerySession<R> remove(String sessionId) throws SessionException;

  public synchronized QuerySession<R> querySessionFactory() throws SessionException {
    // TODO: Relicense dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator and use it for unique session ids
    try {
      Thread.sleep(1);
    } catch (InterruptedException ie) {
      // whatever
    }
    return querySessionFactory(String.valueOf(System.currentTimeMillis()));
  }

  public QuerySession<R> querySessionFactory(String id) throws SessionException {
    QuerySession<R> querySession = new QuerySession<R>();
    querySession.setId(id);
    put(querySession);
    return querySession;
  }

}
