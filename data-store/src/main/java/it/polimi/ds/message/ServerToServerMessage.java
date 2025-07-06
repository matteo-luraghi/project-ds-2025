package it.polimi.ds.message;

import java.io.Serializable;

import it.polimi.ds.server.ServerHandler;

public abstract class ServerToServerMessage implements Serializable {

  /**
   * Executes the message
   * @param serverHandler
   */
  public abstract void execute(ServerHandler serverHandler);

}
