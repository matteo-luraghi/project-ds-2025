package it.polimi.ds.model;

import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.model.exception.InvalidInitValuesException;
import java.io.Serializable;

/**
 * Log
 *
 * <p>saves the write operation performed by a specific server with a specific vector clock
 */
public class Log implements Serializable {
  private final TimeVector vectorClock;
  private final int serverId;
  private final String writeKey;
  private final String writeValue;

  /**
   * Constructor
   *
   * @params vectorClock the vector clock of the server performing the write
   * @params serverId the id of the server performing the write
   * @params writeKey the key written in the db
   * @params writeValue the value written in the db
   */
  public Log(TimeVector vectorClock, int serverId, String writeKey, String writeValue)
      throws InvalidDimensionException, InvalidInitValuesException {
    this.vectorClock = TimeVector.copyTimeVector(vectorClock);
    this.serverId = serverId;
    this.writeKey = writeKey;
    this.writeValue = writeValue;
  }

  /** vectorClock getter */
  public TimeVector getVectorClock() throws InvalidDimensionException, InvalidInitValuesException {
    return TimeVector.copyTimeVector(this.vectorClock);
  }

  /** serverId getter */
  public int getServerId() {
    return this.serverId;
  }

  /** writeKey getter */
  public String getWriteKey() {
    return new String(this.writeKey);
  }

  /** writeValue getter */
  public String getWriteValue() {
    return new String(this.writeValue);
  }
}
