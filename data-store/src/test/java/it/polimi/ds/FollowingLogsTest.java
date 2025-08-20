package it.polimi.ds;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.polimi.ds.model.Log;
import it.polimi.ds.model.TimeVector;
import it.polimi.ds.model.exception.ImpossibleComparisonException;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.model.exception.InvalidInitValuesException;
import it.polimi.ds.server.Server;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FollowingLogsTest {

  @Test
  public void followingLogsTest()
      throws IOException,
          InvalidDimensionException,
          InvalidInitValuesException,
          SQLException,
          ImpossibleComparisonException {

    Server server = new Server(0, 1110, 2, true);

    TimeVector timeVector = new TimeVector(2);
    Log testLog = null;

    // clear the db tables and reset time vector
    server.reset();
    // fill the db with operations
    for (int i = 0; i < 10; i++) {
      // change operating server each iteration
      int serverId = i % 2;
      timeVector.increment(serverId);
      Log log = new Log(timeVector, serverId, "x", "abcd");
      server.executeWrite(log);

      // save a log to test later
      if (i == 6) {
        testLog = log;
      }
      System.out.println("i=" + i + ": " + Arrays.toString(timeVector.getVector()));
    }

    // get all the logs following the testLog
    List<Log> followingLogs = server.getDb().getFollowingLogs(testLog);
    // check that only the last 3 logs are returned
    assertTrue(followingLogs.size() == 3);

    for (Log log : followingLogs) {
      System.out.println(Arrays.toString(log.getVectorClock().getVector()));
    }
  }
}
