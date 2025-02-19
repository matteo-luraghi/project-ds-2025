package it.polimi.ds.message;

import it.polimi.ds.server.ServerHandler;
import java.io.Serializable;

public abstract class ServerMessage implements Serializable {

  public void process(ServerHandler serverHandler) {}
}
