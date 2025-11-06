# Multi-user Instant Messaging (IM) System

> **G02 DAW LEIC ISEL 2024/25**

**Contributors:**
- Diogo Ribeiro 47207
- António Coelho 47236
- Rafael Pegacho 49423

## Configuration

Before running the application, you must set the following environment variables.

### JWT Secret

The application uses a JWT (JSON Web Token) for authentication, which requires a strong secret key. Set this using the `JWT_SECRET` environment variable.

You can generate a strong secret using a tool like OpenSSL: `openssl rand -base64 32`

## Steps to Execute

1. **Start Docker Desktop:**
    - Ensure Docker Desktop is running on your machine.

2. **Open the Backend Project:**
    - Navigate to the `jvm` folder.
    - Open it using IntelliJ IDEA.

3. **Open the Frontend Project:**
    - Navigate to the `js` folder.
    - Open it using Visual Studio Code.

4. **Run Docker Compose:**
    - In the `jvm` folder, locate the `docker-compose.yml` file.
    - Execute the following command to start the necessary Docker containers:
      ```bash
      docker-compose up -d
      ```

5. **Start the Spring Boot Application:**
    - In IntelliJ IDEA, locate the `AppInstantMessaging` Spring Boot application in the `host` folder.
    - Run the application with the following environment variables:
      ```
      # Replace with your strong, randomly generated secret
      JWT_SECRET=your-strong-and-long-jwt-secret-key
      DB_URL=jdbc:postgresql://localhost:5432/db?user=dbuser&password=isel
      ```
    - This sets up the connection to the PostgreSQL database.

6. **Start the Frontend Application:**
    - Open a terminal in the `js` folder.
    - Install dependencies if not already done:
      ```bash
      npm install
      ```
    - Start the frontend development server:
      ```bash
      npm start
      ```

7. **Access the IM System:**
    - Open your web browser.
    - Navigate to [http://localhost:8000](http://localhost:8000).
    - You can now start using the Instant Messaging (IM) system.

## Test Users

The test database is pre-populated with the following users. Use these credentials to log in and explore the system's functionalities.

| **Username** | **Password** |
|--------------|--------------|
| antonio      | Password1!   |
| diogo        | Password1!   |
| rafael       | Password1!   |
| miguel       | Password1!   |
