package it.polimi.ds.message;

import it.polimi.ds.server.Server;
import java.io.Serializable;

/**
 * ServerToServerMessage
 *
 * <p>used to communicate between servers
 */
public abstract class ServerToServerMessage implements Serializable {

  /**
   * Executes the message action on the receiving server
   *
   * @param serverHandler
   */
  public abstract void execute(Server server);
}
