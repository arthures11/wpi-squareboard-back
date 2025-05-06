# Battle Board Game - Backend API

**FRONTEND OF THIS PROJECT (ANGULAR):** [wpi-squareboard-frontend](https://github.com/arthures11/wpi-squareboard-frontend)

![image](https://github.com/user-attachments/assets/4b48517c-0aed-45ee-a47b-16a69f58e78b)


[![Java CI with Maven](https://github.com/arthures11/wpi-squareboard-back/actions/workflows/ci.yml/badge.svg)](https://github.com/arthures11/wpi-squareboard-back/actions/workflows/ci.yml)

## Overview

This project is a Spring Boot REST API server designed to manage and simulate a turn-based (with simultaneous action resolution) battle on a configurable chessboard-like battlefield. It allows two players (White and Black) to control various units, issue commands, and engage in combat.

The game state, unit positions, and command history are persisted in a PostgreSQL database. The backend handles game logic, command validation (including cooldowns and rules), unit interactions, and concurrency control using optimistic locking.

## Features

*   **Configurable Battlefield:** Set board dimensions via `application.properties`.
*   **Two Players:** White and Black opponents.
*   **Multiple Unit Types:**
    *   **Archer:** Moves orthogonally (1 square), Shoots orthogonally (configurable range).
    *   **Vehicle:** Drives orthogonally (1-3 squares), runs over enemy units.
    *   **Cannon:** Shoots diagonally/orthogonally (configurable X/Y range).
*   **Simultaneous Actions:** Players can issue commands concurrently, with cooldowns enforced per unit based on the last action.
*   **Combat Resolution:** Shots destroy units (enemy or ally). Vehicles destroy enemies upon moving onto their square. Vehicles cannot move onto ally squares.
*   **Command Cooldowns:** Different time intervals required between actions based on unit type and command (move/shoot), configured via `application.properties`.
*   **Random Unit Placement:** Units are placed randomly at the start of a new game based on configured counts from `application.properties`.
*   **RESTful API:** Manage games and issue unit commands via HTTP endpoints.
*   **Persistence:** Game state and command history are saved to a PostgreSQL database using JPA/Hibernate.
*   **Concurrency Handling:** Uses optimistic locking (`@Version`) to handle simultaneous update conflicts, returning HTTP 409 Conflict errors.

## Technology Stack

*   **Java:** 17 or later
*   **Spring Boot:** 3.x (Web, Data JPA, Validation)
*   **JPA / Hibernate:** Object-Relational Mapping
*   **PostgreSQL:** Relational Database
*   **Maven:** Build Automation Tool
*   **Lombok:** Reduced boilerplate code
*   **JUnit 5 / Mockito / H2:** Unit and Integration Testing
*   ***Angular / Tailwind etc.*** for the frontend

## Setup and Installation

1.  **Prerequisites:**
    *   JDK 17 or later installed
    *   Maven installed
    *   PostgreSQL server running

2.  **Clone the Repository:**
    ```bash
    git clone https://github.com/arthures11/wpi-squareboard-back.git
    cd wpi-squareboard-back
    ```

3.  **Database Setup:**
    *   Connect to your PostgreSQL instance.
    *   Create a dedicated database for the application (e.g., `battleboard_db`).
    *   Create a user/role with privileges on that database (e.g., `battle_user`).

4.  **Configure Application:**
    *   Open the `src/main/resources/application.properties` file.
    *   **Update the following database properties:**
        ```properties
        spring.datasource.url=jdbc:postgresql://localhost:5432/battleboard_db # Replace with your DB name/host/port
        spring.datasource.username=your_db_user     # Replace with your DB username
        spring.datasource.password=your_db_password # Replace with your DB password
        ```
    *   Review other game configuration settings in this file (see Configuration section below).

5.  **Build the Application:**
    ```bash
    mvn clean package
    ```

## Configuration and endpoints

Game rules and initial setup are configured via `src/main/resources/application.properties`. **Note:** Custom configuration per game via the API is *not* implemented; these properties define the behavior for all new games created via the `/api/games/new` endpoint.


### Game Management

*   **`POST /api/games/new`**
    *   **Description:** Starts a new game using the default configuration from `application.properties`. Archives any currently active game.
    *   **Request Body:** None (or empty JSON `{}`)
    *   **Success Response (201 Created):** `GameDTO` object representing the new game.
    *   **Error Responses:** `400 Bad Request` (if default config validation fails), `500 Internal Server Error`.

*   **`GET /api/games/active`**
    *   **Description:** Retrieves details of the currently active game.
    *   **Success Response (200 OK):** `GameDTO` of the active game.
    *   **Error Responses:** `404 Not Found` (if no game is active).

*   **`GET /api/games/{gameId}`**
    *   **Description:** Retrieves details of a specific game by its ID.
    *   **Path Variable:** `gameId` (long) - The ID of the game.
    *   **Success Response (200 OK):** `GameDTO` of the requested game.
    *   **Error Responses:** `404 Not Found` (if game with ID doesn't exist).
### Unit Information
*   **`GET /api/games/{gameId}/units`**
    *   **Description:** Lists units for a specific game. Can be filtered by player color. Primarily lists ACTIVE units.
    *   **Path Variable:** `gameId` (long) - The ID of the game.
    *   **Query Parameter (Optional):** `playerColor` (Enum: `WHITE` or `BLACK`) - Filters units by player.
    *   **Success Response (200 OK):** `List<UnitDTO>` containing the units matching the criteria.
    *   **Error Responses:** `404 Not Found` (if game doesn't exist).

*   **`GET /api/games/{gameId}/units/{unitId}`**
    *   **Description:** Retrieves details of a specific unit within a specific game.
    *   **Path Variables:**
        *   `gameId` (long) - The ID of the game.
        *   `unitId` (long) - The ID of the unit.
    *   **Success Response (200 OK):** `UnitDTO` of the requested unit.
    *   **Error Responses:** `404 Not Found` (if game or unit within game doesn't exist).
### Unit Commands
*   **`POST /api/games/{gameId}/units/{unitId}/command`**
    *   **Description:** Issues a specific command (Move or Shoot) to a unit, validating against the game's rules and cooldowns.
    *   **Path Variables:** `gameId`, `unitId`.
    *   **Request Body:** `CommandRequestDTO`
        ```json
        {
          "playerColor": "WHITE", // or "BLACK" - Player issuing the command
          "commandType": "MOVE",  // or "SHOOT"
          "targetX": 5,
          "targetY": 6
        }
        ```
    *   **Success Response (200 OK):** `UnitDTO` representing the updated state of the commanded unit.
    *   **Error Responses:**
        *   `400 Bad Request`: Invalid command (e.g., out of bounds, invalid distance/target, required target missing, invalid JSON field/enum value).
        *   `403 Forbidden`: Attempting to command opponent's unit.
        *   `404 Not Found`: Game or Unit not found.
        *   `409 Conflict`: Optimistic locking failure (concurrent modification detected). Try again.
        *   `429 Too Many Requests`: Cooldown period not yet elapsed for the unit/action.
*   **`POST /api/games/{gameId}/units/{unitId}/command/random`**
    *   **Description:** Issues a request for the server to execute a valid random command (Move or Shoot, depending on unit type and available actions) for the specified unit.
    *   **Path Variables:** `gameId`, `unitId`.
    *   **Request Body:** `RandomCommandRequestDTO`
        ```json
        {
          "playerColor": "WHITE" // or "BLACK" - Player requesting the random command
        }
        ```
    *   **Success Response (200 OK):** `UnitDTO` representing the updated state of the commanded unit after the random action.
    *   **Error Responses:** Same as specific command, plus potential `400 Bad Request` if no valid random actions are currently possible for the unit (e.g., blocked, cannot move/shoot).
## Testing
The project includes unit and integration tests designed to run quickly and validate functionality. Tests utilize H2 as an in-memory database.
*   **Unit Tests (Mockito):** Test service layer logic in isolation (`src/test/java/.../service`).
*   **Repository Tests (`@DataJpaTest`):** Test JPA mappings and custom queries (`src/test/java/.../repository`).
*   **Controller Tests (`@WebMvcTest`):** Test API endpoints, mocking service interactions (`src/test/java/.../controller`).
*   **Integration Tests (`@SpringBootTest`):** Test the full application context loading (`contextLoads`) using H2.
Run all tests using Maven (tests are also automatically executed on every push request via GitHub Actions (see CI badge at the top):
```bash
mvn test
```
## Demo
See a quick overview of the backend API functionalities interacting with a basic frontend client:

*(Click the image to watch the video)*

[![BattleBoard Demo Video](https://img.youtube.com/vi/ScBcltVp-0M/0.jpg)](https://www.youtube.com/watch?v=ScBcltVp-0M)
