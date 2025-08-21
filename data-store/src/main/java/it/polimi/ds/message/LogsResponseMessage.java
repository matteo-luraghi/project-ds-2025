package it.polimi.ds.message;

import it.polimi.ds.model.Log;
import it.polimi.ds.model.TimeVector;
import it.polimi.ds.model.exception.ImpossibleComparisonException;
import it.polimi.ds.server.Server;
import java.sql.SQLException;

/**
 * LogsRequestMessage
 *
 * <p>used to send the missing logs to a server
 */
public class LogsResponseMessage extends ServerToServerMessage {
  private final Log missingLog;

  /**
   * Constructor
   *
   * @param missingLog the list of logs to append to the receiver
   */
  public LogsResponseMessage(Log missingLog) {
    this.missingLog = missingLog;
  }

  /** Save and execute the missing log */
  @Override
  public void execute(Server server) {
    try {
      TimeVector msgVC = this.missingLog.getVectorClock();
      int senderId = this.missingLog.getServerId();
      TimeVector vectorClock = server.getTimeVector();

      System.out.println(
          "- Updates received:"
              + this.missingLog.getServerId()
              + ","
              + "("
              + this.missingLog.getWriteKey()
              + ","
              + this.missingLog.getWriteValue()
              + ")");

      synchronized (vectorClock) {
        if (msgVC.happensBefore(vectorClock, senderId)) {
          server.executeWrite(this.missingLog);
        } else {
          server.addToUpdatesBuffer(this.missingLog);
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
