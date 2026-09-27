# MessengerFront

Angular client for the Messenger Spring Boot API. The API must be running from `../messenger` on port 8080; the development server proxies `/api` requests to it.

## Development server

Start the API, then run the frontend from this directory:

```bash
npm start
```

Open `http://localhost:4200/`. Register or sign in, then add another account by its user ID before selecting it as a message recipient. Friend lists are per-user, so each recipient must add you to message you back. The application reloads automatically when source files change.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
