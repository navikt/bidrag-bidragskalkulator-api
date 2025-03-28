# Bidragskalkulator API - Project Guidelines

## Project Overview

The **Bidragskalkulator API** is a backend service that calculates child support payments (barnebidrag) in Norway. It serves as the calculation engine for the [bidrag-bidragskalkulator-ui](https://github.com/navikt/bidrag-bidragskalkulator-ui) frontend application.

### Purpose

This API provides endpoints for calculating child support payments based on various factors such as:
- Parents' income
- Child's age
- Visitation arrangements (samværsklasse)
- Type of support (bidragstype)

The calculations follow the Norwegian regulations for child support determination, providing accurate and legally compliant results.

### Key Features

- REST API for child support calculations
- Validation of input parameters
- Detailed calculation results
- OpenAPI/Swagger documentation

## Technical Architecture

### Technology Stack

- **Programming Language**: Kotlin
- **Framework**: Spring Boot
- **Build Tool**: Gradle
- **API Documentation**: OpenAPI/Swagger
- **Deployment**: NAIS (Kubernetes in GCP)
- **CI/CD**: GitHub Actions

### Project Structure

The project follows standard Spring Boot application structure:

- `src/main/kotlin/no/nav/bidrag/bidragskalkulator/` - Main application code
  - `controller/` - REST API endpoints
  - `service/` - Business logic and calculation services
  - `dto/` - Data Transfer Objects
  - `mapper/` - Object mappers
- `src/test/` - Test code

## Development Guidelines

### Prerequisites

- Java 21
- Gradle

### Local Development

To run the application locally:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Or via IntelliJ IDEA:
1. Right-click on BidragBidragskalkulatorApiApplication.kt
2. Select "More Run/Debug"
3. Click "Modify Run Configuration"
4. Add "local" under "Active Profiles"
5. Apply and run

### Testing

Run tests with:

```bash
./gradlew test
```

All pull requests automatically go through a test pipeline that builds the application and runs all tests.

### API Documentation

- Local: http://localhost:8080/swagger-ui/index.html
- Dev: https://bidragskalkulator-api.intern.dev.nav.no/swagger-ui/index.html

## Deployment

The application is deployed to the development environment automatically on merge to main branch. Currently, the application is not in production.

## Ownership

**Team Bidragskalkulator**, part of **Team Bidrag** in PO Familie, is responsible for maintaining this application.

For questions or contributions, contact the team via NAV's internal Slack channel.