package it.polimi.ds.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database
 *
 * <p>handles sqlite database initialization, writes and reads
 */
public class Database {
  private final Connection conn;

  /**
   * Connects (or creates) to a sqlite database and creates the data_store table if not already
   * present
   *
   * @params serverId server identifier used to name the .db file
   */
  public Database(String serverId) throws SQLException {
    String dbPath = "storage/" + serverId + ".db";
    this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    Statement statement = conn.createStatement();
    statement.execute(
        "CREATE TABLE IF NOT EXISTS data_store (key TEXT " + "PRIMARY KEY, value TEXT)");
  }

  /**
   * Writes the value relative to a key in the db
   *
   * @params key the key to
   * @params value the value to write
   */
  public void insertValue(String key, String value) throws SQLException {
    String query = "INSERT INTO data_store VALUES (?, ?)";
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
}
