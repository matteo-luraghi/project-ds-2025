package it.polimi.ds.message;

import it.polimi.ds.server.ClientHandler;

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

  public void execute(ClientHandler clientHandler) {
    System.out.println("User asked to write value:" + value + ", with key: " + key);
    System.out.println("");
  }
}
