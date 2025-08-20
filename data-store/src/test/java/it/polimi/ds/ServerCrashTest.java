package it.polimi.ds;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.polimi.ds.client.Client;
import it.polimi.ds.message.WriteRequest;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.server.Server;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ServerCrashTest {
  List<Server> servers = new ArrayList<>();
  List<Client> clients = new ArrayList<>();
  int serversNum = 4;

  private void init() throws IOException, InvalidDimensionException, SQLException {
    for (int i = 0; i < serversNum; i++) {
      servers.add(new Server(i, 1111 + i, serversNum, true));
    }
    // clean the databases and reset time vector
    for (Server server : servers) {
      server.reset();
    }
  }

  private void cleanup() {
    for (Server server : servers) {
      server.stop();
    }
  }

  /**
   * Create a thread to run a Server
   *
   * @params server the server to be run
   */
  private Thread createServerThread(Server server) {
    return new Thread(
        () -> {
          try {
            server.start();
          } catch (IOException e) {
            System.out.println("Error starting server");
          }
        });
  }

  @Test
  public void crashTest()
      throws IOException, InvalidDimensionException, InterruptedException, SQLException {
    init();

    List<Thread> serverThreads = new ArrayList<>();

    for (Server server : servers) {
      // create a different thread that runs the server
      Thread serverThread = createServerThread(server);
      // save the server thread in the list
      serverThreads.add(serverThread);
      // run the server
      serverThread.start();
    }

    // setup the clients
    for (int i = 0; i < serversNum; i++) {
      clients.add(new Client(InetAddress.getLocalHost().getHostAddress(), 1111 + i));
    }

    // simulate message exchanging
    Thread clientsThread =
        new Thread(
            () -> {
              int i = 0;
              int counter = 0;
              while (true) {
                clients
                    .get(i)
                    .sendMessageServer(
                        new WriteRequest(Integer.toString(counter), Integer.toString(counter)));
                i++;
                i %= this.serversNum;
                counter++;
                try {
                  // add a time delay
                  Thread.sleep(200);
                } catch (InterruptedException e) {
                  System.out.println(e);
                }
              }
            });
    clientsThread.start();

    Thread.sleep(3000);
    // simulate server crash
    servers.get(0).stop();
    serverThreads.get(0).interrupt();
    System.out.println("Server crashed");
    // wait for the thread to terminate
    serverThreads.get(0).join();
    // keep the server down for some time
    Thread.sleep(3000);
    // create a new thread for the crashed server
    serverThreads.add(createServerThread(new Server(0, 1111, serversNum, true)));
    // restart the crashed server
    serverThreads.getLast().start();
    System.out.println("Server active again");
    // wait for some messages to update logs
    Thread.sleep(3000);
    // stop sending messages
    System.out.println("Terminating clients");
    clientsThread.interrupt();
    for (Client client : clients) {
      client.disconnect();
    }

    // check if all servers are synchronized
    for (int i = 0; i < serversNum; i++) {
      System.out.println(
          "server #" + i + " : " + Arrays.toString(servers.get(i).getTimeVector().getVector()));
      for (int j = i + 1; j < serversNum; j++) {
        assertTrue(
            Arrays.toString(servers.get(i).getTimeVector().getVector())
                .equals(Arrays.toString(servers.get(j).getTimeVector().getVector())));
      }
    }

    cleanup();
  }
}
