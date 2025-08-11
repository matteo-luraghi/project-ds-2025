package it.polimi.ds.server;

import it.polimi.ds.message.ClientMessage;
import it.polimi.ds.message.Ping;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connectionhandler
 *
 * <p>used to handle the connection with the client from the server
 */
public class ClientHandler implements Runnable {
  private final Server server;
  private final Socket socket;
  private final Thread pingThread;
  private final AtomicBoolean active = new AtomicBoolean(false);
  private ObjectOutputStream outputStream;
  private ObjectInputStream inputStream;

  /**
   * Constructor that initializes a ping thread
   *
   * @param server the handler's server
   * @param socket the socket
   */
  public ClientHandler(Server server, Socket socket) {
    this.server = server;
    this.socket = socket;
    this.pingThread =
        new Thread(
            () -> {
              while (this.active.get()) {
                try {
                  Thread.sleep(5000);
                  sendMessageClient(new Ping());
                } catch (InterruptedException e) {
                  break;
                }
              }
            });
  }

  /**
   * Start the connection handler by getting the input and output streams of the socket connection,
   * starting the pingThread, sending the first message and start listening for new messages from
   * the server
   */
  @Override
  public void run() {
    try {
      this.outputStream = new ObjectOutputStream(socket.getOutputStream());
      this.inputStream = new ObjectInputStream(socket.getInputStream());
      this.active.set(true);

      // start the ping thread to keep the socket connection alive
      this.pingThread.start();

      while (this.active.get()) {
        try {
          Object object = this.inputStream.readObject();
          if (!(object instanceof Ping)) {
            ClientMessage msg = (ClientMessage) object;
            // execute the client request
            msg.execute(this);
          }
        } catch (ClassNotFoundException | SocketTimeoutException e) {
          disconnect();
        }
      }

    } catch (IOException e) {
      System.err.println("Client disconnected");
      disconnect();
    }
  }

  /**
   * Send a message to the client
   *
   * @param msg message to be sent
   */
  public void sendMessageClient(Serializable msg) {
    try {
      this.outputStream.writeObject(msg);
      this.outputStream.flush();
      this.outputStream.reset();
    } catch (IOException e) {
      disconnect();
    }
  }

  /** Disconnects the handler closing the input and output stream and socket */
  public void disconnect() {
    if (this.active.get()) {
      this.active.set(false);
      try {
        this.inputStream.close();
      } catch (IOException ignored) {
      }
      try {
        this.outputStream.close();
      } catch (IOException ignored) {
      }
      try {
        this.socket.close();
      } catch (IOException ignored) {
      }
    }
  }

  /** server getter */
  public Server getServer() {
    return this.server;
  }
}
