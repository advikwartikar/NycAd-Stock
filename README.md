# Stock Trading Simulation

A Java and Spring Boot web application for practising stock trading with virtual credits. Users can explore stock data, simulate buy/sell transactions, and review portfolio performance without using real money.

> **Project status:** Educational project. Verify the current implementation and deployment configuration before relying on any feature described here.

## Features

- User authentication and role-based access with Spring Security
- Virtual trading and portfolio tracking
- CSV-based OHLCV stock data and technical indicators (where configured)
- Stock charts built with Thymeleaf, JavaScript, and Chart.js
- Administrative views for user and transaction monitoring (where implemented)

## Technology stack

- Java 17
- Spring Boot 3.2
- Spring MVC, Spring Security, Spring Data JPA
- Thymeleaf, HTML, CSS, JavaScript, Chart.js
- Maven
- PostgreSQL for configured deployments; H2 is included for local development

Check `pom.xml` and the active Spring profile for the exact database and dependency configuration used by your checkout.

## Prerequisites

- JDK 17+
- Maven 3.6+
- A configured database if your selected profile does not use an embedded database

## Run locally

Clone the repository:

```bash
git clone https://github.com/advikwartikar/NycAd-Stock.git
cd NycAd-Stock
```

Build and run the tests:

```bash
mvn clean verify
```

Start the application:

```bash
mvn spring-boot:run
```

Then open [http://localhost:8080](http://localhost:8080), assuming the active configuration uses port 8080.

If the application requires database environment variables, configure them locally before starting it. Never commit real passwords, tokens, or production connection strings.

## Stock data

The project supports CSV-based stock data. Confirm the expected headers and file locations against the current data-loading code before adding files. Example OHLCV columns are:

```csv
Open,High,Low,Close,Volume
100.0,102.5,99.5,101.0,50000
```

Technical-indicator columns such as SMA or RSI should only be included when supported by the configured parser.

## Testing

Run the test suite with:

```bash
mvn test
```

The CI workflow runs Maven verification on pushes and pull requests. A green workflow means the configured build and tests passed in that run; it does not replace manual testing of the web application.

## Project structure

```text
src/
  main/
    java/        Application code
    resources/   Configuration, templates, static assets, and data
  test/          Automated tests
docs/            Project documentation index
.github/
  workflows/     Continuous integration
```

## Documentation

See [docs/README.md](docs/README.md) for the documentation index and guidance on keeping project notes organized.

## Security notes

- Do not use demo credentials in a public or production deployment.
- Store secrets in environment variables or your hosting provider's secret manager.
- Keep database consoles and administrative endpoints protected.
- Review authentication, authorization, CSRF protection, and input validation before deployment.

## Disclaimer

This is an educational trading simulator, not a brokerage platform. It does not execute real financial transactions and is not investment advice.

## License

No open-source license is currently specified. Unless a license is added, reuse and redistribution remain subject to the repository owner's rights.
