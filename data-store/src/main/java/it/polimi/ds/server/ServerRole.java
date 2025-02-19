package it.polimi.ds.server;

import it.polimi.ds.message.AppendLogMessage;
import it.polimi.ds.message.CommitMessage;
import it.polimi.ds.model.Log;
import java.util.ArrayList;

public abstract class ServerRole {
  // ALL
  public void appendLog(Log log) {}

  public void commit(Log log) {}

  // LEADER
  public void notifyFollowers(AppendLogMessage message) throws IllegalStateException {
    throw new IllegalStateException();
  }

  public void sendResponse(ServerHandler serverHandler) {}

  public void notifyFollowers(CommitMessage message) throws IllegalStateException {
    throw new IllegalStateException();
  }

  // FOLLOWER
  public void sendWriteRequest(ServerHandler leader) throws IllegalStateException {
    throw new IllegalStateException();
  }

  public void sendACK(ServerHandler leader) throws IllegalStateException {
    throw new IllegalStateException();
  }

  // CANDIDATE
  public void elect(ArrayList<ServerHandler> servers) throws IllegalStateException {
    throw new IllegalStateException();
  }

  public void coord(ArrayList<ServerHandler> servers) throws IllegalStateException {
    throw new IllegalStateException();
  }
}
