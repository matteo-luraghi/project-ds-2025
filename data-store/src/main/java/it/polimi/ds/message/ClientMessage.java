package it.polimi.ds.message;

import it.polimi.ds.server.ClientHandler;
import java.io.Serializable;

/**
 * ClientMessage
 *
 * <p>messages sent from the client to the server
 */
public abstract class ClientMessage implements Serializable {

  /**
   * Execute the client request on the server
   *
   * @param clientHandler
   */
  public void execute(ClientHandler clientHandler) {}
}
