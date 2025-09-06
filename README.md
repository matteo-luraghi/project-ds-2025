# Distributed System Project - Data Store

## 🗄️ Project Overview

Project developed for the "Distributed System" course. This project is a distributed key-value store that ensures high availability and causal consistency across multiple servers.

The complete requirements are described in the [Technical Specification](./Technical%20Specification.md)

### Key Features

- **Replicated Storage** – every server holds a full copy of the key-value store.
- **Causal Consistency** – operations respect causal dependencies across clients and servers.
- **High Availability** – clients can continue reading and writing as long as their server is reachable, even if that server is temporarily disconnected from the others.
- **Client–Server Architecture** – clients connect to one server for their lifetime and issue `read(key)` and `write(key, value)` requests.

The system is fully implemented in Java 21 and uses Maven for building and running.

You can either [compile the code yourself](#%EF%B8%8F-compile-yourself) (recommended for testing and applying modifications) or run the [pre-built JAR files](#-use-the-jars).

---

## 🛠️ Compile Yourself

### Requirements

- [Maven](https://maven.apache.org/)
- [Java 21](https://www.oracle.com/it/java/technologies/downloads/)

### Steps

1. Navigate to the `data-store` directory:

   ```bash
   cd data-store
   ```
2. Compile the project:

   ```bash
   mvn compile
   ```
3. Run the server:

   ```bash
   mvn exec:java -Dexec.mainClass="it.polimi.ds.server.ServerMain"
   ```
4. Run the client:

   ```bash
   mvn exec:java -Dexec.mainClass="it.polimi.ds.client.ClientMain"
   ```

---

## 📦 Use the JARs

### Requirements

- [Java 21](https://www.oracle.com/it/java/technologies/downloads/)

### Steps

1. Open a terminal in the directory that contains the `.jar` files

2. Run the server:

   ```bash
   java -jar ./server.jar
   ```
3. Run the client:

   ```bash
   java -jar ./client.jar
   ```

### ⚠️ Troubleshooting

If you encounter an error like:
   ```bash
   java.sql.SQLException: path to 'storage/ip_port.db': '/path/to/jars/storage' does not exist
   ```
you need to create a `storage` directory in the same directory as the JAR files:
   ```bash
   mkdir storage
   ```

