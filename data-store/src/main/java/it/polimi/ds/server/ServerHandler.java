package it.polimi.ds.server;

import it.polimi.ds.message.ServerToServerMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

// FIX: probably need some sort of multicast, not sockets to communicate
// bewtween different servers

public class ServerHandler implements Runnable {
  private final Socket otherServer;
  private ObjectOutputStream output;
  private ObjectInputStream input;

  ServerHandler(Socket otherServer) {
    this.otherServer = otherServer;
  }

  @Override
  public void run() {
    try {
      this.output = new ObjectOutputStream(otherServer.getOutputStream());
      this.input = new ObjectInputStream(otherServer.getInputStream());

    } catch (IOException e) {
      // TODO: manage server crash
    }

    try {
      while (true) {
        Object message = input.readObject();
        ServerToServerMessage msg = (ServerToServerMessage) message;
        msg.execute(this);
      }
    } catch (ClassNotFoundException e) {
      System.err.println("Invalid stream");
      System.exit(1);
    } catch (IOException e) {
      // TODO handle server disconnection
    }
  }

  public void asnwerServer(ServerToServerMessage msg) throws IOException {
    this.output.writeObject(msg);
  }
}
