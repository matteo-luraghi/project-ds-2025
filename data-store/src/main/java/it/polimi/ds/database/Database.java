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
import java.util.List;

/**
 * Database
 *
 * <p>
 * handles sqlite database initialization, writes and reads
 */
public class Database {
  private final Connection conn;

  /**
   * Connects (or creates) to a sqlite database and creates the data_store table
   * and the log table
   * if not already present
   *
   * @params serverId server identifier used to name the .db file
   * @throws SQLException
   */
  public Database(String serverName) throws SQLException {
    String dbPath = "storage/" + serverName + ".db";
    this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    Statement statement = conn.createStatement();
    statement.execute(
        "CREATE TABLE IF NOT EXISTS data_store (key TEXT " + "PRIMARY KEY, value TEXT)");
    statement.execute(
        "CREATE TABLE IF NOT EXISTS log (vector_clock TEXT, server_id INT, "
            + "write_key TEXT, write_value TEXT,  "
            + "PRIMARY KEY (vector_clock, server_id))");
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
        if (!res.isBeforeFirst())
          return null;
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

    // save the vector clock as a string where values are separated by ";"
    String vectorClockString = "";
    for (int i = 0; i < vectorClock.getDimension(); i++) {
      if (i != vectorClock.getDimension() - 1) {
        vectorClockString += vectorClock.getVector()[i] + ";";
      } else {
        vectorClockString += vectorClock.getVector()[i];
      }
    }

    try (PreparedStatement pstatement = this.conn.prepareStatement(query)) {
      pstatement.setString(1, vectorClockString);
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
      if (!res.isBeforeFirst())
        return null;
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
   * Writes the log and the (key, value) pair in a single transaction, rollbacks
   * if either one fails
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

    String query = "SELECT * FROM log";
    try (PreparedStatement pstatement = this.conn.prepareStatement(query)) {
      pstatement.setString(1, Integer.toString(rowid));
      try (ResultSet res = pstatement.executeQuery(query)) {
        if (!res.isBeforeFirst())
          return null;
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
    String[] vectorArrayStr = timeVectorStr.split(";");
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
