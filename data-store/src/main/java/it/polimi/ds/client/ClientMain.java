package it.polimi.ds.client;

import java.io.IOException;
import java.util.Scanner;

public class ClientMain {

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    Client client = null;

    boolean started = false;

    do {
      try {
        String ip;

        do {
          System.out.println("Insert a valid ip address:");
          ip = scanner.nextLine();
          try {
            client = new Client(ip, 1234);
            System.out.println("Client starting...");
            started = true;
          } catch (IOException e) {
            System.err.println("Error connecting to the server, try again");
          }

          // parse user commands
          client.getUserCommands(scanner);

        } while (!started);

      } catch (Exception e) {
        System.err.println(e.getMessage());
        System.exit(0);
      }
    } while (!started);
  }
}
