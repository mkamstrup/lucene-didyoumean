package org.apache.lucene.search.didyoumean.session;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * FIXME: Missing class docs for org.apache.lucene.search.didyoumean.session.MemoryQuerySessionManager
 *
 * @author mke
 * @since Jan 25, 2010
 */
public class MemoryQuerySessionManager<R> extends QuerySessionManager<R> {

  private Map<String,QuerySession<R>> sessions = new HashMap<String,QuerySession<R>>();

  public void close() throws SessionException {
    sessions.clear();
  }

  public void put(QuerySession querySession) throws SessionException {
    sessions.put(querySession.getId(), querySession);
  }

  public QuerySession<R> remove(String sessionId) throws SessionException {
    return sessions.remove(sessionId);
  }

  public Iterator<QuerySession<R>> iterator() {
    return sessions.values().iterator();
  }
}
