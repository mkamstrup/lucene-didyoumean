package org.apache.lucene.search.didyoumean.dictionary;

import org.apache.lucene.search.didyoumean.Suggester;
import org.apache.lucene.search.didyoumean.Suggestion;

import java.io.IOException;
import java.sql.*;
import java.util.Iterator;

/**
 * A persistent dictionary implementation talking to a JDBC SQL backend.
 * <p/>
 * The database consists of three tables:
 * <ul>
 *   <li><b>dict(</b><code>PRIMARY KEY(queryKeyId,suggId), queryKeyId INTEGER, suggId INTEGER</code><b>)</b></li>
 *   <li><b>sugg(</b><code>PRIMARY KEY(id), UNIQUE(string), id INTEGER, string VARCHAR(1024), score DOUBLE, results INTEGER</code><b>)</b></li>
 *   <li><b>query(</b><code>PRIMARY KEY(keyId), keyId INTEGER, key VARCHAR(1024)</code><b>)</b></li>
 * </ul>
 * It is the responsibility of the {@link #createTables()} method to set this up.
 * The columns {@code dict.queryKeyId}, {@code sugg.id}, and {@code query.keyId} <i>must</i>
 * be set to automatically increment upon insertion.
 * <p/>
 * For performance reasons it is highly recommended to put an index on the {@code sugg.string} column
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Jan 27, 2010
 */
public abstract class JDBCDictionary extends Dictionary {

  protected Connection conn;

  public JDBCDictionary(Connection conn) throws SQLException {
    this.conn = conn;
    conn.setAutoCommit(false);
    createTables();
  }

  /**
   * Create the tables as described in the class docs. Since SQL
   * table creation is highly non-portable between JDBC backends
   * most databases are expected to require their own implementation
   * of this method in a subclass
   * @throws SQLException if there is an error creating the tables
   */
  protected abstract void createTables() throws SQLException;

  @Override
  public SuggestionList getSuggestions(String query) throws QueryException {
    SuggestionList suggestions = suggestionListFactory(query);
    PreparedStatement stmt = null;
    try {
      stmt = conn.prepareStatement(
        "SELECT sugg.string, sugg.score, sugg.results FROM sugg, dict, query " +
        "WHERE query.key=? AND query.keyId=dict.queryKeyId AND dict.suggId=sugg.id");
      stmt.setString(1, suggestions.getQueryKey());
      ResultSet result = stmt.executeQuery();

      while (result.next()) {
        suggestions.addSuggested(result.getString(1), result.getDouble(2), result.getInt(3));
      }
    } catch (SQLException e) {
      throw new QueryException(String.format(
                  "Failed to get suggestions for '%s'", query), e);
    } finally {
      try {
        if (stmt != null) stmt.close();
      } catch (SQLException e) {
        // FIXME: Don't throw inside a finally block... But this is ugly:
        e.printStackTrace();
        System.err.println(
          "Error closing SQL statement while getting suggestions for '" + query + "'");
      }
    }
    return suggestions;
  }

  @Override
  public void close() throws IOException {
    try {
      conn.close();
    } catch (SQLException e) {
      IOException ioe = new IOException("Failed to close database connection");
      ioe.initCause(e);
      throw ioe;
    }
  }

  @Override
  public void put(SuggestionList suggestions) {
    // Sorry about the try/catch hell here - but JDBC is like that when you try to play safe
    for (Suggestion suggestion : suggestions) {
      try {
        put(suggestions.getQueryKey(), suggestion);
        conn.commit();
      } catch (SQLException e) {
        try {
          conn.rollback();
        } catch (SQLException e1) {
          e1.printStackTrace();
          System.err.println(String.format(
            "Failed to insert suggestion '%s'", suggestion));
        }
      }
    }
  }

  /**
   * Insert {@code suggestion} and associate it with the query key {@code queryKey}
   * @param queryKey the query key of the {@link SuggestionList} containing {@code suggestion}
   * @param suggestion the suggestion to insert
   * @throws SQLException
   */
  protected void put(String queryKey, Suggestion suggestion) throws SQLException {
    int queryKeyId = checkQueryTable(queryKey);
    int suggId = checkSuggTable(suggestion);
    checkDictTable(queryKeyId, suggId);
  }

