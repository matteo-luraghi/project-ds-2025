package it.polimi.ds.database;

import it.polimi.ds.model.Log;
import it.polimi.ds.model.TimeVector;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.model.exception.InvalidInitValuesException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Database
 *
 * <p>handles sqlite database initialization, writes and reads
 */
public class Database {
  private final Connection conn;

  /**
   * Connects (or creates) to a sqlite database and creates the data_store table and the log table
   * if not already present
   *
   * @params serverId server identifier used to name the .db file
   * @throws SQLException
   */
  public Database(String serverName) throws SQLException {
    String dbPath = "storage/" + serverName + ".db";
    this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    Statement statement = this.conn.createStatement();
    statement.execute(
        "CREATE TABLE IF NOT EXISTS data_store (key TEXT " + "PRIMARY KEY, value TEXT)");
    statement.execute(
        "CREATE TABLE IF NOT EXISTS log (vector_clock TEXT, server_id INT, "
            + "write_key TEXT, write_value TEXT,  "
            + "PRIMARY KEY (vector_clock, server_id))");
  }

  /** Removes all the rows from both the tables in the db */
  public void resetDatabase() throws SQLException {
    Statement statement = this.conn.createStatement();
    statement.execute("DELETE FROM data_store");
    statement.execute("DELETE FROM log;");
  }

  /**
   * Writes the value relative to a key in the db
   *
   * @params key the key to
   * @params value the value to write
   * @throws SQLException
   */
  public void insertValue(String key, String value) throws SQLException {
    // REPLACE works as INSERT but if the key is already present overwrites the
    // value
    String query = "REPLACE INTO data_store VALUES (?, ?)";
    try (PreparedStatement pstatement = this.conn.prepareStatement(query)) {
      pstatement.setString(1, key);
      pstatement.setString(2, value);
      pstatement.executeUpdate();
    }
  }

  /**
   * Reads the value relative to a key from the db
   *
   * @params key the key to read
   * @throws SQLException
   */
  public String readValue(String key) throws SQLException {
    String query = "SELECT value FROM data_store WHERE key = ?";
    try (PreparedStatement pstatement = this.conn.prepareStatement(query)) {
      pstatement.setString(1, key);

      try (ResultSet res = pstatement.executeQuery()) {
        if (!res.isBeforeFirst()) return null;
        else {
          res.next();
          String value = res.getString("value");
          return value;
        }
      }
    }
  }

  /**
   * Writes the log to the db
   *
   * @params log the log to write
   * @throws SQLException
   * @throws InvalidDimensionException
   * @throws InvalidInitValuesException
   */
  public void insertLog(Log log) throws SQLException {
    String query = "INSERT INTO log VALUES (?, ?, ?, ?)";

    TimeVector vectorClock = log.getVectorClock();

    // save the vector clock as a string like [0, 1, 2, 3]
    try (PreparedStatement pstatement = this.conn.prepareStatement(query)) {
      pstatement.setString(1, Arrays.toString(vectorClock.getVector()));
      pstatement.setString(2, Integer.toString(log.getServerId()));
      pstatement.setString(3, log.getWriteKey());
      pstatement.setString(4, log.getWriteValue());
      pstatement.executeUpdate();
    }
  }

  /**
   * Extract the last log from the db
   *
   * @throws SQLException
   */
  public Log getLastLog()
      throws SQLException,
          NumberFormatException,
          InvalidDimensionException,
          InvalidInitValuesException {
    String query = "SELECT * FROM log ORDER BY rowid DESC LIMIT 1";
    Statement statement = conn.createStatement();
    try (ResultSet res = statement.executeQuery(query)) {
      if (!res.isBeforeFirst()) return null;
      else {
        res.next();
        TimeVector vectorClock = this.parseTimeVector(res.getString("vector_clock"));
        int serverId = Integer.parseInt(res.getString("server_id"));
        String writeKey = res.getString("write_key");
        String writeValue = res.getString("write_value");
        return new Log(vectorClock, serverId, writeKey, writeValue);
      }
    }
  }

  /**
   * Writes the log and the (key, value) pair in a single transaction, rollbacks if either one fails
   *
   * @param log the Log of the write to be performed
   */
  public void executeTransactionalWrite(Log log) throws SQLException {
    this.conn.setAutoCommit(false);
    try {
      this.insertLog(log);
      this.insertValue(log.getWriteKey(), log.getWriteValue());
      this.conn.commit();
    } catch (SQLException e) {
      this.conn.rollback();
      this.conn.setAutoCommit(true);
      throw e;
    }
    this.conn.setAutoCommit(true);
  }

  /**
   * Returns all the logs following a given log
   *
   * @param log the given log
   */
  public List<Log> getFollowingLogs(Log log)
      throws SQLException, InvalidDimensionException, InvalidInitValuesException {
    List<Log> followingLogs = new ArrayList<>();

    // query to get all following logs given a log
    String query =
        "WITH given(v) AS (VALUES (?))"
            + " SELECT * FROM log, given"
            + " WHERE NOT EXISTS ("
            + " SELECT 1 FROM json_each(given.v) g"
            + " JOIN json_each(log.vector_clock) t ON g.key = t.key"
            // no smaller element in vector
            + " WHERE CAST(t.value AS INT) < CAST(g.value AS INT)"
            + " ) AND EXISTS ("
            + " SELECT 1 FROM json_each(given.v) g"
            + " JOIN json_each(log.vector_clock) t ON g.key = t.key"
            // at least one greater element in vector
            + " WHERE CAST(t.value AS INT) > CAST(g.value AS INT)"
            + " );";

    try (PreparedStatement pstatement = this.conn.prepareStatement(query)) {
      pstatement.setString(1, Arrays.toString(log.getVectorClock().getVector()));

      try (ResultSet res = pstatement.executeQuery()) {
        if (!res.isBeforeFirst()) return null;
        else {
          // cycle all results and add the logs to the ArrayList
          while (res.next()) {
            TimeVector vectorClock = this.parseTimeVector(res.getString("vector_clock"));
            int serverId = Integer.parseInt(res.getString("server_id"));
            String writeKey = res.getString("write_key");
            String writeValue = res.getString("write_value");
            followingLogs.add(new Log(vectorClock, serverId, writeKey, writeValue));
          }
        }
      }
    }

    return followingLogs;
  }

  /**
   * Parses the time vector from the db
   *
   * @param timeVectorStr the string to parse as a TimeVector
   */
  private TimeVector parseTimeVector(String timeVectorStr)
      throws NumberFormatException, InvalidDimensionException, InvalidInitValuesException {
    String[] vectorArrayStr = timeVectorStr.substring(1, timeVectorStr.length() - 1).split(", ");
    int dimension = vectorArrayStr.length;
    int[] vectorArray = new int[dimension];
    for (int i = 0; i < dimension; i++) {
      vectorArray[i] = Integer.parseInt(vectorArrayStr[i]);
    }
    return new TimeVector(dimension, vectorArray);
  }

  /**
   * Close che connection with the db
   *
   * @throws SQLException
   */
  public void close() throws SQLException {
    if (this.conn != null) {
      this.conn.close();
    }
  }
}
