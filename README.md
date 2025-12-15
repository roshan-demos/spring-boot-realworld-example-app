# ![RealWorld Example App using Kotlin and Spring](example-logo.png)

[![Actions](https://github.com/gothinkster/spring-boot-realworld-example-app/workflows/Java%20CI/badge.svg)](https://github.com/gothinkster/spring-boot-realworld-example-app/actions)

> ### Spring boot + MyBatis codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a fully fledged full-stack application built with Spring boot + Mybatis including CRUD operations, authentication, routing, pagination, and more.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# Table of Contents

- [GraphQL Support](#new-graphql-support)
- [How it works](#how-it-works)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Security](#security)
- [Database](#database)
- [Getting Started](#getting-started)
- [REST API Endpoints](#rest-api-endpoints)
- [GraphQL API](#graphql-api)
- [Running Tests](#run-test)
- [Code Format](#code-format)
- [Configuration](#configuration)
- [Contributing](#help)

# *NEW* GraphQL Support  

Following some DDD principles. REST or GraphQL is just a kind of adapter. And the domain layer will be consistent all the time. So this repository implement GraphQL and REST at the same time.

The GraphQL schema is https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/src/main/resources/schema/schema.graphqls and the visualization looks like below.

![](graphql-schema.png)

And this implementation is using [dgs-framework](https://github.com/Netflix/dgs-framework) which is a quite new java graphql server framework.

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

The application follows a 4-layer Domain-Driven Design architecture that cleanly separates concerns and allows both REST and GraphQL APIs to share the same business logic.

## Layer Overview

**API Layer (Adapters)** - This layer handles all incoming HTTP requests and GraphQL queries. REST controllers like `UsersApi`, `ArticleApi`, and `CommentsApi` process HTTP requests, while GraphQL resolvers handle GraphQL operations. The security configuration and JWT token filtering also reside here.

**Application Layer (Services)** - This layer orchestrates business operations and implements the CQRS pattern. Command services like `ArticleCommandService` handle write operations (create, update, delete), while query services like `ArticleQueryService` handle read operations with optimized data fetching. Cross-cutting concerns like JWT token generation and authorization checks are also managed here.

**Core Layer (Domain Model)** - This is the heart of the application containing pure business logic. Domain entities like `User`, `Article`, `Comment`, and `Tag` define the business model. Repository interfaces define contracts for data access without specifying implementation details. This layer has no dependencies on frameworks or infrastructure.

**Infrastructure Layer (Technical Details)** - This layer provides concrete implementations of repository interfaces using MyBatis. SQL mappings are externalized in XML files, keeping the domain layer clean. Password encoding with BCrypt and JWT token handling are implemented here.

## Key Design Patterns

The CQRS pattern separates read and write models, allowing optimized queries for reading data while maintaining clean domain models for writes. For example, `ArticleQueryService` returns enriched `ArticleData` DTOs with favorite counts and author profiles, while `ArticleCommandService` works directly with `Article` domain entities.

The Data Mapper pattern via MyBatis keeps SQL separate from domain objects. This means domain entities remain pure Java objects without ORM annotations, and SQL queries can be optimized independently of the domain model.

# Project Structure

```
src/main/java/io/spring/
├── api/                           # REST Controllers
│   ├── ArticleApi.java            # Single article CRUD operations
│   ├── ArticlesApi.java           # Article listing and creation
│   ├── ArticleFavoriteApi.java    # Favorite/unfavorite articles
│   ├── CommentsApi.java           # Comment operations
│   ├── CurrentUserApi.java        # Current user profile
│   ├── ProfileApi.java            # User profiles and follow/unfollow
│   ├── TagsApi.java               # Tag listing
│   ├── UsersApi.java              # Registration and login
│   ├── exception/                 # Custom exceptions
│   └── security/                  # Security configuration
│       ├── JwtTokenFilter.java    # JWT authentication filter
│       └── WebSecurityConfig.java # Security rules
│
├── application/                   # Application Services
│   ├── article/                   # Article services
│   │   ├── ArticleCommandService.java  # Write operations
│   │   └── NewArticleParam.java        # Input DTOs
│   ├── comment/                   # Comment services
│   ├── user/                      # User services
│   ├── ArticleQueryService.java   # Article read operations
│   ├── CommentQueryService.java   # Comment read operations
│   ├── ProfileQueryService.java   # Profile read operations
│   ├── UserQueryService.java      # User read operations
│   └── data/                      # Data transfer objects
│
├── core/                          # Domain Layer
│   ├── article/                   # Article domain
│   │   ├── Article.java           # Article entity
│   │   ├── ArticleRepository.java # Repository interface
│   │   └── Tag.java               # Tag entity
│   ├── comment/                   # Comment domain
│   ├── favorite/                  # Favorite domain
│   ├── user/                      # User domain
│   │   ├── User.java              # User entity
│   │   ├── UserRepository.java    # Repository interface
│   │   └── FollowRelation.java    # Follow relationship
│   └── service/                   # Domain services
│
├── graphql/                       # GraphQL Resolvers
│   ├── UserMutation.java          # User mutations
│   ├── ArticleDatafetcher.java    # Article data fetchers
│   └── ...                        # Other resolvers
│
└── infrastructure/                # Infrastructure Layer
    ├── mybatis/                   # MyBatis configuration
    │   ├── mapper/                # Mapper interfaces
    │   └── readservice/           # Read-optimized queries
    ├── repository/                # Repository implementations
    │   ├── MyBatisArticleRepository.java
    │   ├── MyBatisUserRepository.java
    │   └── ...
    └── service/                   # Service implementations
        └── DefaultJwtService.java # JWT implementation

src/main/resources/
├── application.properties         # Application configuration
├── mapper/                        # MyBatis SQL mappings
│   ├── ArticleMapper.xml          # Article CRUD SQL
│   ├── ArticleReadService.xml     # Complex article queries
│   ├── UserMapper.xml             # User SQL
│   └── ...
└── schema/
    └── schema.graphqls            # GraphQL schema definition
```

# Security

Integration with Spring Security and add other filter for jwt token process.

The secret key is stored in `application.properties`.

## Authentication Flow

The application uses JWT (JSON Web Token) for stateless authentication. When a user registers or logs in, the server generates a JWT token signed with HS512 algorithm. This token is returned to the client and must be included in subsequent requests.

To authenticate requests, include the token in the `Authorization` header:

```
Authorization: Token <your-jwt-token>
```

The `JwtTokenFilter` intercepts all requests, extracts the token from the header, validates it, and sets the Spring Security context with the authenticated user.

## Endpoint Authorization

Public endpoints (no authentication required):
- `POST /users` - User registration
- `POST /users/login` - User login
- `GET /articles` - List articles
- `GET /articles/{slug}` - Get single article
- `GET /profiles/{username}` - Get user profile
- `GET /tags` - List tags
- `POST /graphql` - GraphQL endpoint (authentication handled per-query)

Protected endpoints (authentication required):
- `GET /user` - Get current user
- `PUT /user` - Update current user
- `POST /articles` - Create article
- `PUT /articles/{slug}` - Update article (author only)
- `DELETE /articles/{slug}` - Delete article (author only)
- `GET /articles/feed` - Get personalized feed
- `POST /articles/{slug}/comments` - Add comment
- `DELETE /articles/{slug}/comments/{id}` - Delete comment (author only)
- `POST /articles/{slug}/favorite` - Favorite article
- `DELETE /articles/{slug}/favorite` - Unfavorite article
- `POST /profiles/{username}/follow` - Follow user
- `DELETE /profiles/{username}/follow` - Unfollow user

## Password Security

User passwords are hashed using BCrypt before storage. The application never stores or logs plain-text passwords.

# Database

It uses a ~~H2 in-memory database~~ sqlite database (for easy local test without losing test data after every restart), can be changed easily in the `application.properties` for any other database.

## Database Schema

The application uses the following tables:

**users** - Stores user accounts with id, username, email, password (hashed), bio, and image URL.

**articles** - Stores articles with id, user_id (author), slug, title, description, body, and timestamps.

**tags** - Stores unique tag names with id and name.

**article_tags** - Junction table linking articles to tags (many-to-many relationship).

**comments** - Stores comments with id, body, article_id, user_id, and timestamps.

**article_favorites** - Junction table tracking which users have favorited which articles.

**follow_relations** - Tracks user follow relationships with user_id and follow_id.

## Changing the Database

To use a different database, update `application.properties`:

```properties
# For MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/realworld
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=your_username
spring.datasource.password=your_password

# For PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/realworld
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=your_username
spring.datasource.password=your_password
```

You'll also need to add the appropriate JDBC driver dependency to `build.gradle`.

# Getting started

You'll need Java 11 installed.

    ./gradlew bootRun

To test that it works, open a browser tab at http://localhost:8080/tags .  
Alternatively, you can run

    curl http://localhost:8080/tags

## Prerequisites

- Java 11 or higher
- Gradle (wrapper included)

## Quick Start

1. Clone the repository:
```bash
git clone https://github.com/gothinkster/spring-boot-realworld-example-app.git
cd spring-boot-realworld-example-app
```

2. Run the application:
```bash
./gradlew bootRun
```

3. The API will be available at `http://localhost:8080`

## Verify Installation

Test the API is running:
```bash
curl http://localhost:8080/tags
```

Register a new user:
```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"user":{"username":"testuser","email":"test@example.com","password":"password123"}}'
```

# Try it out with [Docker](https://www.docker.com/)

You'll need Docker installed.
	
    ./gradlew bootBuildImage --imageName spring-boot-realworld-example-app
    docker run -p 8081:8080 spring-boot-realworld-example-app

# Try it out with a RealWorld frontend

The entry point address of the backend API is at http://localhost:8080, **not** http://localhost:8080/api as some of the frontend documentation suggests.

# REST API Endpoints

## Users and Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/users` | Register a new user | No |
| POST | `/users/login` | Login and get JWT token | No |
| GET | `/user` | Get current user | Yes |
| PUT | `/user` | Update current user | Yes |

### Register User
```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
    "user": {
      "username": "jacob",
      "email": "jake@jake.jake",
      "password": "jakejake"
    }
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "user": {
      "email": "jake@jake.jake",
      "password": "jakejake"
    }
  }'
```

## Profiles

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/profiles/{username}` | Get user profile | No |
| POST | `/profiles/{username}/follow` | Follow a user | Yes |
| DELETE | `/profiles/{username}/follow` | Unfollow a user | Yes |

## Articles

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/articles` | List articles (with filters) | No |
| GET | `/articles/feed` | Get articles from followed users | Yes |
| GET | `/articles/{slug}` | Get single article | No |
| POST | `/articles` | Create article | Yes |
| PUT | `/articles/{slug}` | Update article | Yes (author) |
| DELETE | `/articles/{slug}` | Delete article | Yes (author) |

### List Articles with Filters
```bash
# Filter by tag
curl "http://localhost:8080/articles?tag=dragons"

# Filter by author
curl "http://localhost:8080/articles?author=jake"

# Filter by favorited
curl "http://localhost:8080/articles?favorited=jake"

# Pagination
curl "http://localhost:8080/articles?limit=10&offset=0"
```

### Create Article
```bash
curl -X POST http://localhost:8080/articles \
  -H "Content-Type: application/json" \
  -H "Authorization: Token <your-token>" \
  -d '{
    "article": {
      "title": "How to train your dragon",
      "description": "Ever wonder how?",
      "body": "You have to believe",
      "tagList": ["dragons", "training"]
    }
  }'
```

## Comments

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/articles/{slug}/comments` | Get comments for article | No |
| POST | `/articles/{slug}/comments` | Add comment to article | Yes |
| DELETE | `/articles/{slug}/comments/{id}` | Delete comment | Yes (author) |

## Favorites

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/articles/{slug}/favorite` | Favorite an article | Yes |
| DELETE | `/articles/{slug}/favorite` | Unfavorite an article | Yes |

## Tags

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/tags` | Get all tags | No |

# GraphQL API

Access the GraphQL playground at `http://localhost:8080/graphiql` when the application is running.

## Queries

```graphql
# Get a single article
query {
  article(slug: "how-to-train-your-dragon") {
    title
    body
    author {
      username
    }
  }
}

# List articles with pagination
query {
  articles(first: 10) {
    edges {
      node {
        title
        slug
        favoritesCount
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}

# Get current user
query {
  me {
    username
    email
  }
}

# Get user feed
query {
  feed(first: 10) {
    edges {
      node {
        title
        author {
          username
        }
      }
    }
  }
}
```

## Mutations

```graphql
# Create user
mutation {
  createUser(input: {
    username: "newuser"
    email: "new@example.com"
    password: "password123"
  }) {
    ... on UserPayload {
      user {
        username
        token
      }
    }
    ... on Error {
      message
    }
  }
}

# Login
mutation {
  login(email: "jake@jake.jake", password: "jakejake") {
    user {
      token
    }
  }
}

# Create article
mutation {
  createArticle(input: {
    title: "My Article"
    description: "About something"
    body: "The content"
    tagList: ["tag1", "tag2"]
  }) {
    article {
      slug
      title
    }
  }
}

# Follow user
mutation {
  followUser(username: "jake") {
    profile {
      username
      following
    }
  }
}
```

# Run test

The repository contains a lot of test cases to cover both api test and repository test.

    ./gradlew test

## Test Structure

The test suite is organized to mirror the main source structure:

**API Tests** (`src/test/java/io/spring/api/`) - Integration tests for REST controllers using `@WebMvcTest` and RestAssured MockMvc. These tests verify HTTP request/response handling, authentication, and validation.

**Application Tests** (`src/test/java/io/spring/application/`) - Unit tests for service layer business logic with mocked repositories.

**Core Tests** (`src/test/java/io/spring/core/`) - Unit tests for domain entities and value objects.

**Infrastructure Tests** (`src/test/java/io/spring/infrastructure/`) - Integration tests for repository implementations with a real database.

## Running Specific Tests

```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "io.spring.api.ArticlesApiTest"

# Run tests matching a pattern
./gradlew test --tests "*Api*"
```

# Code format

Use spotless for code format.

    ./gradlew spotlessJavaApply

## Code Style

The project uses [Google Java Format](https://github.com/google/google-java-format) via the Spotless Gradle plugin. Before committing code, run:

```bash
# Check formatting
./gradlew spotlessCheck

# Apply formatting
./gradlew spotlessJavaApply
```

# Configuration

## Application Properties

Key configuration options in `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:sqlite:dev.db
spring.datasource.driver-class-name=org.sqlite.JDBC

# JWT Configuration
jwt.secret=your-secret-key-here
jwt.sessionTime=86400  # Token validity in seconds (24 hours)

# Default User Image
image.default=https://static.productionready.io/images/smiley-cyrus.jpg

# MyBatis Configuration
mybatis.mapper-locations=mapper/*.xml
mybatis.configuration.map-underscore-to-camel-case=true
```

## Environment Variables

For production deployments, override sensitive values using environment variables:

```bash
export JWT_SECRET=your-production-secret
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/realworld
export SPRING_DATASOURCE_USERNAME=dbuser
export SPRING_DATASOURCE_PASSWORD=dbpassword
```

# Technology Stack

- **Framework**: Spring Boot 2.6.3
- **Language**: Java 11
- **Build Tool**: Gradle
- **Database**: SQLite (default), supports any JDBC-compatible database
- **ORM**: MyBatis 2.2.2
- **GraphQL**: Netflix DGS Framework 4.9.21
- **Authentication**: JWT (JJWT 0.11.2)
- **Security**: Spring Security
- **Validation**: Spring Boot Validation
- **Code Formatting**: Spotless with Google Java Format
- **Testing**: JUnit 5, RestAssured, Spring Test

# Help

Please fork and PR to improve the project.
