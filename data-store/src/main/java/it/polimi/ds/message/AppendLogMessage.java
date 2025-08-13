package it.polimi.ds.message;

import java.sql.SQLException;
import java.util.Vector;
import it.polimi.ds.database.Database;
import it.polimi.ds.model.Log;
import it.polimi.ds.model.TimeVector;
import it.polimi.ds.model.exception.ImpossibleComparisonException;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.model.exception.InvalidInitValuesException;
import it.polimi.ds.server.Server;

/**
 * AppendLogMessage
 *
 * <p>used to notify all the other servers of a write operation using logs
 */
public class AppendLogMessage extends ServerToServerMessage {
  private final Log log;

  /**
   * Constructor
   *
   * @param log the log to save and execute
   */
  public AppendLogMessage(Log log){
    this.log =
        new Log(
            log.getVectorClock(), log.getServerId(),
            log.getWriteKey(), log.getWriteValue());
  }

  /**
   *  Confronts the log's vector clock with the server's vector clock if the first happens before 
   *  the second the write is performed on the database otherwhise the log is inserted in the buffer
   */
  @Override
  public void execute(Server server) {

    // message that come back to the sending server are ignored
    if (this.log.getServerId() == server.getServerId()) {
      return;
    }

    // log in local db and execute the write query
    try {
      TimeVector msgVC = log.getVectorClock();
      int senderId= log.getServerId();
      TimeVector vectorClock = server.getTimeVector();
      synchronized(vectorClock){
        if(msgVC.happensBefore(vectorClock, senderId)){
          server.executeWrite(log);
        }
        else{
          server.addToUpdatesBuffer(log);
        }
      }
      System.out.println("Updates received:"+log.getServerId()+","+"("+log.getWriteKey()+","+log.getWriteValue()+")");

    } catch (ImpossibleComparisonException e) {
      System.out.println(e.getMessage());
    } catch (SQLException e) {
      System.out.println("Error in database while executing AppendLog Message");
    }
  }
}
