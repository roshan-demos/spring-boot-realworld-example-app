# ![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Actions](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)

> ### Spring boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with Spring boot + Mybatis including CRUD operations, authentication, routing, pagination, and more.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# Table of Contents

- [Overview](#overview)
- [GraphQL Support](#graphql-support)
- [How it works](#how-it-works)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Security](#security)
- [Database](#database)
- [Getting Started](#getting-started)
- [Docker](#try-it-out-with-docker)
- [Testing](#testing)
- [Development](#development)
- [Configuration](#configuration)
- [Contributing](#contributing)

# Overview

This application implements a Medium.com-like blogging platform where users can publish articles, follow other users, favorite articles, and comment on posts. It serves as a reference implementation demonstrating best practices for building production-ready Spring Boot applications.

Key features include user registration and authentication with JWT tokens, article management with tagging and slug-based URLs, social features like following users and favoriting articles, personalized feeds showing content from followed authors, and comment functionality on articles. The application exposes both REST and GraphQL APIs, sharing the same domain layer to demonstrate API-agnostic business logic.

# GraphQL Support  

Following DDD principles, REST or GraphQL is just a kind of adapter while the domain layer remains consistent. This repository implements both GraphQL and REST simultaneously.

The GraphQL schema is defined in [src/main/resources/schema/schema.graphqls](src/main/resources/schema/schema.graphqls) and the visualization looks like below.

![](graphql-schema.png)

This implementation uses [Netflix DGS Framework](https://github.com/Netflix/dgs-framework), a GraphQL server framework for Spring Boot. The GraphQL API supports queries for articles, user profiles, tags, and feeds, as well as mutations for user management, article CRUD operations, comments, and social interactions.

Access the GraphQL Playground at http://localhost:8080/graphiql after starting the application.

# How it works

The application uses Spring Boot (Web, Mybatis).

* Use the idea of Domain Driven Design to separate the business term and infrastructure term.
* Use MyBatis to implement the [Data Mapper](https://martinfowler.com/eaaCatalog/dataMapper.html) pattern for persistence.
* Use [CQRS](https://martinfowler.com/bliki/CQRS.html) pattern to separate the read model and write model.

And the code is organized as this:

1. `api` is the web layer implemented by Spring MVC
2. `core` is the business model including entities and services
3. `application` is the high-level services for querying the data transfer objects
4. `infrastructure`  contains all the implementation classes as the technique details

# Architecture

The application follows a 4-layer Domain-Driven Design architecture that cleanly separates concerns and maintains a consistent domain model regardless of the API type (REST or GraphQL).

## What is Domain-Driven Design (DDD)?

Domain-Driven Design is a software development approach introduced by Eric Evans in his 2003 book "Domain-Driven Design: Tackling Complexity in the Heart of Software." The core philosophy is that the structure and language of your code should match the business domain it serves. Rather than organizing code around technical concerns (like databases, frameworks, or UI), DDD organizes code around the business concepts and rules that define what the software actually does.

In DDD, the domain model is the central artifact. It represents the business concepts, rules, and logic in code form. The domain model should be developed in close collaboration with domain experts (people who understand the business) and should use the same language they use, known as the Ubiquitous Language. This shared vocabulary ensures that developers and business stakeholders can communicate effectively and that the code accurately reflects business requirements.

DDD introduces several key concepts that this application implements. Entities are objects with a distinct identity that persists over time, like User and Article in this codebase. Value Objects are immutable objects defined by their attributes rather than identity, such as a Tag. Aggregates are clusters of entities and value objects treated as a single unit for data changes, with one entity serving as the Aggregate Root. Repositories provide an abstraction for data access, allowing the domain layer to remain ignorant of persistence details. Domain Services contain business logic that doesn't naturally fit within an entity.

The strategic design aspect of DDD involves organizing large systems into Bounded Contexts, each with its own domain model and ubiquitous language. While this application is small enough to be a single bounded context, the layered architecture demonstrates how DDD principles create maintainable, testable code that can evolve with changing business requirements.

## Layer Overview

The API Layer (Adapters) contains REST controllers like UsersApi, ArticleApi, and CommentsApi, as well as GraphQL resolvers using the DGS framework. This layer handles HTTP requests, input validation, and response formatting. Security configuration and JWT token filtering also reside here.

The Application Layer (Services) contains command services for write operations and query services for read operations, following the CQRS pattern. ArticleCommandService handles article creation and updates, while ArticleQueryService handles complex read queries with filtering and pagination. This layer orchestrates domain operations and manages transactions.

The Core Layer (Domain Model) contains the domain entities (User, Article, Comment, Tag), value objects, and repository interfaces. This layer is pure business logic with no framework dependencies, making it highly testable and portable.

The Infrastructure Layer (Technical Details) contains MyBatis repository implementations, SQL mappers in XML files, security implementations like BCrypt password encoding and JWT token generation, and database configuration. This layer provides concrete implementations of the interfaces defined in the core layer.

## Key Design Patterns

The CQRS pattern separates read and write models for optimized data access. Write operations work with domain entities while read operations return enriched DTOs with computed fields like favorite counts and following status.

The Data Mapper pattern via MyBatis keeps SQL separate from domain objects. SQL queries are defined in XML mapper files, maintaining clean architecture boundaries and allowing complex queries without polluting domain entities.

The Repository pattern abstracts data access behind interfaces defined in the core layer, with implementations in the infrastructure layer. This allows the domain layer to remain independent of persistence technology.

# Project Structure

```
src/main/java/io/spring/
├── api/                           # REST Controllers
│   ├── ArticleApi.java            # Single article operations (GET/PUT/DELETE /articles/{slug})
│   ├── ArticlesApi.java           # Article listing and creation (GET/POST /articles)
│   ├── ArticleFavoriteApi.java    # Favorite/unfavorite articles
│   ├── CommentsApi.java           # Comment operations on articles
│   ├── CurrentUserApi.java        # Current user profile (GET/PUT /user)
│   ├── ProfileApi.java            # User profiles and follow/unfollow
│   ├── TagsApi.java               # List all tags
│   ├── UsersApi.java              # Registration and login
│   ├── exception/                 # Custom exceptions and handlers
│   └── security/                  # Security configuration
│       ├── JwtTokenFilter.java    # JWT authentication filter
│       └── WebSecurityConfig.java # Spring Security configuration
│
├── application/                   # Application Services
│   ├── ArticleQueryService.java   # Article read operations with filtering
│   ├── CommentQueryService.java   # Comment read operations
│   ├── ProfileQueryService.java   # Profile read operations
│   ├── TagsQueryService.java      # Tag listing
│   ├── UserQueryService.java      # User read operations
│   ├── article/                   # Article command service and DTOs
│   ├── data/                      # Data transfer objects
│   └── user/                      # User command service and DTOs
│
├── core/                          # Domain Model
│   ├── article/                   # Article entity, Tag, and repository interface
│   ├── comment/                   # Comment entity and repository interface
│   ├── favorite/                  # ArticleFavorite entity
│   ├── service/                   # Domain services (Authorization, JWT interface)
│   └── user/                      # User entity, FollowRelation, and repository interface
│
├── graphql/                       # GraphQL Resolvers (DGS)
│   ├── ArticleDatafetcher.java    # Article queries and mutations
│   ├── ArticleMutation.java       # Article mutations
│   ├── CommentDatafetcher.java    # Comment operations
│   ├── CommentMutation.java       # Comment mutations
│   ├── ProfileDatafetcher.java    # Profile queries
│   ├── RelayPageUtil.java         # Relay-style pagination utilities
│   ├── SecurityUtil.java          # Security utilities for GraphQL
│   ├── TagDatafetcher.java        # Tag queries
│   ├── UserMutation.java          # User mutations
│   └── exception/                 # GraphQL exception handling
│
├── infrastructure/                # Technical Implementations
│   ├── mybatis/                   # MyBatis mappers and read services
│   │   ├── mapper/                # Write operation mappers
│   │   └── readservice/           # Read operation mappers
│   ├── repository/                # Repository implementations
│   └── service/                   # Service implementations (JWT)
│
├── JacksonCustomizations.java     # JSON serialization configuration
├── MyBatisConfig.java             # MyBatis configuration
├── RealWorldApplication.java      # Application entry point
└── Util.java                      # Utility methods

src/main/resources/
├── application.properties         # Application configuration
├── db/migration/                  # Flyway database migrations
├── mapper/                        # MyBatis SQL mapper XML files
└── schema/
    └── schema.graphqls            # GraphQL schema definition
```

# API Reference

## Authentication

All authenticated endpoints require a JWT token in the Authorization header using the format `Authorization: Token {jwt_token}`. Tokens are obtained through the login or registration endpoints.

## User Endpoints

**POST /users** - Register a new user. Request body should contain email, username, and password wrapped in a "user" object. Returns the created user with a JWT token.

**POST /users/login** - Authenticate an existing user. Request body should contain email and password wrapped in a "user" object. Returns the user with a JWT token.

**GET /user** - Get the currently authenticated user's profile. Requires authentication.

**PUT /user** - Update the current user's profile. Accepts optional fields: email, username, password, bio, and image. Requires authentication.

## Profile Endpoints

**GET /profiles/{username}** - Get a user's public profile including username, bio, image, and following status.

**POST /profiles/{username}/follow** - Follow a user. Requires authentication.

**DELETE /profiles/{username}/follow** - Unfollow a user. Requires authentication.

## Article Endpoints

**GET /articles** - List articles with optional filtering. Query parameters include tag (filter by tag), author (filter by author username), favorited (filter by user who favorited), limit (default 20), and offset (default 0).

**GET /articles/feed** - Get articles from users you follow. Requires authentication. Supports limit and offset parameters.

**POST /articles** - Create a new article. Request body should contain title, description, body, and optional tagList wrapped in an "article" object. Requires authentication.

**GET /articles/{slug}** - Get a single article by its slug.

**PUT /articles/{slug}** - Update an article. Only the author can update. Accepts optional title, description, and body fields. Requires authentication.

**DELETE /articles/{slug}** - Delete an article. Only the author can delete. Requires authentication.

## Comment Endpoints

**GET /articles/{slug}/comments** - Get all comments for an article.

**POST /articles/{slug}/comments** - Add a comment to an article. Request body should contain the comment body wrapped in a "comment" object. Requires authentication.

**DELETE /articles/{slug}/comments/{id}** - Delete a comment. Only the comment author or article author can delete. Requires authentication.

## Favorite Endpoints

**POST /articles/{slug}/favorite** - Favorite an article. Requires authentication.

**DELETE /articles/{slug}/favorite** - Unfavorite an article. Requires authentication.

## Tag Endpoints

**GET /tags** - Get all tags used across articles.

# Security

The application uses JWT (JSON Web Token) for stateless authentication, making it suitable for distributed systems and single-page applications.

## Authentication Flow

When a user registers or logs in, the server generates a JWT token signed with the HS512 algorithm using a secret key configured in application.properties. The token contains the user ID as the subject and has a configurable expiration time (default 24 hours).

For authenticated requests, the JwtTokenFilter intercepts incoming requests, extracts the JWT from the Authorization header (format: "Token {jwt}"), validates the token signature and expiration, loads the user from the database, and sets the Spring Security context with the authenticated user.

## Endpoint Authorization

Public endpoints that don't require authentication include POST /users (registration), POST /users/login (authentication), GET /articles (list articles), GET /articles/{slug} (single article), GET /profiles/{username} (user profile), GET /tags (list tags), and the GraphQL endpoint at /graphql.

Protected endpoints requiring authentication include GET/PUT /user (current user), POST /articles (create article), PUT/DELETE /articles/{slug} (modify article), POST/DELETE /articles/{slug}/favorite (favorite operations), POST/DELETE /articles/{slug}/comments (comment operations), POST/DELETE /profiles/{username}/follow (follow operations), and GET /articles/feed (personalized feed).

## Authorization Rules

Article modifications (update/delete) are restricted to the article author. Comment deletion is allowed for either the comment author or the article author. These rules are enforced by the AuthorizationService class.

# Database

The application uses SQLite for persistence, making it easy to run locally without external database setup. The database file (dev.db) is created automatically in the project root directory.

## Schema Overview

The database schema includes tables for users (storing id, username, email, password hash, bio, and image), articles (storing id, user_id, slug, title, description, body, and timestamps), tags (storing id and name), article_tags (join table for many-to-many relationship), comments (storing id, body, article_id, user_id, and timestamps), article_favorites (join table for user-article favorites), and follows (storing user_id and follow_id for follow relationships).

## Database Migrations

Database schema is managed by Flyway. Migration scripts are located in src/main/resources/db/migration and are automatically applied on application startup.

## Changing the Database

To use a different database, update the datasource configuration in application.properties with the appropriate JDBC URL, driver class, username, and password. Add the corresponding JDBC driver dependency to build.gradle.

# Getting Started

## Prerequisites

You'll need Java 11 or higher installed. Verify your installation by running `java -version`.

## Running the Application

Clone the repository and navigate to the project directory. Run the application using `./gradlew bootRun`. The application will start on port 8080.

To verify it's working, open a browser to http://localhost:8080/tags or run `curl http://localhost:8080/tags`.

## Quick Start with API

Register a new user:
```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"user":{"username":"jake","email":"jake@example.com","password":"password123"}}'
```

Login to get a token:
```bash
curl -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"user":{"email":"jake@example.com","password":"password123"}}'
```

Create an article (replace {token} with your JWT):
```bash
curl -X POST http://localhost:8080/articles \
  -H "Content-Type: application/json" \
  -H "Authorization: Token {token}" \
  -d '{"article":{"title":"How to train your dragon","description":"Ever wonder how?","body":"You have to believe","tagList":["dragons","training"]}}'
```

# Try it out with [Docker](https://www.docker.com/)

You'll need Docker installed.

Build the Docker image:
```bash
./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
```

Run the container:
```bash
docker run -p 8081:8080 spring-boot-realworld-example-app
```

The application will be available at http://localhost:8081.

Note: When running in Docker, the SQLite database is created inside the container and will be lost when the container is removed. For persistent data, mount a volume for the database file.

# Try it out with a RealWorld frontend

The entry point address of the backend API is at http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests.

You can use any RealWorld frontend implementation with this backend. Popular options include the React, Angular, and Vue implementations available at the [RealWorld repository](https://github.com/gothinkster/realworld).

# Testing

The repository contains comprehensive test coverage across all layers.

## Running Tests

Run all tests:
```bash
./gradlew test
```

## Test Categories

API Tests (src/test/java/io/spring/api/) are integration tests using @WebMvcTest and RestAssuredMockMvc. They test REST endpoints with mocked services to verify request/response handling, validation, and error cases.

Service Tests (src/test/java/io/spring/application/) are unit tests for business logic with mocked repositories. They verify service layer behavior including validation rules and business constraints.

Repository Tests (src/test/java/io/spring/infrastructure/) are integration tests with a real database connection. They verify MyBatis mappers and SQL queries work correctly.

Domain Tests (src/test/java/io/spring/core/) are unit tests for domain entities and value objects. They verify entity behavior and business rules.

# Development

## Code Formatting

The project uses Spotless with Google Java Format for consistent code style. Format your code before committing:
```bash
./gradlew spotlessJavaApply
```

Check formatting without applying changes:
```bash
./gradlew spotlessCheck
```

## Building

Build the project:
```bash
./gradlew build
```

Build without running tests:
```bash
./gradlew build -x test
```

## Cleaning

Clean build artifacts and the development database:
```bash
./gradlew clean
```

## IDE Setup

For IntelliJ IDEA, import the project as a Gradle project. The IDE should automatically detect the project structure and configure it appropriately. Install the Lombok plugin for proper annotation processing support.

# Configuration

Application configuration is managed through src/main/resources/application.properties.

## Key Configuration Properties

**Database Configuration**: spring.datasource.url sets the JDBC URL (default: jdbc:sqlite:dev.db), spring.datasource.driver-class-name sets the JDBC driver class.

**JWT Configuration**: jwt.secret is the secret key for signing JWT tokens (should be changed in production), jwt.sessionTime is the token expiration time in seconds (default: 86400 = 24 hours).

**MyBatis Configuration**: mybatis.mapper-locations specifies the location of XML mapper files, mybatis.configuration.map-underscore-to-camel-case enables automatic conversion between database column names and Java property names.

**Default User Image**: image.default sets the default profile image URL for new users.

## Environment-Specific Configuration

For production deployments, you should change the JWT secret to a secure random value, configure an appropriate production database, enable HTTPS, and review and adjust logging levels.

# CI/CD

The project includes a GitHub Actions workflow (.github/workflows/gradle.yml) that runs on every push and pull request. The workflow checks out the code, sets up JDK 11, caches Gradle dependencies, and runs the test suite.

# Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your changes.

When contributing, ensure all tests pass by running `./gradlew test`, format your code using `./gradlew spotlessJavaApply`, follow the existing code style and architecture patterns, and add tests for new functionality.

# Help

Please fork and PR to improve the project.
