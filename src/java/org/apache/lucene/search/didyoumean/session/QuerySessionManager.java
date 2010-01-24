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
public interface QuerySessionManager<R> extends Iterable<QuerySession<R>> {

  public void close() throws SessionException;

  public void put(QuerySession<R> session) throws SessionException;

  public QuerySession<R> remove(String sessionId) throws SessionException;

  public QuerySession<R> querySessionFactory() throws SessionException;

  public QuerySession<R> querySessionFactory(String id) throws SessionException;

}
