package it.polimi.ds.message;

import java.io.Serializable;

/**
 * ServerToClientMessage
 *
 * <p>used to communicate from server to client
 */
public abstract class ServerToClientMessage implements Serializable {

  /** Show the result of the processing back to the client */
  public abstract void show();
}
