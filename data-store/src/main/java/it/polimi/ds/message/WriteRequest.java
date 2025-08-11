package it.polimi.ds.message;

import it.polimi.ds.server.ClientHandler;
import java.sql.SQLException;

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

  // TODO: add multicast sending of the write
  public void execute(ClientHandler clientHandler) {
    try {
      clientHandler.getServer().getDb().insertValue(this.key, this.value);
      clientHandler.sendMessageClient(
          new ServerToClientResponseMessage(
              "\nSuccessfully inserted pair (" + this.key + ", " + this.value + ")\n"));
    } catch (SQLException e) {
      clientHandler.sendMessageClient(
          new ServerToClientResponseMessage(
              "\nError inserting pair: (" + this.key + ", " + this.value + ")\n"));
    }
  }
}
