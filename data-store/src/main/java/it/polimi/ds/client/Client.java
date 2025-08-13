package it.polimi.ds.client;

import it.polimi.ds.message.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client
 *
 * <p>used to connect to servers to perform read/write operations
 */
public class Client {
  private final Socket clientSocket;
  private final ObjectInputStream inputStream;
  private final ObjectOutputStream outputStream;
  private final Thread messageReceiver;
  private final Thread pingThread;
  private final AtomicBoolean connected = new AtomicBoolean();

  /**
   * Constructor, builds the threads needed for messages
   *
   * @param ip the server's ip address
   * @param port the server's port of the connection
   * @throws IOException if the connection with the server fails
   */
  public Client(String ip, int port) throws IOException {
    this.messageReceiver = new Thread(this::readMessages);
    this.clientSocket = new Socket();
    this.clientSocket.connect(new InetSocketAddress(ip, port));
    this.outputStream = new ObjectOutputStream(this.clientSocket.getOutputStream());
    this.inputStream = new ObjectInputStream(this.clientSocket.getInputStream());
    this.connected.set(true);

    // start the message receiver thread
    messageReceiver.start();

    // sart the ping thread to keep the socket connection alive
    this.pingThread =
        new Thread(
            () -> {
              while (this.connected.get()) {
                try {
                  Thread.sleep(5000);
                  sendMessageServer(new Ping());
                } catch (InterruptedException e) {
                  // if the server is not online, disconnect the client
                  disconnect();
                }
              }
            });
    pingThread.start();
  }

  /**
   * Send a message to the server
   *
   * @param message the message to be sent
   */
  /*private*/ public void sendMessageServer(Serializable message) {
    if (this.connected.get()) {
      try {
        outputStream.writeObject(message);
        outputStream.flush();
        outputStream.reset();
      } catch (IOException e) {
        disconnect();
      }
    }
  }

  /** Read messages from server and display them */
  public void readMessages() {
    try {
      while (this.connected.get()) {
        Object msg = this.inputStream.readObject();
        if (msg instanceof ServerToClientMessage) {
          // view the message via the TUI
          ((ServerToClientMessage) msg).show();
        } else if (msg instanceof Disconnection) {
          disconnect();
        }
      }
    } catch (IOException | ClassNotFoundException e) {
      System.err.println("Error reading message:" + e);
      disconnect();
    }
  }

  /**
   * Function that will execute when a client start, asks for commands until the user exits the
   * application
   *
   * @param scanner
   */
  public void getUserCommands(Scanner scanner) {
    String command = "";
    String key = "";
    String value = "";
    do {
      System.out.println("Commands available:");
      System.out.println("- read");
      System.out.println("- write");
      System.out.println("- exit");
      System.out.println("Insert the action you want to perform:");
      command = scanner.nextLine();

      if (command.equalsIgnoreCase("read")) {
        System.out.println("Insert the key you want to read:");
        key = scanner.nextLine();
        value = "";
        // send the request to read the key value to the server
        sendMessageServer(new ReadRequest(key));
      } else if (command.equalsIgnoreCase("write")) {
        System.out.println("Insert the key you want to write to:");
        key = scanner.nextLine();
        System.out.println("Insert the value you want to write:");
        value = scanner.nextLine();
        // send the request to write the value to the server
        sendMessageServer(new WriteRequest(key, value));
      } else if (command.equalsIgnoreCase("exit")) {
        disconnect();
      } else {
        System.err.println("Insert a valid command!");
      }
    } while (!command.equals("exit") && this.connected.get());
  }

  /** Disconnect the client */
  public void disconnect() {
    if (this.connected.get()) {
      System.out.println("Disconnecting...");
      this.connected.set(false);
      if (this.messageReceiver.isAlive()) this.messageReceiver.interrupt();
      if (this.pingThread.isAlive()) this.pingThread.interrupt();
      try {
        this.inputStream.close();
        this.outputStream.close();
        this.clientSocket.close();
      } catch (IOException ignored) {
      }
    }
  }
}
