package it.polimi.ds.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {
  private int id;
  private ServerSocket serverSocket;
  private ServerSocket networkSocket;
  private ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
  private HashMap<Integer, ServerHandler> serverHandlers = new HashMap<>();
  private int serverPort = 4321;
  private int networkPort = 1234;
  private ServerRole role;

  Server() {
    try {
      this.serverSocket = new ServerSocket(this.serverPort);
      this.networkSocket = new ServerSocket(this.networkPort);
    } catch (IOException ignored) {
    }
  }

  public void handleClientConnection() {
    while (true) {
      try {
        Socket client = this.serverSocket.accept();
        ClientHandler clientHandler = new ClientHandler(client);
        clientHandlers.add(clientHandler);

        new Thread(clientHandler, "Connected to:" + client.getInetAddress()).start();;
      } catch (IOException e) {
        System.out.println("Connection dropped");
      }
    }
  }
}
