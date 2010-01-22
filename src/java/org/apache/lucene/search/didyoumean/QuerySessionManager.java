package org.apache.lucene.search.didyoumean;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;

import java.io.File;
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
 * Time: 18:42:42
 */
public class QuerySessionManager<R> {

  private EntityStore store;
  private PrimaryIndex<String, QuerySession> sessionsByID;


  public QuerySessionManager(File bdbPath) throws DatabaseException {
    if (!bdbPath.exists()) {
      System.out.println("Creating path " + bdbPath);
      bdbPath.mkdirs();
    }

    EnvironmentConfig envConfig = new EnvironmentConfig();
    envConfig.setAllowCreate(true);
    envConfig.setTransactional(false);
    envConfig.setLocking(false);
    Environment env = new Environment(bdbPath, envConfig);

    StoreConfig storeConfig = new StoreConfig();
    storeConfig.setAllowCreate(true);
    storeConfig.setTransactional(false);
    store = new EntityStore(env, "sessionManager", storeConfig);

    sessionsByID = store.getPrimaryIndex(String.class, QuerySession.class);
  }

  public void close() throws DatabaseException {
    store.close();
  }

  public EntityStore getStore() {
    return store;
  }

  public PrimaryIndex<String, QuerySession> getSessionsByID() {
    return sessionsByID;
  }

  public synchronized QuerySession<R> querySessionFactory() throws DatabaseException {
    // ensure unique session id, todo better solution
    try {
      Thread.sleep(1);
    } catch (InterruptedException ie) {
      // whatever
    }
    return querySessionFactory(String.valueOf(System.currentTimeMillis()));
  }

  public QuerySession<R> querySessionFactory(String id) throws DatabaseException {
    QuerySession<R> querySession = new QuerySession<R>();
    querySession.setId(id);
    getSessionsByID().put(querySession);
    return querySession;
  }

}
