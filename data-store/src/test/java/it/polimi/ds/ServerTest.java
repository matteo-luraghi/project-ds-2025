package it.polimi.ds;


import java.io.IOException;
import java.net.InetAddress;

import org.junit.jupiter.api.Test;

import it.polimi.ds.client.Client;
import it.polimi.ds.message.AppendLogMessage;
import it.polimi.ds.message.ClientMessage;
import it.polimi.ds.message.ServerToServerMessage;
import it.polimi.ds.message.WriteRequest;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.server.Server;

public class ServerTest {
    Server server1;
    Server server2;
    void init() throws IOException, InvalidDimensionException{
        server1= new Server(0,1111,2);
        server2= new Server(1,1112,2);
    }
    @Test
    public void serverTest() throws IOException, InvalidDimensionException{
        init();
        server1.start();
        server2.start();
        Client client= new Client(InetAddress.getLocalHost().getHostAddress(), 1111);
        ClientMessage msg= new WriteRequest("x","abcd");
        client.sendMessageServer(msg);
        


    }
}
