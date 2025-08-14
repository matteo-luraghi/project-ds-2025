package it.polimi.ds;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.server.Server;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ServerCrashTest {
  List<Server> servers = new ArrayList<>();
  int serversNum = 4;

  private void init() throws IOException, InvalidDimensionException {
    for (int i = 0; i < serversNum; i++) {
      servers.add(new Server(i, 1111 + i, serversNum));
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
  public void crashTest() throws IOException, InvalidDimensionException, InterruptedException {
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

    // TODO: create a thread where clients that exchange messages in a for loop
    // with some random sleep so that even when the crashed server is down
    // messages continue being exchanged

    // simulate server crash
    serverThreads.get(0).interrupt();
    // wait for the thread to terminate
    serverThreads.get(0).join();
    // keep the server down for some time
    Thread.sleep(5000);
    // create a new thread for the crashed server
    serverThreads.set(0, createServerThread(servers.get(0)));
    // restart the crashed server
    serverThreads.get(0).start();

    // TODO: check that the crashed server has recovered state correctly

    assertTrue(true);

    cleanup();
  }
}
