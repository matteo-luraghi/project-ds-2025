package it.polimi.ds.message;

import it.polimi.ds.server.ClientHandler;
import java.sql.SQLException;

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

  /** Read the value associated with the key from the db and show it to the user */
  public void execute(ClientHandler clientHandler) {
    try {
      String value = clientHandler.db.readValue(this.key);
      String message =
          (value != null)
              ? "\nKey: " + this.key + "\nValue: " + value + "\n"
              : "\nNo value for key: " + this.key + "\n";
      clientHandler.sendMessageClient(new ServerToClientResponseMessage(message));
    } catch (SQLException e) {
      clientHandler.sendMessageClient(
          new ServerToClientResponseMessage("Error reading key: " + this.key));
    }
  }
}
