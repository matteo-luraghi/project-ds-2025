package it.polimi.ds.server;

import java.io.IOException;
import java.util.Scanner;

/**
 * ServerMain
 *
 * <p>
 * used to create and run a new server
 */
public class ServerMain {
  public static void main(String[] args) {
    boolean started = false;
    Scanner scanner = new Scanner(System.in);

    do {
      System.out.println("Inser the server port:");
      String portStr = scanner.nextLine();
      try {
        int serverPort = Integer.parseInt(portStr);
        Server server = new Server(serverPort);
        server.start();
        started = true;
      } catch (NumberFormatException e) {
        System.err.println("Invalid server port");
      } catch (IOException e) {
        System.err.println("Error starting the server, try again");
      }

    } while (!started);
    scanner.close();
  }
}
