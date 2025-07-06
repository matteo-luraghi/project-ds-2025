package it.polimi.ds.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
  private int id;
  private ServerSocket serverSocket;
  private ServerSocket networkSocket;
  private ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
  private HashMap<Integer, ServerHandler> serverHandlers = new HashMap<>();
  private int serverPort = 1234;
  private int networkPort = 4321;
  private ServerRole role;
  private final ExecutorService executor;

  Server() {
    this.executor = Executors.newCachedThreadPool();
  }

  /** Start the server by opening the sockets and accepting sockets connections */
  public void start() {

    try {
      String serverIP = InetAddress.getLocalHost().getHostAddress();
      this.serverSocket = new ServerSocket(this.serverPort);
      this.networkSocket = new ServerSocket(this.networkPort);
      System.out.println("Server running at: " + serverIP);
      System.out.println("Port for clients connections: 1234");
      System.out.println("Port for servers connections: 4321");
    } catch (IOException e) {
      System.out.println("Error setting up server");
    }

    while (true) {
      try {
        Socket clientSocket = this.serverSocket.accept();
        clientSocket.setSoTimeout(10000);
        ClientHandler clientHandler = new ClientHandler(clientSocket);
        clientHandlers.add(clientHandler);

        // start the connection handler thread
        executor.submit(clientHandler);
      } catch (IOException e) {
        System.out.println("Error connecting to the client");
      }
    }
  }
}
