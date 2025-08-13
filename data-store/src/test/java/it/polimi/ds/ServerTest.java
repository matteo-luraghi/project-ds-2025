package it.polimi.ds;

import it.polimi.ds.client.Client;
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
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

public class ServerTest {
  Server server1;
  Server server2;

  void init() throws IOException, InvalidDimensionException {
    server1 = new Server(0, 1111, 2);
    server2 = new Server(1, 1112, 2);
  }

  @Test
  public void serverTest() throws IOException, InvalidDimensionException {
    init();
    server1.start();
    server2.start();
    Client client = new Client(InetAddress.getLocalHost().getHostAddress(), 1111);
    ClientMessage msg = new WriteRequest("x", "abcd");
    client.sendMessageServer(msg);

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      System.err.println(e);
    }
  }
  @Test
  public void updatesBufferTest() throws IOException, InvalidDimensionException, InvalidInitValuesException, SQLException{
    init();
    Log log1= new Log(new TimeVector(2, new int[]{0,2}),1,"x","abcd");
    Log log2= new Log(new TimeVector(2, new int[]{0,1}),1,"y","abcd");
  
    
    server1.start();

    server1.addToUpdatesBuffer(log1);
    server1.addToUpdatesBuffer(log2);

/*   try {
      Thread.sleep(1*60*1000);
    } catch (InterruptedException e) {
      System.err.println(e);
    }
     */

    
  }
}
