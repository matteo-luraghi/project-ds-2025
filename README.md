# Distributed System Project

To compile the code go in the data-store directory and run:
```bash
mvn compile
```

Then you can run the server using the command:
```bash
mvn exec:java -Dexec.mainClass="it.polimi.ds.server.ServerMain"
```

And the client using the command:
```bash
mvn exec:java -Dexec.mainClass="it.polimi.ds.client.ClientMain"
```

## TODO
- ask past updates if see that a replica is not up to date
- manage network partitions
- implement a failure recovery system to copy the updated data store when a node comes back up after a crash
- presentation slides to describe the software and run-time architecture
