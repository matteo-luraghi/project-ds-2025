package it.polimi.ds.server;

import it.polimi.ds.database.Database;
import it.polimi.ds.message.LogsRequestMessage;
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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private NetworkInterface networkInterface;
  private SocketAddress groupAddress;
  private final AtomicBoolean running = new AtomicBoolean(true);
  private final Thread clientsThread;
  private final Thread multicastReceiveThread;
  private final Thread updateBufferThread;
  private AtomicBoolean isBufferReady = new AtomicBoolean(false);
  private int UpdateMissLoops = 0;
  private final int maxUpdateMissLoops = 1;
  private final ExecutorService executor;
  private final List<ClientHandler> clients = new ArrayList<>();
  private TimeVector timeVector = null;
  private TreeSet<Log> updatesBuffer = new TreeSet<Log>(new Log.LogComparator());

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
  public Server(int id, int serverPort, int serversNumber, boolean test)
      throws IOException, InvalidDimensionException {
    this.id = id;
    this.serverPort = serverPort;

    // select the correct network interface
    this.setupNetworkInterface(test);

    this.serverIP = InetAddress.getLocalHost().getHostAddress();

    // db setup, the name of the db file is serverIP_serverPort.db
    try {
      this.db = new Database(this.serverIP + "_" + Integer.toString(this.serverPort));
    } catch (SQLException e) {
      System.err.println(e);
      throw new IOException();
    }

    // restore the last time vector stored in the db
    try {
      Log log = this.db.getLastLog();
      if (log != null) {
        this.timeVector = log.getVectorClock();
      }
    } catch (NumberFormatException
        | SQLException
        | InvalidDimensionException
        | InvalidInitValuesException e) {
      System.out.println(e);
    }

    // initialize the server's time vector with all zeros
    if (this.timeVector == null) {
      this.timeVector = new TimeVector(serversNumber);
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
    System.out.println("\nServer running at: " + this.serverIP);
    System.out.println("Port for client connections: " + this.serverPort + "\n");
  }

  /**
   * Setup the multicast socket
   *
   * @throws IOException
   */
  private void multicastSocketSetup() throws IOException {
    this.multicastGroup = InetAddress.getByName(multicastAddress);
    this.groupAddress = new InetSocketAddress(multicastGroup, this.multicastPort);

    this.multicastSocket = new MulticastSocket(this.multicastPort);
    this.multicastSocket.setReuseAddress(true);
    this.multicastSocket.setNetworkInterface(this.networkInterface);
    // join multicast group
    this.multicastSocket.joinGroup(this.groupAddress, this.networkInterface);
    System.out.println(
        "Multicast group info:\n   - Port: "
            + this.multicastPort
            + "\n   - IP: "
            + this.multicastAddress
            + "\n");
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

    // start managing updates buffer
    this.updateBufferThread.start();
  }

  /** Thread that accepts connections from clients */
  private void acceptClients() {
    while (this.running.get()) {
      try {
        Socket clientSocket = this.serverSocket.accept();
        clientSocket.setSoTimeout(10000);
        ClientHandler clientHandler = new ClientHandler(this, clientSocket);

        // add the client to the clients
        this.clients.add(clientHandler);
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
    while (this.running.get()) {
      try {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        this.multicastSocket.receive(packet);
        // System.out.println("multicast message received");

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
   * Waits until updatesBuffer is not empty or there are log that cannot be processed, then for each
   * log inside the buffer if it happens before the current vector clock executes the writes on the
   * database and removes the log from the buffer. If at least one log is not processed for
   * "maxUpdateMissLoops" consecutive iteration then a LogsRequestMessage is sent
   */
  public void manageUpdateBuffer() {
    while (this.running.get()) {
      synchronized (timeVector) {
        while (!this.isBufferReady.get()) {
          // waiting for new logs
          try {
            timeVector.wait();
          } catch (InterruptedException ignore) {
          }
        }
        // clone the buffer for safety
        TreeSet<Log> updatesBufferClone = new TreeSet<>(updatesBuffer);

        boolean haveLostUpdates = false;
        for (Log log : updatesBufferClone) {
          TimeVector logVC = log.getVectorClock();
          try {
            if (logVC.happensBefore(this.timeVector, log.getServerId())) {
              // causal consistency satisfied, execute the write
              executeWrite(log);
              timeVector.merge(logVC, log.getServerId());
              updatesBuffer.remove(log);
            } else {
              // write cannot be executed
              haveLostUpdates = true;
            }
          } catch (ImpossibleComparisonException | SQLException e) {
            System.out.println("While managing updates buffer:");
            System.out.println(e.getMessage());
          }
        }
        // set the buffer to not ready
        this.isBufferReady.set(false);

        // if during this loop an update is not executed
        if (haveLostUpdates) {
          // increment counter
          UpdateMissLoops++;
          if (UpdateMissLoops > maxUpdateMissLoops) {
            // too many consecutive loops with update miss, send a log request
            sendLogsRequestMessage();
          }
        } else {
          // no update lost, reset the counter
          UpdateMissLoops = 0;
        }
      }
    }
  }

  /** Send a LogRequestMessage in multicast to get missing logs */
  private void sendLogsRequestMessage() {
    try {
      this.sendMulticastMessage(
          new LogsRequestMessage(this.getServerId(), this.getDb().getLastLog()));
    } catch (SQLException | InvalidDimensionException | InvalidInitValuesException e) {
      System.out.println(e);
    }
  }

  /**
   * if the log is not already in the db then
   * Writes the log in the db Perfrorms the write in the db Merges the vector clock of the log with
   * the server's one Awakes the buffer updates thread
   * otherwhise nothing happens
   *
   * @param log the log of the write to be performed
   */
  public void executeWrite(Log log) throws SQLException, ImpossibleComparisonException {
    this.db.executeTransactionalWrite(log);
    
    System.out.println(
        Integer.toString(this.getServerId())
            + ")Write executed "
            + "("
            + log.getWriteKey()
            + ","
            + log.getWriteValue()
            + ") "
            + "vc: "
            + Arrays.toString(log.getVectorClock().getVector())
            );
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
      // System.out.println("multicast msg send");

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

  /**
   * Add log to Buffer
   *
   * @param log the log to add
   */
  public void addToUpdatesBuffer(Log log) {
    synchronized (timeVector) {
      updatesBuffer.add(log);
      isBufferReady.set(true);
      timeVector.notify();
    }
  }

  /** Get all the valid interface of the machine */
  private List<NetworkInterface> getValidInterfaces() throws SocketException {
    List<NetworkInterface> result = new ArrayList<>();

    for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
      if (!nif.isUp() || !nif.supportsMulticast() || nif.isLoopback()) continue;

      boolean hasIPv4 = false;
      for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
        if (addr instanceof Inet4Address
            && !addr.isLoopbackAddress()
            && !addr.isLinkLocalAddress()) {
          hasIPv4 = true;
          break;
        }
      }

      if (hasIPv4) {
        result.add(nif);
      }
    }
    return result;
  }

  /** Selects the only valid network interface or lets the user choose it */
  private void setupNetworkInterface(boolean test) throws SocketException {
    // set prefernce on IPv4
    System.setProperty("java.net.preferIPv4Stack", "true");

    // filter only valid network interfaces
    List<NetworkInterface> validNets = getValidInterfaces();

    // if 1 valid network interface choose that one
    if (validNets.size() == 1 || test) {
      this.networkInterface = validNets.getFirst();
    } else {
      // show network interfaces and ip addresses
      for (int i = 0; i < validNets.size(); i++) {
        System.out.println(i + ") " + validNets.get(i).getName() + ":");
        Enumeration<InetAddress> addresses = validNets.get(i).getInetAddresses();

        while (addresses.hasMoreElements()) {
          InetAddress addr = addresses.nextElement();
          System.out.println("- " + addr.getHostAddress());
        }
      }
      // let the user choose the network interface
      Scanner scanner = new Scanner(System.in);
      boolean valid = false;
      int netIdx = 10000;
      while (!valid) {
        try {
          System.out.println("Select the number of the network interface to use:");
          netIdx = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
          System.out.println("Insert a valid number");
        }
        if (netIdx < validNets.size()) {
          this.networkInterface = validNets.get(netIdx);
          valid = true;
          scanner.close();
        } else {
          System.out.println("Insert a valid number");
        }
      }
    }
  }

  /** Reset database and time vector */
  public void reset() throws SQLException, InvalidDimensionException {
    this.getDb().resetDatabase();
    int len = this.timeVector.getVector().length;
    this.timeVector = new TimeVector(len);
  }

  /** Stop all the threads, close sockets and db connection */
  public void stop() {
    // stop all the clients
    for (ClientHandler c : clients) {
      c.disconnect();
    }
    // stop running threads
    this.running.set(false);

    // stop the active threads
    if (this.clientsThread != null && this.clientsThread.isAlive()) {
      this.clientsThread.interrupt();
    }
    if (this.multicastReceiveThread != null && this.multicastReceiveThread.isAlive()) {
      this.multicastReceiveThread.interrupt();
    }
    if (this.updateBufferThread != null && this.updateBufferThread.isAlive()) {
      synchronized (timeVector) {
        this.updateBufferThread.interrupt();
      }
    }

    // stop accepting new client connections
    try {
      this.serverSocket.close();
    } catch (IOException e) {
      System.err.println("Error closing server socket: " + e.getMessage());
    }

    // stop receiving multicast messages
    if (this.multicastSocket != null && !this.multicastSocket.isClosed()) {
      try {
        this.multicastSocket.leaveGroup(this.groupAddress, this.networkInterface);
        this.multicastSocket.close();
      } catch (IOException e) {
        System.err.println("Error closing multicast socket: " + e.getMessage());
      }
    }

    // shutdown the executor to stop all client handler threads
    if (this.executor != null && !this.executor.isShutdown()) {
      this.executor.shutdownNow();
    }

    // close the db connection
    try {
      this.db.close();
    } catch (SQLException e) {
      System.err.println("Error closing the database: " + e.getMessage());
    }
  }
}
