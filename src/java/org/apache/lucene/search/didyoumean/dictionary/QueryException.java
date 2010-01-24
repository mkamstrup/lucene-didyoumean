package org.apache.lucene.search.didyoumean.dictionary;

import java.io.IOException;

/**
 * Thrown when there is an error performing a query against a {@link Dictionary}
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Jan 22, 2010
 */
public class QueryException extends IOException {

  public QueryException() {
    super();
  }

  public QueryException(String msg) {
    super(msg);
  }

  public QueryException(String msg, Throwable cause) {
    this(msg);
    initCause(cause);
  }

  public QueryException(Throwable cause) {
    this();
    initCause(cause);
  }
}
