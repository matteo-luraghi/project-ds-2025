# Distributed System Project

This project provides both server and client components.\
You can either **compile the code yourself** (recommended for testing and applying modifications)\
or run the **pre-built JAR files**.

---

## Option 1: Compile Yourself

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

## Option 2: Using the Pre-Built JARs

### Requirements

- [Java 21](https://www.oracle.com/it/java/technologies/downloads/)

### Steps

1. Open a terminal in the `JAR` directory:

   ```bash
   cd deliverables/JAR
   ```
2. Run the server:

   ```bash
   java -jar ./server.jar
   ```
3. Run the client:

   ```bash
   java -jar ./client.jar
   ```
