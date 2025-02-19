package it.polimi.ds.server

    import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import it.polimi.ds.message.ClientMessage;
import it.polimi.ds.message.ServerMessage;

public class ClientHandler implements Runnable {
  private final Socket client;
  private ObjectOutputStream output;
  private ObjectInputStream input;

  ClientHandler(Socket client) { this.client = client; }

  @Override
  public void run() {
    try {
      this.output = new ObjectOutputStream(client.getOutputStream());
      this.input = new ObjectInputStream(client.getInputStream());

    } catch (IOException e) {
      System.err.println("Couldn't connect to client");
    }

    try {
      while (true) {
        Object message = input.readObject();
        ClientMessage cmd = (ClientMessage)message;
        cmd.process(this);
      }
    } catch (ClassNotFoundException e) {
      System.err.println("Invalid stream");
      System.exit(1);
    }
  }

  public void asnwerClient(ServerMessage msg) throws IOException {
    this.output.writeObject(msg);
  }
}
