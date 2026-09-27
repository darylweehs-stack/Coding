# Full-stack skeleton

A starter workspace for an Angular frontend and Java REST API backend using Spring Boot.

## Projects

- `angular-frontend` - Angular 18 standalone frontend on port 4200
- `java-rest-api-skeleton` - Java 21 / Spring Boot 3 REST API on port 8080

## Run locally

Start the API:

```bash
cd java-rest-api-skeleton
mvn spring-boot:run
```

In a second terminal, start the frontend:

```bash
cd angular-frontend
npm install
npm start
```

Visit `http://localhost:4200`. Angular's development proxy forwards `/api` requests to the backend.
