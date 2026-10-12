Stock Trading Simulation Application

A Java-based stock market trading simulator built with Spring Boot. The application lets users practise buying and selling stocks using virtual credits, monitor portfolio performance, and explore stock price charts without risking real money.

Features

* User Management: Role-based access for regular users and administrators.
* Virtual Trading: Trade using a virtual starting balance of 100,000 credits.
* Stock Analysis: Load OHLCV stock data from CSV files and display technical indicators such as SMA and RSI.
* Portfolio Management: Track holdings, available balance, and profit/loss.
* Interactive Charts: Visualize stock price movements using candlestick and line charts.
* Admin Dashboard: Monitor registered users, portfolios, and transactions.
* Security: Spring Security-based authentication and authorization.

Features listed above should be enabled and tested in the current version of the application.

Technology Stack

Layer	Technologies
Backend	Java 17, Spring Boot 3.2
Security	Spring Security
Persistence	Spring Data JPA, H2 / MySQL
Frontend	Thymeleaf, HTML, CSS, JavaScript
Charts	Chart.js
Build Tool	Maven
Data Input	CSV-based OHLCV data

Getting Started

Prerequisites

* Java 17 or later
* Maven 3.6 or later
* MySQL 8.0 or later, if using MySQL instead of H2

Installation

Clone the repository:

git clone https://github.com/advikw artikar/NycAd-Stock.git
cd NycAd-Stock

Replace the clone URL above with the exact repository URL if your GitHub username or repository name differs.

Build the application:

mvn clean package

Run the application:

mvn spring-boot:run

Open http://localhost:8080 in your browser, provided the application is configured to use port 8080.

Database Configuration

The application can be configured to use H2 for local development or MySQL for persistent database storage.

For MySQL, create the database:

CREATE DATABASE stocktrading;

Configure the datasource URL, username, and password using your local application configuration or environment variables.

Never commit database passwords, API keys, or other secrets to the repository.

Stock Data Format

Stock history can be loaded from CSV files containing OHLCV values and optional technical indicators.

Example:

Open,High,Low,Close,Volume,SMA,RSI
100.0,102.5,99.5,101.0,50000,100.5,65.0

The actual CSV headers and file naming conventions must match the application’s data-loading implementation.

Application Modules

* Model: Users, stocks, transactions, and portfolios.
* Repository: Database persistence and queries.
* Service: Trading operations and business logic.
* Controller: Web routes and request handling.
* Config: Security and application configuration.
* DTO: Data transfer objects.
* Templates: Thymeleaf views.
* Static Resources: CSS, JavaScript, and other frontend assets.

Testing

Run the automated test suite:

mvn test

Important areas to test include authentication, authorization, transaction validation, available balance, stock purchases and sales, portfolio calculations, and persistence.

Roadmap

* [ ]	Improve unit and integration test coverage.
* [ ]	Strengthen validation and error handling.
* [ ]	Improve portfolio analytics and chart interactions.
* [ ]	Add CI checks using GitHub Actions.
* [ ]	Document deployment and environment configuration.
* [ ]	Add screenshots and a verified demonstration link.

Disclaimer

This project is intended for educational purposes. It simulates stock trading and does not execute real financial transactions or provide investment advice.

License

Educational/academic use. A formal open-source license can be added if the project is intended for public reuse.
