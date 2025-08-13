package it.polimi.ds.server;

import it.polimi.ds.database.Database;
import it.polimi.ds.message.ServerToServerMessage;
import it.polimi.ds.model.Log;
import it.polimi.ds.model.TimeVector;
import it.polimi.ds.model.exception.ImpossibleComparisonException;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.model.exception.InvalidInitValuesException;

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
import java.sql.Time;
import java.util.TreeSet;
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
  private final Thread multicastReceiveThread;
  private final Thread updateBufferThread;
  private final ExecutorService executor;
  private TimeVector timeVector = null;
  private TreeSet<Log> updatesBuffer= new TreeSet<Log>(new Log.LogComparator());

  /**
   * Constructor, initializes the database connection, the client socket and the multicast socket
   * for the communication with the other servers
   *
   * @param id the server id
   * @param serverPort the port where the server is running
   * @param serversNumber the number of the servers in the network
   * @throws IOException
   * @throws InvalidDimensionException
   */
  public Server(int id, int serverPort, int serversNumber) throws IOException, InvalidDimensionException {
    this.id = id;
    this.serverPort = serverPort;
    this.serverIP = InetAddress.getLocalHost().getHostAddress();

    // TODO: could be useful to check db before initializing the timeVector to
    // initialize it as the last log appended (for crash detection)

    // initialize the server's time vector with all zeros
    this.timeVector = new TimeVector(serversNumber);

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
    this.multicastReceiveThread = new Thread(this::readMulticastMessages);
    this.updateBufferThread = new Thread(this::manageUpdateBuffer);
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
    this.multicastReceiveThread.start();

    //start managing updates buffer
    this.updateBufferThread.start();
  }

  /** Thread that accepts connections from clients */
  private void acceptClients() {
    while (true) {
      try {
        Socket clientSocket = this.serverSocket.accept();
        clientSocket.setSoTimeout(10000);
        ClientHandler clientHandler = new ClientHandler(this, clientSocket);

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
        System.out.println("multicast message received");

        ByteArrayInputStream bis =
            new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object msg = ois.readObject();

        // execute server to server message
        if (msg instanceof ServerToServerMessage) {
          ((ServerToServerMessage) msg).execute(this);
        }

      } catch (IOException | ClassNotFoundException e) {
        System.err.println(e.getMessage());
      }
    }
  }
  /**
   * Waits until updatesBuffer is not empty, then for each log inside the buffer 
   *  if it happens before the current vector clock executes the writes on the database
   * and removes the log from the buffer
   */
    public void manageUpdateBuffer() {
      synchronized(timeVector){
        while(updatesBuffer.isEmpty()) {
          try {
            timeVector.wait();
          } catch (InterruptedException ignore) {}
       
          Log oldLog= updatesBuffer.first();
          for (Log log : updatesBuffer) {
            TimeVector logVC = log.getVectorClock();
            try {
              assert oldLog.equals(log) || oldLog.getVectorClock().happensBefore(logVC):"buffer is not sorted";
            } catch (ImpossibleComparisonException e) {}
            oldLog=log;
            try {
              if(logVC.happensBefore(this.timeVector)){
                executeWrite(log);
                updatesBuffer.remove(log);
              }
            } catch (ImpossibleComparisonException | SQLException e) {
              System.out.println("While managing updates buffer:");
              System.out.println(e.getMessage());
            }
          }
        }
      }

    }

  /**
   * Writes the log in the db
   * Perfrorms the write in the db
   * Merges the vector clock of the log with the server's one 
   * Awakes the buffer updates thread
   * @param log the log of the write to be performed  
   */
  public void executeWrite(Log log) throws SQLException, ImpossibleComparisonException {
    TimeVector logVC = log.getVectorClock();
    this.db.insertLog(log);
    this.db.insertValue(log.getWriteKey(), log.getWriteValue());
    synchronized(timeVector){
      this.timeVector.merge(logVC, this.id);
      this.timeVector.notify();
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
      System.out.println("multicast msg send");

    } catch (IOException e) {
      System.err.println("Error sending message: " + e);
    }
  }

  /** id getter */
  public int getServerId() {
    return this.id;
  }

  /** db getter */
  public Database getDb() {
    return this.db;
  }

  /** timeVector getter */
  public TimeVector getTimeVector() {
    return this.timeVector;
  }

  public void addToUpdatesBuffer(Log log) {
    synchronized(timeVector){ 
      updatesBuffer.add(log);
      timeVector.notify();}
  }
}
