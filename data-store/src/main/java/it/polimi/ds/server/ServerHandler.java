package it.polimi.ds.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import it.polimi.ds.message.ServerMessage;

public class ServerHandler implements Runnable {
  private final Socket otherServer;
  private ObjectOutputStream output;
  private ObjectInputStream input;

  ServerHandler(Socket otherServer) { this.otherServer = otherServer; }

  @Override
  public void run() {
    try {
      this.output = new ObjectOutputStream(otherServer.getOutputStream());
      this.input = new ObjectInputStream(otherServer.getInputStream());

    } catch (IOException e) {
      //TODO: manage server crash
    }

    try {
      while (true) {
        Object message = input.readObject();
        ServerMessage cmd = (ServerMessage)message;
        cmd.process(this);
      }
    } catch (ClassNotFoundException e) {
      System.err.println("Invalid stream");
      System.exit(1);
    } catch (IOException e) {
          // TODO handle server disconnection
        }
  }

  public void asnwerServer(ServerMessage msg) throws IOException {
    this.output.writeObject(msg);
  }
}
