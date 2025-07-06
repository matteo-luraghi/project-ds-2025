package it.polimi.ds.message;

import it.polimi.ds.server.ClientHandler;

public class ReadRequest extends ClientMessage {
  private final String key;

  public ReadRequest(String key) {
    this.key = key;
  }

  public void execute(ClientHandler clientHandler) {
    System.out.println("User asked to read: " + key);
    System.out.println("");
  }
}
