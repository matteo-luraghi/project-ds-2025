# Distributed System Project

To compile the code go in the data-store directory and run:
```bash
mvn compile
```

Then you can run the server using the command:
```bash
java -cp target/classes it.polimi.server.ServerMain
```

And the client using the command:
```bash
java -cp target/classes it.polimi.client.ClientMain
```

## Info
- the current classes are based on the raft algo for consensus, tho this may not be the one for our case since the specifics say: "a client can continue to fully operate
(read and write) as soon as it remains connected to its server, even if that server is disconnected from other servers in the system"
- a better solution could be to use a leaderless topology, using some quorum algorithm to agree on the read value and whether to write or not
- remember to provide CAUSAL consistency (reads influence writes in the same process)

## TODO
- implement the physical data store in persistent memory
- implement a quorum algo for reading values (and update wrong nodes)
- implement a quorum algo for writing values
- implement the client commands
- implement a failure recovery system to copy the updated data store when a node comes back up after a crash
- presentation slides to describe the software and run-time architecture
