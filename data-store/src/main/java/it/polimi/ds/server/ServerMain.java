package it.polimi.ds.server;

import it.polimi.ds.model.exception.InvalidDimensionException;
import java.io.IOException;
import java.util.Scanner;

/**
 * ServerMain
 *
 * <p>used to create and run a new server
 */
public class ServerMain {
  public static void main(String[] args) {
    boolean started = false;
    Scanner scanner = new Scanner(System.in);

    do {
      System.out.println("Insert the server port:");
      String portStr = scanner.nextLine();
      System.out.println("Insert the server ID:");
      String idStr = scanner.nextLine();
      System.out.println("Insert the total number of servers:");
      String serversNumberStr = scanner.nextLine();

      try {
        int serverPort = Integer.parseInt(portStr);
        int serverId = Integer.parseInt(idStr);
        int serversNumber = Integer.parseInt(serversNumberStr);
        Server server = new Server(serverId, serverPort, serversNumber);
        server.start();
        started = true;
      } catch (NumberFormatException e) {
        System.err.println("Invalid server port, server ID or servers number, try again");
      } catch (IOException | InvalidDimensionException e) {
        System.err.println("Error starting the server, try again");
      }

    } while (!started);
    scanner.close();
  }
}
