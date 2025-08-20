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
    servers = new ArrayList<>();
    clients = new ArrayList<>();
  }

  @Test
  public void crashTest()
      throws IOException, InvalidDimensionException, InterruptedException, SQLException {
    init();

    for (Server server : servers) {
      server.start();
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
    servers.remove(0);
    System.out.println("Server crashed");
    // keep the server down for some time
    Thread.sleep(3000);
    // create a new thread for the crashed server
    servers.add(new Server(0, 1111, serversNum, true));
    // restart the crashed server
    servers.getLast().start();
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

  @Test
  public void writeAfterCrash()
      throws IOException, InvalidDimensionException, InterruptedException, SQLException {
    init();

    for (Server server : servers) {
      server.start();
    }

    // setup the client
    clients.add(new Client(InetAddress.getLocalHost().getHostAddress(), 1111));

    clients.get(0).sendMessageServer(new WriteRequest("x", "1"));
    Thread.sleep(1000);
    // simulate server crash
    servers.get(1).stop();
    servers.remove(1);
    Thread.sleep(1000);
    clients.get(0).sendMessageServer(new WriteRequest("y", "1"));
    Thread.sleep(1000);
    // recreate server
    servers.add(new Server(1, 1112, serversNum, true));
    servers.getLast().start();
    Thread.sleep(500);
    // create new client
    clients.add(new Client(InetAddress.getLocalHost().getHostAddress(), 1112));
    // write from the crashed server
    clients.get(1).sendMessageServer(new WriteRequest("y", "2"));
    Thread.sleep(1000);
    // write from updated server to trigger log request
    clients.get(0).sendMessageServer(new WriteRequest("z", "1"));
    Thread.sleep(1000);
    clients.get(0).sendMessageServer(new WriteRequest("z", "2"));
    Thread.sleep(1000);

    for (Client client : clients) {
      client.disconnect();
    }
    for (Server server : servers) {
      server.stop();
    }

    for (Server server : servers) {
      System.out.println(server.getServerId());
      System.out.println(Arrays.toString(server.getTimeVector().getVector()));
    }

    cleanup();
  }
}
