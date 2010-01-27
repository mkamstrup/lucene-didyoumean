package org.apache.lucene.search.didyoumean.dictionary;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * FIXME: Missing class docs for org.apache.lucene.search.didyoumean.dictionary.H2Dictionary
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Jan 27, 2010
 */
public class H2Dictionary extends JDBCDictionary {

  public H2Dictionary(File baseDir) throws SQLException, IOException {
    super(createConnection(baseDir));
  }

  /**
   * Create a new connection to a H2 database with the URL
   * {@code "jdbc:h2:" + baseDir + File.separator + "didyoumean"}
   * @param baseDir the base directory the database files should be in
   * @return a new connection to the database
   * @throws SQLException if unable to connecto the database or the H2 JDBC driver wasn't found
   * @throws IOException if {@code baseDir} is a regular file
   */
  public static Connection createConnection(File baseDir) throws SQLException, IOException {
    if (baseDir.isFile()) {
      throw new IOException(baseDir + " is a regular file. Expected a directory");
    }
    String jdbcUrl = "jdbc:h2:" + baseDir + File.separator + "didyoumean";

    // Initialize the H2 JDBC driver (required for Java <= 5)
    try {
      Class.forName("org.h2.Driver");
    } catch (ClassNotFoundException e) {
      throw new SQLException("No such JDBC driver 'org.h2.Driver'", e);
    }

    return DriverManager.getConnection(jdbcUrl);
  }

  @Override
  protected void createTables() throws SQLException {
    Statement stmt = conn.createStatement();
    stmt.execute("CREATE TABLE IF NOT EXISTS dict (" +
                 "  PRIMARY KEY(queryKeyId,suggId)," +
                 "  queryKeyId INTEGER AUTO_INCREMENT," +
                 "  suggId INTEGER" +
                 ")");
    stmt.execute("CREATE TABLE IF NOT EXISTS sugg (" +
                 "  PRIMARY KEY(id)," +
                 "  id INTEGER AUTO_INCREMENT," +
                 "  string VARCHAR(1024)," +
                 "  score DOUBLE," +
                 "  results INTEGER" +
                 ")");
    stmt.execute("CREATE TABLE IF NOT EXISTS query (" +
                 "  PRIMARY KEY(keyId)," +
                 "  UNIQUE(key)," +
                 "  keyId INTEGER AUTO_INCREMENT," +
                 "  key VARCHAR(1024)" +
                 ")");
    stmt.execute("CREATE UNIQUE INDEX suggString ON sugg(string)");
    conn.commit();
    stmt.close();
  }
}
