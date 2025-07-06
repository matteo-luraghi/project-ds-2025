package it.polimi.ds.message;

import it.polimi.ds.server.ClientHandler;

public class WriteRequest extends ClientMessage {
  private final String key;
  private final String value;

  public WriteRequest(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public void execute(ClientHandler clientHandler) {
    System.out.println("User asked to write value:" + value + ", with key: " + key);
    System.out.println("");
  }
}
