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
  private final int serverId;
  private final Log lastLog;
  
  /**
   * Constructor
   *
   * @param lastLog the last log of the sender
   */
  public LogsRequestMessage(int serverId, Log lastLog) {
    this.serverId = serverId;
    this.lastLog = lastLog;
  }

  /** Send the missing logs in multicast */
  @Override
  public void execute(Server server) {

    // ignore if it's your request
    if (this.serverId == server.getServerId()) {
      return;
    }

    try {
      // get the missing logs from db
      List<Log> missingLogs = server.getDb().getFollowingLogs(this.lastLog);
      // send the missing logs
      System.out.println("Sending missing logs");
      for (Log missingLog : missingLogs) {
        server.sendMulticastMessage(new AppendLogMessage(missingLog));
      }
    } catch (SQLException | InvalidDimensionException | InvalidInitValuesException e) {
      System.out.println(e);
    }
  }
}
