package it.polimi.ds.message;

import it.polimi.ds.model.Log;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.model.exception.InvalidInitValuesException;
import it.polimi.ds.server.Server;
import java.sql.SQLException;
import java.util.List;

/**
 * LogsRequestMessage
 *
 * <p>used to ask another server for missing logs
 */
public class LogsRequestMessage extends ServerToServerMessage {
  private final Log lastLog;

  /**
   * Constructor
   *
   * @param lastLog the last log of the sender
   */
  public LogsRequestMessage(Log lastLog) {
    this.lastLog = lastLog;
  }

  /** Gets the missing logs from db and sends them back to the requesting server */
  @Override
  public void execute(Server server) {
    try {
      // get the missing logs from db
      List<Log> missingLogs = server.getDb().getFollowingLogs(this.lastLog);
      // send the missing logs
      server.sendMulticastMessage(new LogsResponseMessage(missingLogs));
    } catch (SQLException | InvalidDimensionException | InvalidInitValuesException e) {
      System.out.println(e);
    }
  }
}
