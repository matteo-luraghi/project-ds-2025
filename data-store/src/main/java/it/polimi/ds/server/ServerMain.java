package it.polimi.ds.server;


public class ServerMain {
  public static void main(String[] args) {
    boolean started = false;

    do {
      try {
        Server server = new Server();
        server.start();
        started = true;
      } catch (Exception e) {
        System.err.println("Error starting the server, try again");
      }
    } while (!started);
  }
}
