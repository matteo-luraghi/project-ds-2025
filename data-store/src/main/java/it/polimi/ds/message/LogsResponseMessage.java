package it.polimi.ds.message;

import it.polimi.ds.model.Log;
import it.polimi.ds.model.TimeVector;
import it.polimi.ds.model.exception.ImpossibleComparisonException;
import it.polimi.ds.server.Server;
import java.sql.SQLException;
import java.util.List;

/**
 * LogsRequestMessage
 *
 * <p>used to send the missing logs to a server
 */
public class LogsResponseMessage extends ServerToServerMessage {
  private final int serverId;
  private final List<Log> missingLogs;

  /**
   * Constructor
   *
   * @param serverId the id of the sender
   * @param missingLogs the list of logs to append to the receiver
   */
  public LogsResponseMessage(int serverId, List<Log> missingLogs) {
    this.serverId = serverId;
    this.missingLogs = missingLogs;
  }

  /** Save all logs in db and execute all writes in db */
  @Override
  public void execute(Server server) {
    System.out.println("Missing logs received:");
    for (Log log : missingLogs) {
      try {
        TimeVector msgVC = log.getVectorClock();
        int senderId = log.getServerId();
        TimeVector vectorClock = server.getTimeVector();

        System.out.println(
            "- Updates received:"
                + log.getServerId()
                + ","
                + "("
                + log.getWriteKey()
                + ","
                + log.getWriteValue()
                + ")");

        synchronized (vectorClock) {
          if (msgVC.happensBefore(vectorClock, senderId)) {
            server.executeWrite(log);
          } else {
            server.addToUpdatesBuffer(log);
          }
        }

      } catch (ImpossibleComparisonException e) {
        System.out.println(e.getMessage());
        System.out.println("Ignoring received log");
      } catch (SQLException e) {
        System.out.println("Error in database while writing missing logs");
      }
    }
  }
}
