package it.polimi.ds.server;

import it.polimi.ds.database.Database;
import it.polimi.ds.message.AppendLogMessage;
import it.polimi.ds.message.ServerToServerMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server
 *
 * <p>handles clients' requests and connects to other servers to keep state coherent
 */
public class Server {
  private int id;
  private final Database db;
  private ServerSocket serverSocket;
  private MulticastSocket multicastSocket;
  private final String serverIP;
  private final int serverPort;
  private final int multicastPort = 5000;
  private InetAddress multicastGroup;
  private final String multicastAddress = "230.0.0.1";
  private final Thread clientsThread;
  private final Thread muticastReceiveThread;
  private final ExecutorService executor;

  /**
   * Constructor, initializes the database connection, the client socket and the multicast socket
   * for the communication with the other servers
   *
   * @param id the server id
   * @param serverPort the port where the server is running
   * @throws IOException
   */
  Server(int id, int serverPort) throws IOException {
    this.id = id;
    this.serverPort = serverPort;
    this.serverIP = InetAddress.getLocalHost().getHostAddress();

    // db setup, the name of the db file is serverIP:serverPort.db
    try {
      this.db = new Database(this.serverIP + ":" + Integer.toString(this.serverPort));
    } catch (SQLException e) {
      System.err.println(e);
      throw new IOException();
    }

    // threads setup
    this.executor = Executors.newCachedThreadPool();
    this.clientsThread = new Thread(this::acceptClients);
    this.muticastReceiveThread = new Thread(this::readMulticastMessages);
    // client socket setup
    this.clientSocketSetup();
    // multicast socket setup
    this.multicastSocketSetup();
  }

  /**
   * Setup the server socket for clients connections
   *
   * @throws IOException
   */
  private void clientSocketSetup() throws IOException {
    this.serverSocket = new ServerSocket(this.serverPort);
    System.out.println("Server running at: " + this.serverIP);
    System.out.println("Port for client connections: " + this.serverPort);
    System.out.println(
        "Multicast group info:\n   - Port: "
            + this.multicastPort
            + "\n   - IP: "
            + this.multicastAddress);
  }

  /**
   * Setup the multicast socket
   *
   * @throws IOException
   */
  private void multicastSocketSetup() throws IOException {
    this.multicastGroup = InetAddress.getByName(multicastAddress);
    NetworkInterface networkInterface =
        NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
    SocketAddress groupAddress = new InetSocketAddress(multicastGroup, this.multicastPort);

    this.multicastSocket = new MulticastSocket(this.multicastPort);
    this.multicastSocket.setReuseAddress(true);
    this.multicastSocket.setNetworkInterface(networkInterface);
    // join multicast group
    this.multicastSocket.joinGroup(groupAddress, networkInterface);
  }

  /**
   * Start the server by opening the socket, accepting sockets connections and joining the multicast
   * group
   *
   * @throws IOException
   */
  public void start() throws IOException {
    // start accepting clients connections
    this.clientsThread.start();

    // start receiving multicast messages
    this.muticastReceiveThread.start();
    // TODO: remove this, only for checking multicast working
    if (this.serverPort == 1234) {
      sendMulticastMessage(new AppendLogMessage());
    }
  }

  /** Thread that accepts connections from clients */
  private void acceptClients() {
    while (true) {
      try {
        Socket clientSocket = this.serverSocket.accept();
        clientSocket.setSoTimeout(10000);
        ClientHandler clientHandler = new ClientHandler(clientSocket, this.db);

        // start the client handler thread
        executor.submit(clientHandler);
      } catch (IOException e) {
        System.out.println("Error connecting to the client");
      }
    }
  }

  /** Thread that reads messages from the multicast socket */
  private void readMulticastMessages() {
    byte[] buffer = new byte[65535];
    while (true) {
      try {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        this.multicastSocket.receive(packet);

        ByteArrayInputStream bis =
            new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object msg = ois.readObject();

        // execute server to server message
        if (msg instanceof ServerToServerMessage) {
          ((ServerToServerMessage) msg).execute();
        }

      } catch (IOException | ClassNotFoundException e) {
        System.err.println(e.getMessage());
      }
    }
  }

  /**
   * Send a multicast message to all other servers
   *
   * @params msg the message to send
   */
  public void sendMulticastMessage(Serializable msg) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);

      oos.writeObject(msg);
      oos.flush();

      byte[] data = bos.toByteArray();
      DatagramPacket packet =
          new DatagramPacket(data, data.length, this.multicastGroup, this.multicastPort);
      this.multicastSocket.send(packet);

    } catch (IOException e) {
      System.err.println("Error sending message: " + e);
    }
  }
}
