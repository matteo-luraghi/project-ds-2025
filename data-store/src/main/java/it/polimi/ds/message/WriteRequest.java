package it.polimi.ds.message;

import it.polimi.ds.model.Log;
import it.polimi.ds.model.TimeVector;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.model.exception.InvalidInitValuesException;
import it.polimi.ds.server.ClientHandler;
import java.sql.SQLException;

/**
 * WriteRequest
 *
 * <p>requests the server to write a value given a key
 */
public class WriteRequest extends ClientMessage {
  private final String key;
  private final String value;

  /**
   * Constructor
   *
   * @param key the key of the item to save
   * @param value the value to save
   */
  public WriteRequest(String key, String value) {
    this.key = key;
    this.value = value;
  }

  /**
   * Increment the local time vector, save the log and the value in db, answer the client and
   * multicast the log
   *
   * @param clientHandler the one that handles the connection with the client server-side
   */
  public void execute(ClientHandler clientHandler) {
    int serverId = clientHandler.getServer().getServerId();
    TimeVector vectorClock= clientHandler.getServer().getTimeVector();
    Log log = new Log(clientHandler.getServer().getTimeVector(), serverId, this.key, this.value);

    synchronized(vectorClock){
      //TODO use server.executeWrite()
      // increment the time vector of the server processing the request
      vectorClock.increment(serverId);

      // TODO: add a commit/rollback setup to commit only after both insertLog and
      // insertValue worked

      // build the log based on the server informations
      try {
        // add the log to the server's logs
        clientHandler.getServer().getDb().insertLog(log);
        // insert the value in the db
        clientHandler.getServer().getDb().insertValue(this.key, this.value);
        
      } catch (SQLException e) {
        System.out.println(e);
        // send the error response back to the client
        clientHandler.sendMessageClient(
            new ServerToClientResponseMessage(
                "\nError inserting pair: (" + this.key + ", " + this.value + ")\n"));
        return;
      }
    }
    // send the success response back to the client
      clientHandler.sendMessageClient(
          new ServerToClientResponseMessage(
              "\nSuccessfully inserted pair (" + this.key + ", " + this.value + ")\n"));
      // send the AppendLogMessage in multicast to the other servers
      clientHandler.getServer().sendMulticastMessage(new AppendLogMessage(log));

    
  }
}
