package it.polimi.ds.message;

import it.polimi.ds.model.Log;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.model.exception.InvalidInitValuesException;

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
  public AppendLogMessage(Log log) throws InvalidDimensionException, InvalidInitValuesException {
    this.log =
        new Log(
            log.getVectorClock(), log.getServerId(),
            log.getWriteKey(), log.getWriteValue());
  }

  @Override
  public void execute() {
    // TODO: probably need to pass the server in this function to then save the
    // log in local db and execute the write query
    System.out.println("Log needs to be appended");
  }
}
