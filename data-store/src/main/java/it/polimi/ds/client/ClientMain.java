package it.polimi.ds.client;

import java.io.IOException;
import java.util.Scanner;

/**
 * ClientMain
 *
 * <p>
 * used to start a new client
 */
public class ClientMain {

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    Client client = null;

    boolean started = false;

    do {
      try {
        String ip;

        do {
          System.out.println("Insert the server ip address:");
          ip = scanner.nextLine();
          System.out.println("Insert the server port:");
          String portStr = scanner.nextLine();
          try {
            int serverPort = Integer.parseInt(portStr);
            client = new Client(ip, serverPort);
            System.out.println("Client starting...");
            started = true;
          } catch (NumberFormatException e) {
            System.err.println("Invalid server port");
          } catch (IOException e) {
            System.err.println("Error connecting to the server, try again");
          }

          // parse user commands
          client.getUserCommands(scanner);
          // user exited the application
          scanner.close();
          System.exit(0);

        } while (!started);

      } catch (Exception e) {
        System.err.println(e.getMessage());
        scanner.close();
        System.exit(0);
      }
    } while (!started);
  }
}
