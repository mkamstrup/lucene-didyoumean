package org.apache.lucene.search.didyoumean.session;

import java.io.IOException;

/**
 * Checked exception throw when there is an error creating a session or
 * otherwise controlling a {@link QuerySession}.
 * <p/>
 * Session exceptions are sub classes of {@link IOException}s because it is
 * expected that session managers are backed by some persistence layer where
 * the primary point of failure lies.
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since 2010-01-24
 */
public class SessionException extends IOException {

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
