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
  private final int lastLogRowId;

  /**
   * Constructor
   *
   * @param serverId the id of the receiver
   * @param lastLogRowId the last log rowid of the sender
   */
  public LogsRequestMessage(int serverId, int lastLogRowId) {
    this.serverId = serverId;
    this.lastLogRowId = lastLogRowId;
  }

  /** Gets the missing logs from db and sends them back to the requesting server */
  @Override
  public void execute(Server server) {
    // only the correct server answers
    if (server.getServerId() != this.serverId) {
      return;
    }

    try {
      // get the missing logs from db
      List<Log> missingLogs = server.getDb().getFollowingLogs(this.lastLogRowId);
      // send the missing logs
      server.sendMulticastMessage(new LogsResponseMessage(server.getServerId(), missingLogs));
    } catch (SQLException | InvalidDimensionException | InvalidInitValuesException e) {
      System.out.println(e);
    }
  }
}
