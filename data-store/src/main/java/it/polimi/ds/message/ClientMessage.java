package it.polimi.ds.message;

import it.polimi.ds.server.ClientHandler;
import java.io.Serializable;

public abstract class ClientMessage implements Serializable {

  public void process(ClientHandler clientHandler) {}
}
