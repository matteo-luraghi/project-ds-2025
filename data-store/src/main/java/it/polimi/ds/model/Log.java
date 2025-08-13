package it.polimi.ds.model;

import it.polimi.ds.model.exception.ImpossibleComparisonException;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.model.exception.InvalidInitValuesException;
import java.io.Serializable;
import java.util.Comparator;

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
  public Log(TimeVector vectorClock, int serverId, String writeKey, String writeValue){
    this.vectorClock = TimeVector.copyTimeVector(vectorClock);
    this.serverId = serverId;
    this.writeKey = writeKey;
    this.writeValue = writeValue;
  }

  /** vectorClock getter */
  public TimeVector getVectorClock() {
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
  /**
   * Class that impose a total order in a collection of Log
   */
  public static class LogComparator implements Comparator<Log> {
    /** 
     * @return a negative integer, or a positive integer as 
     * the first argument happens before, or happens after than the second.
     * Server id is used as a tie breaker
    */
    public int compare(Log log1,Log log2){
      TimeVector vc1= log1.vectorClock;
      TimeVector vc2= log2.vectorClock;

      try {
        if(vc1.happensBefore(vc2)){
          return -1;
        }else if(vc2.happensBefore(vc1)){
          return 1;
        }else{ //ties breaker (causal order is only partial but compare method needs a total order)
          return Integer.compare(log1.serverId,log2.serverId);
        }
      } catch (ImpossibleComparisonException e) {
        System.out.println("While updates' buffer sorting:");
        System.out.println(e.getMessage());
        System.exit(1);
      }
      return 0; //useless but Java is petty
     
    }
  }
  
}
