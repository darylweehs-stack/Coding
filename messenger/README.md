# Messenger API

Standalone Java 21 / Spring Boot REST API with JWT authentication and H2 persistence.

## Run

```bash
mvn spring-boot:run
```

The in-memory H2 database resets when the application stops. For deployments, set `JWT_SECRET` to a private secret of at least 32 characters and configure a persistent database.

## API

Register and login return a bearer token. Include it as `Authorization: Bearer <token>` for friend and message endpoints. Adding a user to your friends automatically adds you to their friends as well, so both users can message each other.

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Create an account and return a token |
| `POST` | `/api/auth/login` | Authenticate and return a token |
| `POST` | `/api/friends/{userId}` | Add the user to your friend list |
| `GET` | `/api/friends` | List your friends |
| `POST` | `/api/messages/send` | Send to a friend using the `userId` in the JSON body |
| `GET` | `/api/messages/new` | Receive and mark all pending messages for the authenticated user as delivered |
| `GET` | `/api/messages/conversations/{friendId}` | Retrieve the full history with a friend, oldest first |

Example message body:

```json
{
  "userId": 2,
  "content": "Hello",
  "timestamp": "2026-09-26T12:30:00Z"
}
```

Sending to a user who is not in your friend list returns `403 Forbidden`. Adding the same friend more than once is safe and returns the existing entry.

Registration body: `{"username":"alice","password":"at-least-8-chars"}`. The H2 console is available at `http://localhost:8080/h2-console` during local development; use JDBC URL `jdbc:h2:mem:messenger` and username `sa`.

Run tests with `mvn test`.