  /**
   * Make sure that the {@code query} table contains the query key for {@code suggestion}
   * and return the corresponding (integer valued) {@code keyId}
   * @param queryKey the query key to get the key id for
   * @return the value of the {@code query.keyId} column for the query key
   * @throws SQLException if there is an error working with the database
   */
  protected int checkQueryTable(String queryKey) throws SQLException {
    PreparedStatement insert = conn.prepareStatement(
        "INSERT INTO query(key) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
    PreparedStatement select = conn.prepareStatement(
        "SELECT keyId FROM query WHERE key=?");    

    ResultSet result;
    try {
      insert.setString(1, queryKey);
      insert.executeUpdate();
      result = insert.getGeneratedKeys();
    } catch (SQLException e) {
      // We can only assume that this is an SQLIntegrityConstraintViolationException,
      // but not JDBC drivers throw such exception when they should
      select.setString(1, queryKey);
      result = select.executeQuery();
    }

    return readId(queryKey, result);
  }

  /**
   * Insert or update the {@code sugg} table with the data from {@code suggestion}
   * returning the integer id of the affacted row (ie. {@code sugg.id})
   * @param suggestion the suggestion to insert or update
   * @return the value of the affected {@code sugg.id} column
   * @throws SQLException if there is an error working with the database
   */
  protected int checkSuggTable (Suggestion suggestion) throws SQLException {
    PreparedStatement insert = conn.prepareStatement(
      "INSERT INTO sugg(string,score,results) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
    PreparedStatement select = conn.prepareStatement("" +
      "SELECT id FROM sugg WHERE string=?");
    PreparedStatement update = conn.prepareStatement(
      "UPDATE sugg SET score=?, results=? WHERE id=?");

    try {
      insert.setString(1, suggestion.getSuggested());
      insert.setDouble(2, suggestion.getScore());
      insert.setInt(3, suggestion.getCorpusQueryResults());
      insert.executeUpdate();
      return readId(suggestion.getSuggested(), insert.getGeneratedKeys());
    } catch (SQLException e) {
      // We can only assume that this is an SQLIntegrityConstraintViolationException,
      // but not JDBC drivers throw such exception when they should. So we just
      // try and update the suggestion with the given id
      select.setString(1, suggestion.getSuggested());
      int suggId = readId(suggestion.getSuggested(), select.executeQuery());

      update.setDouble(1, suggestion.getScore());
      update.setInt(2, suggestion.getCorpusQueryResults());
      update.setInt(3, suggId);
      update.executeUpdate();

      return suggId;
    }
  }

  protected void checkDictTable(int queryKeyId, int suggId) throws SQLException {
    PreparedStatement insert = conn.prepareStatement(
      "INSERT INTO dict VALUES (?,?)");
    insert.setInt(1, queryKeyId);
    insert.setInt(2, suggId);
    insert.executeUpdate();
  }

  /**
   * Read a single integer id out of a result set
   * @param name name to include in debug messages
   * @param result the result set - must contain exactly one row
   * @return the integer value contained in the result set
   * @throws SQLException if the result set doesn't contain exactly one row
   */
  private int readId(String name, ResultSet result) throws SQLException {
    // Result set should have at least one row
    if (!result.next()) {
      result.close();
      throw new SQLException(String.format(
        "Missing data when looking id for '%s'", name));
    }

    int id = result.getInt(1);

    // Result set must have *exactly* one row
    if (result.next()) {
      result.close();
      throw new SQLException(String.format(
        "Malformed result set looking up id for '%s'", name));
    }

    result.close();
    return id;
  }

  @Override
  public void optimize(Suggester suggester) throws IOException {
    //FIXME
  }

  @Override
  public void prune(int maxSize) throws IOException {
    //FIXME
  }

  @Override
  public int size() {
    PreparedStatement stmt = null;
    try {
      stmt = conn.prepareStatement(
        "SELECT count(*) FROM query");
      ResultSet result = stmt.executeQuery();

      if (!result.next()) {
        return 0;
      }
      int count = result.getInt(1);

      if (result.next()) {
        System.err.println("Malformed result set from " + conn + " when counting query keys");
      }
      return count;
    } catch (SQLException e) {
      // FIXME: This is ugly, but OTOH size() should not cascade up exceptions...
      e.printStackTrace();
      System.err.println("Error counting queries from " + conn);
      return 0;
    }
  }

  public Iterator<SuggestionList> iterator() {
    throw new UnsupportedOperationException("iterator() not implemented");
  }
}
