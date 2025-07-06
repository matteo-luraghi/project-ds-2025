package it.polimi.ds.message;

import it.polimi.ds.server.ClientHandler;

/**
 * ReadRequest
 *
 * <p>requests the server to read the value given a key
 */
public class ReadRequest extends ClientMessage {
  private final String key;

  /**
   * Constructor
   *
   * @param key the key of the item to read from
   */
  public ReadRequest(String key) {
    this.key = key;
  }

  public void execute(ClientHandler clientHandler) {
    System.out.println("User asked to read: " + key);
    System.out.println("");
  }
}
