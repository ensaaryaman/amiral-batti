# ⚓ Amiral Battı (Battleship)

A network-based two-player Battleship game built with Java, using a Client-Server architecture over socket programming.

---

## About

This project is a real-time multiplayer implementation of the classic Battleship strategy game. Two players connect to a shared server and take turns trying to locate and sink each other's ships. The game handles ship placement, shot firing, hit/miss detection, and ship sinking — all communicated in real time over TCP sockets.

## Features

- **Multiplayer over Network** — Two players connect via TCP sockets to a central server
- **Client-Server Architecture** — Server manages game state and synchronizes both clients
- **Real-Time Gameplay** — Hit, miss, and ship-sunk events are communicated instantly
- **AWS EC2 Support** — Server can be hosted locally or on a cloud instance (e.g. AWS EC2)
- **Classic Battleship Rules** — Ship placement, turn-based firing, win condition

## Tech Stack

| | |
|---|---|
| Language | Java |
| Build Tool | Maven (`pom.xml`) |
| Networking | Java Sockets (TCP) |
| Architecture | Client-Server |

## Getting Started

### Requirements

- Java 11+
- Maven

### Build

```bash
mvn clean package
```

### Run Server

```bash
java -cp target/*.jar com.battleship.Server
```

### Run Client

```bash
java -cp target/*.jar com.battleship.Client
```

> Make sure the server is running before clients connect. Update the server IP in the client config if connecting remotely.

## Project Structure

```
amiral-batti/
├── src/main/java/com/battleship/   # Game source code (server & client)
├── target/                         # Compiled output
├── pom.xml                         # Maven build config
└── README.md
```

## License

This project is open source. Feel free to fork and build on it.
