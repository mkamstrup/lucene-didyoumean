package org.apache.lucene.search.didyoumean.session;

/**
 * Checked exception throw when there is an error creating a session or
 * otherwise controlling a {@link QuerySession}.
 */
public class SessionException extends Exception {

    public SessionException() {
        super();
    }

    public SessionException(String msg) {
        super(msg);
    }

    public SessionException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SessionException(Throwable cause) {
        super(cause);
    }

}
