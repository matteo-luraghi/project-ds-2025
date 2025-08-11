package it.polimi.ds.message;

public class AppendLogMessage extends ServerToServerMessage {

  @Override
  public void execute() {
    System.out.println("Log needs to be appended");
  }
}
