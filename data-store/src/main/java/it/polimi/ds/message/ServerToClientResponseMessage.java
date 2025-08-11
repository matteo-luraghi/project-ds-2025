package it.polimi.ds.message;

/**
 * ServerToClientResponseMessage
 *
 * <p>message to response to the client's request
 */
public class ServerToClientResponseMessage extends ServerToClientMessage {
  private final String message;

  /**
   * Constructor
   *
   * @param message the message to send to the client
   */
  ServerToClientResponseMessage(String message) {
    this.message = message;
  }

  /** Show the message to the client */
  public void show() {
    System.out.println(this.message);
  }
}
