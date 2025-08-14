package it.polimi.ds;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.polimi.ds.client.Client;
import it.polimi.ds.database.Database;
import it.polimi.ds.message.ClientMessage;
import it.polimi.ds.message.WriteRequest;
import it.polimi.ds.model.Log;
import it.polimi.ds.model.TimeVector;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.model.exception.InvalidInitValuesException;
import it.polimi.ds.server.Server;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

public class ServerTest {
  Server server1;
  Server server2;

  void init() throws IOException, InvalidDimensionException {
    server1 = new Server(0, 1111, 2);
    server2 = new Server(1, 1112, 2);
  }

  public void cleanup() {
    if (server1 != null) {
      server1.stop();
    }
    if (server2 != null) {
      server2.stop();
    }
  }

  @Test
  public void serverTest()
      throws IOException, InvalidDimensionException, SQLException, InvalidInitValuesException {
    init();
    server1.start();
    server2.start();

    Database db1 = server1.getDb();
    Database db2 = server2.getDb();
    // initialize all zeros in case log table empty
    int[] previousVector = new int[2];

    // try to get last log as reference for later assertions
    try {
      previousVector = db1.getLastLog().getVectorClock().getVector();
    } catch (Exception ignored) {
    }

    Client client = new Client(InetAddress.getLocalHost().getHostAddress(), 1111);
    ClientMessage msg = new WriteRequest("x", "abcd");
    client.sendMessageServer(msg);

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      System.err.println(e);
    }

    // data stored in db with correct (key, value) pair
    assertTrue(db1.readValue("x").equals("abcd"));
    assertTrue(db2.readValue("x").equals("abcd"));
    // log stored in db
    Log log1 = db1.getLastLog();
    Log log2 = db1.getLastLog();
    // server id must be 0
    assertTrue(log1.getServerId() == 0);
    assertTrue(log2.getServerId() == 0);
    // write key must be "x"
    assertTrue(log1.getWriteKey().equals("x"));
    assertTrue(log2.getWriteKey().equals("x"));
    // write value must be "abcd"
    assertTrue(log1.getWriteValue().equals("abcd"));
    assertTrue(log2.getWriteValue().equals("abcd"));
    // time vectors must be equal between the 2 servers and must be updated wrt
    // previousVector
    int[] vector1 = log1.getVectorClock().getVector();
    int[] vector2 = log2.getVectorClock().getVector();
    assertTrue(vector1[0] == vector2[0] && vector1[0] == previousVector[0] + 1);
    assertTrue(vector1[1] == vector2[1] && vector1[1] == previousVector[1]);

    cleanup();
  }

  @Test
  public void updatesBufferTest()
      throws IOException, InvalidDimensionException, InvalidInitValuesException, SQLException {
    init();
    Log log1 = new Log(new TimeVector(2, new int[] {0, 2}), 1, "x", "abcd");
    Log log2 = new Log(new TimeVector(2, new int[] {0, 1}), 1, "y", "abcd");

    server1.start();

    server1.addToUpdatesBuffer(log1);
    server1.addToUpdatesBuffer(log2);

    cleanup();

    /*
     * try {
     * Thread.sleep(1*60*1000);
     * } catch (InterruptedException e) {
     * System.err.println(e);
     * }
     */
  }
}
