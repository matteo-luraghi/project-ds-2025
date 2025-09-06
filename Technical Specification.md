# Replicated, highly-available data store

Implement a highly-available, replicated key value store. The store offers two primitives:
read(key) and write(key, value).
The store is implemented by N servers. Each of them contains a copy of the entire store and
cooperate to keep a consistent view of this replicated store. In particular, the system must
provide a causal consistency model.
Clients may connect to any of the servers (the same one for the entire lifetime of the client).
The system must be highly available, meaning that a client can continue to fully operate
(read and write) as soon as it remains connected to its server, even if that server is
disconnected from other servers in the system.
Implement the project in Java or simulate it in OmNet++.

## Assumptions

- Servers and channels may fail (omission failures only).

## Rules

1. The project is optional and, if correctly developed, contributes by increasing the final
   score.
2. Projects must be developed in groups composed of a minimum of two and a
   maximum of three students.
3. The set of projects described below are valid for this academic year only. This means
   that they have to be presented before the last official exam session of this academic
   year.
4. Students are expected to demonstrate their project using their own notebooks (at
   least two) connected in a LAN (wired or wireless) to show that everything works in a
   really distributed scenario.
5. To present their work, students are expected to use a few slides describing the
   software and run-time architecture of their solution.
6. Projects developed in Java cannot use networking technologies other than sockets
   (TCP or UDP, unicast or multicast) or RMI.
